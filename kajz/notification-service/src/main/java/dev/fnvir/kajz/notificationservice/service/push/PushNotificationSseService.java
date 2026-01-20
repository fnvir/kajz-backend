package dev.fnvir.kajz.notificationservice.service.push;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dev.fnvir.kajz.notificationservice.dto.res.NotificationResponse;
import dev.fnvir.kajz.notificationservice.model.enums.RecipientRole;
import io.hypersistence.tsid.TSID;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Service for managing Server-Sent Events (SSE) for push notifications.
 */
@Slf4j
@Service
@NoArgsConstructor
public class PushNotificationSseService {

    /**
     * Maximum number of events that can be buffered per user in case the client is
     * slow or temporarily disconnected. Once the buffer is full, new events will be
     * dropped until there is space available.
     */
    private static final int DEFAULT_BUFFER_SIZE = 128; // per-user buffer capacity

    /**
     * The number of past events to keep for replay when a client reconnects.
     */
    private static final int REPLAY_HISTORY_SIZE = 10;

    /**
     * Duration after which an idle user's sink will be removed.
     */
    private static final Duration USER_IDLE_TTL = Duration.ofMinutes(3);

    /**
     * Map of active SSE sinks per user (userId + role). Notifications are separated
     * by user role to allow different notification streams for different platforms
     * (e.g., SELLER app vs CUSTOMER app).
     */
    private final ConcurrentMap<Pair<UUID, RecipientRole>, SinkHolder> sinks = new ConcurrentHashMap<>();

    /**
     * A global heartbeat event sent periodically to keep the connection alive.
     */
    private static final ServerSentEvent<NotificationResponse> HEARTBEAT = ServerSentEvent
            .<NotificationResponse>builder().comment("heartbeat").build();

    
    /**
     * @param userId      the user's ID
     * @param role        the user's role
     * @param lastEventId the last event ID received by client (null if first connection)
     * @return a cold Flux the HTTP connection will subscribe to.
     */
    public Flux<ServerSentEvent<NotificationResponse>> subscribe(UUID userId, RecipientRole role, String lastEventId) {
        SinkHolder holder = getOrCreateSink(Pair.of(userId, role));
        Flux<ServerSentEvent<NotificationResponse>> replayFlux = Flux.empty();

        Long lastEventTimestamp = null;
        try { lastEventTimestamp = Long.parseLong(lastEventId); }
        catch (NumberFormatException e) {}
        
        if (lastEventTimestamp != null) {
            List<ServerSentEvent<NotificationResponse>> missedEvents = holder.getEventsSince(lastEventTimestamp);
            if (!missedEvents.isEmpty()) {
                replayFlux = Flux.fromIterable(missedEvents);
                log.debug("Replaying {} missed events for userId={}, role={}, since={}", missedEvents.size(), userId,
                        role, lastEventTimestamp);
            }
        }
        
        Flux<ServerSentEvent<NotificationResponse>> flux = Flux.concat(replayFlux, holder.sink.asFlux())
                .doOnSubscribe(_ -> {
                    holder.incrementSubscribers();
                    log.debug("New connection opened for userId: {}, role: {}", userId, role);
                }).doFinally(st -> {
                    holder.decrementSubscribers();
                    log.debug("SSE connection closed for userId={}, role:{}, cause={}", userId, role, st);
                });

        return flux;
    }

    /**
     * Publish a notification to a user's sink.
     * 
     * @param notification the notification to publish.
     */
    public void publish(NotificationResponse notification) {
        UUID userId = notification.userId();
        var role = notification.recipientRole();
        
        SinkHolder holder = sinks.get(Pair.of(userId, role));
        if (holder == null) {
            log.debug("No active SSE sink for userId={}, role={}, dropping event", userId, role);
            return;
        }
        
        var notificationEvent = mapNotificationDtoToServerEvent(notification);
        holder.addToHistory(notificationEvent);
        
        if (holder.getSubscriberCount() == 0) {
            log.debug("No active subscribers for userId={}, role={}, event stored in history", userId, role);
            return;
        }
        
        Sinks.EmitResult result = holder.sink.tryEmitNext(notificationEvent);
        
        if (result.isFailure()) {
            switch (result) {
                case FAIL_ZERO_SUBSCRIBER -> {
                    log.debug("Emit failed (no subscribers) for userId={}", userId);
                }
                case FAIL_OVERFLOW -> {
                    log.warn("Emit overflow for userId={}, buffer full", userId);
                }
                case FAIL_NON_SERIALIZED -> {
                    log.warn("Emit non-serialized failure for userId={}, will retry", userId);
                    holder.sink.emitNext(notificationEvent, Sinks.EmitFailureHandler.FAIL_FAST);
                }
                default -> {
                    log.error("Unexpected emit failure for userId={}, result={}", userId, result);
                }
            }
        } else {
            log.debug("Notification emitted to userId={}, id={}", userId, notification.id());
        }
    }
    
