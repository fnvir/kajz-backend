package dev.fnvir.kajz.notificationservice.service.email;

import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import dev.fnvir.kajz.notificationservice.config.KafkaTopicConfig;
import dev.fnvir.kajz.notificationservice.dto.event.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailEventListener {
    
    private final EmailSenderService emailService;
    private final JsonMapper jsonMapper;
    
    @KafkaListener(topics = KafkaTopicConfig.EMAIL_TOPIC, groupId = "notification-service-group")
    @RetryableTopic(
        attempts = "3",
        backOff = @BackOff(delay = 5000, multiplier = 2.0, maxDelay = 30000),
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    public void handleEmailEvent(
            String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        try {
            EmailEvent email = jsonMapper.readValue(payload, EmailEvent.class);
            
            if (email == null) {
                log.warn("Deserialization failed for email in topic {}, check headers for exception", topic);
                return;
            }
            log.debug("Received email event from topic: {}, partition: {}, offset: {}", topic, partition, offset);
            
            emailService.sendEmail(
                email.getTo(), email.getCc(), email.getBcc(), 
                email.getSubject(), email.getContent(),
                email.isHtml(), email.getPriority()
            );
        
        } catch (JacksonException e) {
            log.error("Skipping event! Unable to map event in topic: {}, partition: {}, offset: {} to type {}.",
                    topic, partition, offset, EmailEvent.class.getName());
        }
    }
    
    @DltHandler
    public void handleEmailDlt(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.warn("New event on email-dlq from topic {}", topic);
        // TODO: alert/persist in db/replay
    }

}
