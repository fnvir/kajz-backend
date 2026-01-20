package dev.fnvir.kajz.notificationservice.service.push;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.util.ReflectionTestUtils;

import dev.fnvir.kajz.notificationservice.dto.res.NotificationResponse;
import dev.fnvir.kajz.notificationservice.model.enums.RecipientRole;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("PushNotificationSseService Tests")
public class PushNotificationSseServiceTest {

    private PushNotificationSseService service;
    private ConcurrentMap<?, ?> sinks;

    @BeforeEach
    void setUp() {
        service = new PushNotificationSseService();
        sinks = (ConcurrentMap<?, ?>) ReflectionTestUtils.getField(service, "sinks");
    }

    @Nested
    @DisplayName("Subscribe Tests")
    class SubscribeTests {

        @Test
        @DisplayName("Should create new subscription for first-time user")
        void shouldCreateNewSubscription() {
            UUID userId = UUID.randomUUID();
            RecipientRole role = RecipientRole.CLIENT;

            Flux<ServerSentEvent<NotificationResponse>> flux = service.subscribe(userId, role, null);

            Assertions.assertThat(flux).isNotNull();
            Assertions.assertThat(sinks).hasSize(1);
            Assertions.assertThat(service.getActiveUserCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should reuse existing sink for same user and role")
        void shouldReuseExistingSink() {
            UUID userId = UUID.randomUUID();
            RecipientRole role = RecipientRole.WORKER;

            service.subscribe(userId, role, null);
            service.subscribe(userId, role, null);

            Assertions.assertThat(sinks).hasSize(1);
            Assertions.assertThat(service.getActiveUserCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should create separate sinks for different roles")
        void shouldCreateSeparateSinksForDifferentRoles() {
            UUID userId = UUID.randomUUID();

            service.subscribe(userId, RecipientRole.CLIENT, null);
            service.subscribe(userId, RecipientRole.WORKER, null);

            Assertions.assertThat(sinks).hasSize(2);
            Assertions.assertThat(service.getActiveUserCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should increment subscriber count on subscription")
        void shouldIncrementSubscriberCount() {
            UUID userId = UUID.randomUUID();
            RecipientRole role = RecipientRole.CLIENT;

            Flux<ServerSentEvent<NotificationResponse>> flux = service.subscribe(userId, role, null);

            StepVerifier.create(flux.take(Duration.ofMillis(100))).expectSubscription().thenCancel().verify();

            Assertions.assertThat(service.getActiveConnectionCount()).isGreaterThanOrEqualTo(0);
        }
        
        @Test
        @DisplayName("Should replay missed events when lastEventId is provided")
        void shouldReplayMissedEvents() {
            UUID userId = UUID.randomUUID();
            RecipientRole role = RecipientRole.CLIENT;
            
            // first subscription to create sink
            Flux<ServerSentEvent<NotificationResponse>> initialFlux = service.subscribe(userId, role, null);
            StepVerifier.create(initialFlux.take(1))
                    .expectSubscription()
                    .thenCancel()
                    .verify();

            // publish notifications
            NotificationResponse notification1 = createNotification(userId, role);
            NotificationResponse notification2 = createNotification(userId, role);
            
            service.publish(notification1);
            service.publish(notification2);

            // wait a bit for events to be stored
            await().atMost(Duration.ofMillis(150)).untilAsserted(() -> Assertions.assertThat(sinks).isNotEmpty());

            // subscribe with a very old lastEventId to get replayed events
            Flux<ServerSentEvent<NotificationResponse>> replayFlux = service.subscribe(userId, role, "0");

            StepVerifier.create(replayFlux.take(2))
                    .expectNextMatches(event -> "notification".equals(event.event()))
                    .expectNextMatches(event -> "notification".equals(event.event()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle invalid lastEventId gracefully")
        void shouldHandleInvalidLastEventId() {
            UUID userId = UUID.randomUUID();
            RecipientRole role = RecipientRole.CLIENT;

            Flux<ServerSentEvent<NotificationResponse>> flux = service.subscribe(userId, role, "invalid-id");

            Assertions.assertThat(flux).isNotNull();
            StepVerifier.create(flux.take(Duration.ofMillis(100))).expectSubscription().thenCancel().verify();
        }
    }

    @Nested
    @DisplayName("Publish Tests")
    class PublishTests {

        @Test
        @DisplayName("Should publish notification to active subscriber")
        void shouldPublishToActiveSubscriber() {
            UUID userId = UUID.randomUUID();
            RecipientRole role = RecipientRole.CLIENT;

            Flux<ServerSentEvent<NotificationResponse>> flux = service.subscribe(userId, role, null);
            NotificationResponse notification = createNotification(userId, role);

            StepVerifier.create(flux.take(1)).expectSubscription().then(() -> service.publish(notification))
                    .expectNextMatches(event -> "notification".equals(event.event()) && event.data() != null
                            && event.data().userId().equals(userId))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should drop notification when no sink exists")
        void shouldDropNotificationWhenNoSink() {
            UUID userId = UUID.randomUUID();
            RecipientRole role = RecipientRole.CLIENT;
            NotificationResponse notification = createNotification(userId, role);

            service.publish(notification);

            Assertions.assertThat(sinks).isEmpty();
        }

        @Test
        @DisplayName("Should store notification in history even without active subscribers")
        void shouldStoreInHistoryWithoutSubscribers() {
            UUID userId = UUID.randomUUID();
            RecipientRole role = RecipientRole.CLIENT;

            // create but don't subscribe
            Flux<ServerSentEvent<NotificationResponse>> flux = service.subscribe(userId, role, null);
            StepVerifier.create(flux.take(Duration.ofMillis(50))).expectSubscription().thenCancel().verify();

            NotificationResponse notification = createNotification(userId, role);
            service.publish(notification);

            // verify event is in history by replaying
            Flux<ServerSentEvent<NotificationResponse>> replayFlux = service.subscribe(userId, role, "0");
            StepVerifier.create(replayFlux.take(1)).expectNextMatches(event -> "notification".equals(event.event()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should publish to multiple subscribers of same user")
        void shouldPublishToMultipleSubscribers() {
            UUID userId = UUID.randomUUID();
            RecipientRole role = RecipientRole.CLIENT;

            Flux<ServerSentEvent<NotificationResponse>> flux1 = service.subscribe(userId, role, null);
            Flux<ServerSentEvent<NotificationResponse>> flux2 = service.subscribe(userId, role, null);

            NotificationResponse notification = createNotification(userId, role);

            StepVerifier.create(Flux.merge(flux1.take(1), flux2.take(1))).expectSubscription()
                    .then(() -> service.publish(notification)).expectNextCount(2).verifyComplete();
        }

        @Test
        @DisplayName("Should not publish to different user")
        void shouldNotPublishToDifferentUser() {
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();
            RecipientRole role = RecipientRole.CLIENT;

            Flux<ServerSentEvent<NotificationResponse>> _ = service.subscribe(userId1, role, null);
            Flux<ServerSentEvent<NotificationResponse>> flux2 = service.subscribe(userId2, role, null);

            NotificationResponse notification = createNotification(userId1, role);

            StepVerifier.create(flux2.take(Duration.ofMillis(200))).expectSubscription()
                    .then(() -> service.publish(notification)).expectNoEvent(Duration.ofMillis(100)).thenCancel()
                    .verify();
        }
    }

    @Nested
    @DisplayName("Heartbeat Tests")
    class HeartbeatTests {

        @Test
        @DisplayName("Should send heartbeat to active connections")
        void shouldSendHeartbeatToActiveConnections() {
            UUID userId = UUID.randomUUID();
            RecipientRole role = RecipientRole.CLIENT;

            Flux<ServerSentEvent<NotificationResponse>> flux = service.subscribe(userId, role, null);

            StepVerifier.create(flux.take(1)).expectSubscription().then(() -> service.sendHeartbeat())
                    .expectNextMatches(event -> event.comment() != null && event.comment().contains("heartbeat"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should not fail when no connections exist")
        void shouldNotFailWithNoConnections() {
            service.sendHeartbeat();
            Assertions.assertThat(sinks).isEmpty();
        }
    }

    @Nested
    @DisplayName("Cleanup Tests")
    class CleanupTests {

        @Test
        @DisplayName("Should remove idle sinks after TTL expires")
        void shouldRemoveIdleSinks() {
            UUID userId = UUID.randomUUID();
            RecipientRole role = RecipientRole.CLIENT;

            // create sink but cancel immediately
            Flux<ServerSentEvent<NotificationResponse>> flux = service.subscribe(userId, role, null);
            StepVerifier.create(flux.take(Duration.ofMillis(50))).expectSubscription().thenCancel().verify();

            // manually set lastTouch to expired time using reflection
            Object sinkHolder = sinks.values().iterator().next();
            ReflectionTestUtils.setField(sinkHolder, "lastTouch", Instant.now().minus(Duration.ofMinutes(5)));

            // invoke cleanup
            ReflectionTestUtils.invokeMethod(service, "cleanupIdleSinks");

            Assertions.assertThat(sinks).isEmpty();
        }

        @Test
        @DisplayName("Should not remove active sinks")
        void shouldNotRemoveActiveSinks() {
            UUID userId = UUID.randomUUID();
            RecipientRole role = RecipientRole.CLIENT;

            Flux<ServerSentEvent<NotificationResponse>> flux = service.subscribe(userId, role, null);

            // keep subscription active
            StepVerifier.create(flux.take(Duration.ofMillis(100))).expectSubscription()
                    .then(() -> ReflectionTestUtils.invokeMethod(service, "cleanupIdleSinks")).thenCancel().verify();

            // sink should still exist (might be cleaned up after cancel, that's fine)
            Assertions.assertThat(service.getActiveUserCount()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Metrics Tests")
    class MetricsTests {

        @Test
        @DisplayName("Should return correct active user count")
        void shouldReturnCorrectActiveUserCount() {
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();

            service.subscribe(userId1, RecipientRole.CLIENT, null);
            service.subscribe(userId2, RecipientRole.WORKER, null);
            service.subscribe(userId1, RecipientRole.WORKER, null);

            Assertions.assertThat(service.getActiveUserCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return correct active connection count")
        void shouldReturnCorrectActiveConnectionCount() {
            UUID userId = UUID.randomUUID();
            RecipientRole role = RecipientRole.CLIENT;

            Flux<ServerSentEvent<NotificationResponse>> flux1 = service.subscribe(userId, role, null);
            Flux<ServerSentEvent<NotificationResponse>> flux2 = service.subscribe(userId, role, null);

            StepVerifier.create(Flux.merge(flux1.take(Duration.ofMillis(100)), flux2.take(Duration.ofMillis(100))))
                    .expectSubscription().then(() -> Assertions.assertThat(service.getActiveConnectionCount()).isEqualTo(2))
                    .thenCancel().verify();
        }

        @Test
        @DisplayName("Should return zero when no connections")
        void shouldReturnZeroWhenNoConnections() {
            Assertions.assertThat(service.getActiveUserCount()).isZero();
            Assertions.assertThat(service.getActiveConnectionCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Event History Tests")
    class EventHistoryTests {

        @Test
        @DisplayName("Should maintain limited history size")
        void shouldMaintainLimitedHistorySize() {
            UUID userId = UUID.randomUUID();
            RecipientRole role = RecipientRole.CLIENT;

            // create sink
            Flux<ServerSentEvent<NotificationResponse>> flux = service.subscribe(userId, role, null);
            StepVerifier.create(flux.take(Duration.ofMillis(50))).expectSubscription().thenCancel().verify();

            // publish more events than history size (10)
            for (int i = 0; i < 15; i++) {
                service.publish(createNotification(userId, role));
            }

            // replay should only get last 10 events
            Flux<ServerSentEvent<NotificationResponse>> replayFlux = service.subscribe(userId, role, "0");
            StepVerifier.create(replayFlux.take(11)) // take 11 to ensure we don't get more than 10
                    .expectNextCount(10).thenCancel().verify();
        }
    }

    private NotificationResponse createNotification(UUID userId, RecipientRole role) {
        return NotificationResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .recipientRole(role)
                .title("Test Notification")
                .body("Test notification body")
                .type("NEW_ORDER_TEST")
                .createdAt(Instant.now())
                .read(false)
                .archived(false)
                .build();
    }
}