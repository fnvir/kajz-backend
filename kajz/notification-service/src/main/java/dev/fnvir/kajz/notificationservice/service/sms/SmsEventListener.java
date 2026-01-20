package dev.fnvir.kajz.notificationservice.service.sms;

import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import dev.fnvir.kajz.notificationservice.config.KafkaTopicConfig;
import dev.fnvir.kajz.notificationservice.dto.event.SmsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsEventListener {
    
    private final SmsSenderService smsService;
    private final JsonMapper jsonMapper;
    
    @KafkaListener(topics = KafkaTopicConfig.SMS_TOPIC, groupId = "notification-service-group")
    @RetryableTopic(
        attempts = "2",
        backOff = @BackOff(delay = 5000, multiplier = 2.0, maxDelay = 30000),
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    public void handleSmsEvent(
            String smsPayload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        try {
            SmsEvent sms = jsonMapper.readValue(smsPayload, SmsEvent.class);
            if (sms == null) {
                log.warn("Deserialization failed for message in topic {}, check headers for exception", topic);
                return;
            }
            smsService.sendSms(sms.to(), sms.message());
        } catch (JacksonException e) {
            log.error("Skipping event! Unable to map event in topic: {}, partition: {}, offset: {} to type {}.",
                    topic, partition, offset, SmsEvent.class.getName());
        }
    }
    
    @DltHandler
    public void handleSmsDlt(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.warn("New event on sms-dlq from topic {}", topic);
        // TODO: alert/persist in db/replay
    }
    
}
