package dev.fnvir.kajz.notificationservice.service.push;

import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import dev.fnvir.kajz.notificationservice.config.KafkaTopicConfig;
import dev.fnvir.kajz.notificationservice.dto.event.PushNotificationEvent;
import dev.fnvir.kajz.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationEventListener {
    
    private final NotificationService notificationService;
    private final PushNotificationSseService sseService;
    private final JsonMapper jsonMapper;
    
    @KafkaListener(topics = KafkaTopicConfig.PUSH_TOPIC)
    @RetryableTopic(
        attempts = "3",
        backOff = @BackOff(delay = 5000, multiplier = 2.0, maxDelay = 30000),
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    public void consumePushNotification(
            String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.debug("Received push notification from topic: {}, partition: {}, offset: {}", topic, partition, offset);
        Mono.fromCallable(() -> jsonMapper.readValue(payload, PushNotificationEvent.class))
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume(JacksonException.class, _ -> {
                log.error(
                    "Skipping event! Unable to map event in topic: {}, partition: {}, offset: {} to type {}.",
                    topic, partition, offset, PushNotificationEvent.class.getName()
                );
                return Mono.empty(); // ACK
            })
            .flatMap(event -> 
                Mono.fromCallable(() -> notificationService.saveNotification(event)) // save in db
                    .subscribeOn(Schedulers.boundedElastic())
            ).flatMap(notification -> 
                Mono.fromRunnable(() -> sseService.publish(notification))
                    .onErrorResume(_ -> Mono.empty()) // ignore SSE publish errors
            )
            .block();
    }
    
    @DltHandler
    public void handlePushNotificationDlq(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.warn("New event on {}-dlq", topic);
    }

}
