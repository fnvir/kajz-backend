package dev.fnvir.kajz.notificationservice.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import dev.fnvir.kajz.notificationservice.config.KafkaTopicConfig;
import dev.fnvir.kajz.notificationservice.dto.event.EmailEvent;
import dev.fnvir.kajz.notificationservice.dto.event.PushNotificationEvent;
import dev.fnvir.kajz.notificationservice.dto.event.SmsEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public CompletableFuture<SendResult<String, Object>> publishEmailNotification(@Valid EmailEvent emailDto) {
        log.debug("Sending email notification to topic: {}", KafkaTopicConfig.EMAIL_TOPIC);
        
        return kafkaTemplate.send(KafkaTopicConfig.EMAIL_TOPIC, emailDto)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Email notification sent successfully: offset={}", result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send email notification: {}", ex.getMessage());
                    }
                });
    }
    
    public CompletableFuture<SendResult<String, Object>> sendSmsNotification(@Valid SmsEvent smsDto) {
        log.debug("Sending SMS notification to topic: {}, recipient: {}", KafkaTopicConfig.SMS_TOPIC, smsDto.to());
        
        return kafkaTemplate.send(KafkaTopicConfig.SMS_TOPIC, smsDto)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("SMS notification sent successfully: offset={}", result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send SMS notification: {}", ex.getMessage());
                }
            });
    }

    public CompletableFuture<SendResult<String, Object>> sendPushNotification(@Valid PushNotificationEvent notificationEvent) {
        String key = String.join(
            "_",
            notificationEvent.getUserId()+"", // userId+role as key to keep ordering per-user
            notificationEvent.getRecipientRole().name()
        );
        
        return kafkaTemplate.send(KafkaTopicConfig.PUSH_TOPIC, key, notificationEvent)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Push notification sent successfully: offset={}", result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send Push notification: {}", ex.getMessage());
                }
            });
    }

}
