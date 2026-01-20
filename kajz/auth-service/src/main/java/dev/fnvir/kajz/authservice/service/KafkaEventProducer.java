package dev.fnvir.kajz.authservice.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import dev.fnvir.kajz.authservice.config.KafkaTopicConfig;
import dev.fnvir.kajz.authservice.dto.event.EmailEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class KafkaEventProducer {
    
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

}
