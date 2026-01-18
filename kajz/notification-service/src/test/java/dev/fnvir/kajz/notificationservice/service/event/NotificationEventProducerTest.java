package dev.fnvir.kajz.notificationservice.service.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import dev.fnvir.kajz.notificationservice.config.KafkaTopicConfig;
import dev.fnvir.kajz.notificationservice.dto.event.EmailEvent;
import dev.fnvir.kajz.notificationservice.dto.event.PushNotificationEvent;
import dev.fnvir.kajz.notificationservice.dto.event.SmsEvent;
import dev.fnvir.kajz.notificationservice.model.enums.RecipientRole;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventProducer Unit Tests")
class NotificationEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Captor
    private ArgumentCaptor<Object> messageCaptor;

    private NotificationEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new NotificationEventProducer(kafkaTemplate);
    }

    @Nested
    @DisplayName("Email Notification Tests")
    class EmailNotificationTests {

        @Test
        @DisplayName("Should publish email notification to correct topic")
        void shouldPublishEmailNotificationToCorrectTopic() {
            EmailEvent emailEvent = createValidEmailEvent();
            CompletableFuture<SendResult<String, Object>> future = createSuccessfulSendResult(
                    KafkaTopicConfig.EMAIL_TOPIC, 0, 123L);
            when(kafkaTemplate.send(eq(KafkaTopicConfig.EMAIL_TOPIC), any())).thenReturn(future);

            CompletableFuture<SendResult<String, Object>> result = producer.publishEmailNotification(emailEvent);

            Assertions.assertThat(result).isCompletedWithValueMatching(
                    sendResult -> sendResult.getRecordMetadata().offset() == 123L);
            verify(kafkaTemplate).send(eq(KafkaTopicConfig.EMAIL_TOPIC), messageCaptor.capture());
            Assertions.assertThat(messageCaptor.getValue()).isEqualTo(emailEvent);
        }

        @Test
        @DisplayName("Should handle email notification with all fields")
        void shouldHandleEmailNotificationWithAllFields() {
            EmailEvent emailEvent = createFullEmailEvent();
            CompletableFuture<SendResult<String, Object>> future = createSuccessfulSendResult(
                    KafkaTopicConfig.EMAIL_TOPIC, 1, 456L);
            when(kafkaTemplate.send(eq(KafkaTopicConfig.EMAIL_TOPIC), any())).thenReturn(future);

            CompletableFuture<SendResult<String, Object>> result = producer.publishEmailNotification(emailEvent);

            Assertions.assertThat(result).isCompleted();
            verify(kafkaTemplate).send(eq(KafkaTopicConfig.EMAIL_TOPIC), messageCaptor.capture());
            EmailEvent captured = (EmailEvent) messageCaptor.getValue();
            Assertions.assertThat(captured.getTo()).containsExactlyInAnyOrder("test@example.com");
            Assertions.assertThat(captured.getCc()).containsExactlyInAnyOrder("cc@example.com");
            Assertions.assertThat(captured.getBcc()).containsExactlyInAnyOrder("bcc@example.com");
        }

        @Test
        @DisplayName("Should complete exceptionally when Kafka fails")
        void shouldCompleteExceptionallyWhenKafkaFails() {
            EmailEvent emailEvent = createValidEmailEvent();
            CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("Kafka unavailable"));
            when(kafkaTemplate.send(eq(KafkaTopicConfig.EMAIL_TOPIC), any())).thenReturn(failedFuture);

            CompletableFuture<SendResult<String, Object>> result = producer.publishEmailNotification(emailEvent);

            Assertions.assertThat(result).isCompletedExceptionally();
        }
    }

    @Nested
    @DisplayName("SMS Notification Tests")
    class SmsNotificationTests {

        @Test
        @DisplayName("Should publish SMS notification to correct topic")
        void shouldPublishSmsNotificationToCorrectTopic() {
            SmsEvent smsEvent = new SmsEvent("+1234567890", "Test SMS message");
            CompletableFuture<SendResult<String, Object>> future = createSuccessfulSendResult(
                    KafkaTopicConfig.SMS_TOPIC, 0, 789L);
            when(kafkaTemplate.send(eq(KafkaTopicConfig.SMS_TOPIC), any())).thenReturn(future);

            CompletableFuture<SendResult<String, Object>> result = producer.sendSmsNotification(smsEvent);

            Assertions.assertThat(result).isCompletedWithValueMatching(
                    sendResult -> sendResult.getRecordMetadata().offset() == 789L);
            verify(kafkaTemplate).send(eq(KafkaTopicConfig.SMS_TOPIC), messageCaptor.capture());
            SmsEvent captured = (SmsEvent) messageCaptor.getValue();
            Assertions.assertThat(captured.to()).isEqualTo("+1234567890");
            Assertions.assertThat(captured.message()).isEqualTo("Test SMS message");
        }

        @Test
        @DisplayName("Should complete exceptionally when Kafka fails for SMS")
        void shouldCompleteExceptionallyWhenKafkaFailsForSms() {
            SmsEvent smsEvent = new SmsEvent("+1234567890", "Test SMS");
            CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("Kafka unavailable"));
            when(kafkaTemplate.send(eq(KafkaTopicConfig.SMS_TOPIC), any())).thenReturn(failedFuture);

            CompletableFuture<SendResult<String, Object>> result = producer.sendSmsNotification(smsEvent);

            Assertions.assertThat(result).isCompletedExceptionally();
        }
    }
    
    @Nested
    @DisplayName("Push Notification Tests")
    class PushNotificationTests {

        @Captor
        private ArgumentCaptor<String> keyCaptor;

        @Test
        @DisplayName("Should publish push notification with correct key and topic")
        void shouldPublishPushNotificationWithCorrectKeyAndTopic() {
            UUID userId = UUID.randomUUID();
            PushNotificationEvent event = createValidPushEvent(userId, RecipientRole.WORKER);
            String expectedKey = userId + "_WORKER";

            CompletableFuture<SendResult<String, Object>> future = createSuccessfulSendResult(
                    KafkaTopicConfig.PUSH_TOPIC, 0, 999L);
            
            when(kafkaTemplate.send(eq(KafkaTopicConfig.PUSH_TOPIC), anyString(), any()))
                    .thenReturn(future);

            CompletableFuture<SendResult<String, Object>> result = producer.sendPushNotification(event);

            Assertions.assertThat(result).isCompleted();
            
            verify(kafkaTemplate).send(
                eq(KafkaTopicConfig.PUSH_TOPIC), 
                keyCaptor.capture(), 
                messageCaptor.capture()
            );
            
            Assertions.assertThat(keyCaptor.getValue()).isEqualTo(expectedKey);
            Assertions.assertThat(messageCaptor.getValue()).isEqualTo(event);
        }

        @Test
        @DisplayName("Should handle logging and completion on Kafka failure")
        void shouldHandleKafkaFailureForPush() {
            PushNotificationEvent event = createValidPushEvent(UUID.randomUUID(), RecipientRole.CLIENT);
            CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("Broker Timeout"));
            
            when(kafkaTemplate.send(eq(KafkaTopicConfig.PUSH_TOPIC), anyString(), any()))
                    .thenReturn(failedFuture);

            CompletableFuture<SendResult<String, Object>> result = producer.sendPushNotification(event);

            Assertions.assertThat(result).isCompletedExceptionally();
        }
    }

    private PushNotificationEvent createValidPushEvent(UUID userId, RecipientRole role) {
        PushNotificationEvent event = new PushNotificationEvent();
        event.setUserId(userId);
        event.setRecipientRole(role);
        event.setTitle("Test Title");
        event.setBody("Test Body");
        return event;
    }

    private EmailEvent createValidEmailEvent() {
        EmailEvent event = new EmailEvent();
        event.setTo(Set.of("test@example.com"));
        event.setSubject("Test Subject");
        event.setContent("Test Content");
        event.setHtml(true);
        event.setPriority(3);
        return event;
    }

    private EmailEvent createFullEmailEvent() {
        EmailEvent event = new EmailEvent();
        event.setTo(Set.of("test@example.com"));
        event.setCc(Set.of("cc@example.com"));
        event.setBcc(Set.of("bcc@example.com"));
        event.setSubject("Full Test Subject");
        event.setContent("<html><body>Test HTML Content</body></html>");
        event.setHtml(true);
        event.setPriority(1);
        return event;
    }

    private CompletableFuture<SendResult<String, Object>> createSuccessfulSendResult(
            String topic, int partition, long offset) {
        TopicPartition topicPartition = new TopicPartition(topic, partition);
        RecordMetadata recordMetadata = new RecordMetadata(topicPartition, offset, 0, 0L, 0, 0);
        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(topic, "test");
        SendResult<String, Object> sendResult = new SendResult<>(producerRecord, recordMetadata);
        return CompletableFuture.completedFuture(sendResult);
    }
}