    private SinkHolder getOrCreateSink(Pair<UUID, RecipientRole> user) {
        return sinks.compute(user, (_, existing) -> {
            if (existing != null) {
                existing.touch();
                return existing;
            } else {
                Sinks.Many<ServerSentEvent<NotificationResponse>> sink = Sinks.many().multicast()
                        .onBackpressureBuffer(DEFAULT_BUFFER_SIZE, false); // bounded buffer
                return new SinkHolder(sink);
            }
        });
    }

    @Scheduled(initialDelay = 29, fixedRate = 29, timeUnit = TimeUnit.SECONDS)
    protected void sendHeartbeat() {
        int sent = 0;
        for (var e : sinks.entrySet()) {
            SinkHolder h = e.getValue();
            if (h.getSubscriberCount() > 0) {
                var result = h.sink.tryEmitNext(HEARTBEAT);
                if (result == Sinks.EmitResult.OK) {
                    sent++;
                }
            }
        }
        log.debug("Heartbeat sent to {} connections", sent);
    }
    
    @Scheduled(initialDelay = 3, fixedRate = 2, timeUnit = TimeUnit.MINUTES)
    private void cleanupIdleSinks() {
        Instant now = Instant.now();
        for (var e : sinks.entrySet()) {
            var h = e.getValue();
            if (h.isRemovable(now)) {
                sinks.remove(e.getKey(), h);
                h.sink.tryEmitComplete();
                log.debug("Removed idle SSE sink for userId={}", e.getKey());
            }
        }
    }

    public int getActiveUserCount() {
        return sinks.size();
    }

    public int getActiveConnectionCount() {
        return sinks.values().stream().mapToInt(SinkHolder::getSubscriberCount).sum();
    }
    
    private ServerSentEvent<NotificationResponse> mapNotificationDtoToServerEvent(NotificationResponse dto) {
        return ServerSentEvent.<NotificationResponse>builder()
                .id(TSID.fast().toLong()+"")
                .event("notification")
                .data(dto)
                .build();
    }

    private static class SinkHolder {
        private final Sinks.Many<ServerSentEvent<NotificationResponse>> sink;
        private final AtomicInteger subscribers = new AtomicInteger(0);
        private volatile Instant lastTouch = Instant.now();
        private final ConcurrentLinkedQueue<ServerSentEvent<NotificationResponse>> eventHistory = new ConcurrentLinkedQueue<>(); // for
                                                                                                                                    // replay

        SinkHolder(Sinks.Many<ServerSentEvent<NotificationResponse>> sink) {
            this.sink = sink;
        }

        void touch() {
            lastTouch = Instant.now();
        }

        void incrementSubscribers() {
            subscribers.incrementAndGet();
            touch();
        }

        void decrementSubscribers() {
            if (subscribers.decrementAndGet() < 0) {
                subscribers.set(0);
            }
            touch();
        }

        int getSubscriberCount() {
            return subscribers.get();
        }

        boolean isRemovable(Instant now) {
            return subscribers.get() == 0 && lastTouch.plus(USER_IDLE_TTL).isBefore(now);
        }
        
        void addToHistory(ServerSentEvent<NotificationResponse> event) {
            if (StringUtils.hasText(event.id())) {
                eventHistory.offer(event);
                while (eventHistory.size() > REPLAY_HISTORY_SIZE) {
                    eventHistory.poll();
                }
            }
        }

        List<ServerSentEvent<NotificationResponse>> getEventsSince(Long lastEventTimestamp) {
            return eventHistory.stream().filter(event -> {
                try {
                    var eventTimestamp = Long.parseLong(event.id());
                    return eventTimestamp > lastEventTimestamp;
                } catch (NumberFormatException e) {
                    return false;
                }
            }).toList();
        }
    }

}
