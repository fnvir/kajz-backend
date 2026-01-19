package dev.fnvir.kajz.notificationservice.service.push;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.fnvir.kajz.notificationservice.config.KafkaTopicConfig;
import dev.fnvir.kajz.notificationservice.dto.event.PushNotificationEvent;
import dev.fnvir.kajz.notificationservice.dto.res.NotificationResponse;
import dev.fnvir.kajz.notificationservice.model.enums.RecipientRole;
import dev.fnvir.kajz.notificationservice.service.NotificationService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("PushNotificationEventListener Tests")
public class PushNotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private PushNotificationSseService sseService;
    
    @Spy
    private JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();

    @InjectMocks
    private PushNotificationEventListener notificationEventListener;

    @Captor
    private ArgumentCaptor<NotificationResponse> notificationCaptor;

    private static final String TOPIC = KafkaTopicConfig.PUSH_TOPIC;
    private static final int PARTITION = 0;
    private static final long OFFSET = 123L;
    
    
    @Nested
    @DisplayName("Consume Push Notification Tests")
    class ConsumePushNotificationTests {

        @Test
        @DisplayName("Should successfully process valid notification event")
        void shouldProcessValidNotificationEvent() throws Exception {
            PushNotificationEvent event = createPushNotificationEvent();
            NotificationResponse savedNotification = createNotificationResponse(event);
            
            String payload = jsonMapper.writeValueAsString(event);

            when(notificationService.saveNotification(event)).thenReturn(savedNotification);
            doNothing().when(sseService).publish(any(NotificationResponse.class));

            notificationEventListener.consumePushNotification(payload, TOPIC, PARTITION, OFFSET);

            verify(notificationService).saveNotification(event);
            verify(sseService).publish(notificationCaptor.capture());

            NotificationResponse captured = notificationCaptor.getValue();
            assertEquals(captured, savedNotification);
        }

        @Test
        @DisplayName("Should save notification before publishing to SSE")
        void shouldSaveBeforePublishing() throws Exception {
            PushNotificationEvent event = createPushNotificationEvent();
            NotificationResponse savedNotification = createNotificationResponse(event);
            String payload = jsonMapper.writeValueAsString(event);

            when(notificationService.saveNotification(event)).thenReturn(savedNotification);

            notificationEventListener.consumePushNotification(payload, TOPIC, PARTITION, OFFSET);

            var inOrder = inOrder(notificationService, sseService);
            inOrder.verify(notificationService).saveNotification(event);
            inOrder.verify(sseService).publish(savedNotification);
        }

        @Test
        @DisplayName("Should ignore invalid PushNotification events")
        void shouldIgnoreInvalidEventPayload() {
            String invalidPayload = "invalid-json";

            doThrow(JacksonException.class).when(jsonMapper).readValue(invalidPayload, PushNotificationEvent.class);
            
            assertDoesNotThrow(() -> notificationEventListener.consumePushNotification(invalidPayload, TOPIC, PARTITION, OFFSET));

            verify(notificationService, never()).saveNotification(any());
            verify(sseService, never()).publish(any());
        }

        @Test
        @DisplayName("Should continue processing even if SSE publish fails")
        void shouldContinueOnSsePublishFailure() throws Exception {
            PushNotificationEvent event = createPushNotificationEvent();
            NotificationResponse savedNotification = createNotificationResponse(event);
            String payload = jsonMapper.writeValueAsString(event);

            when(notificationService.saveNotification(event)).thenReturn(savedNotification);
            doThrow(new RuntimeException("SSE error")).when(sseService).publish(any());

            assertThatCode(() -> notificationEventListener.consumePushNotification(payload, TOPIC, PARTITION, OFFSET))
                    .doesNotThrowAnyException();

            verify(notificationService).saveNotification(event);
            verify(sseService).publish(savedNotification);
        }

        @Test
        @DisplayName("Should process notification with all required fields")
        void shouldProcessNotificationWithAllFields() throws Exception {
            PushNotificationEvent event = createPushNotificationEvent();
            NotificationResponse savedNotification = createNotificationResponse();
            String payload = jsonMapper.writeValueAsString(event);

            when(notificationService.saveNotification(event)).thenReturn(savedNotification);

            notificationEventListener.consumePushNotification(payload, TOPIC, PARTITION, OFFSET);

            verify(notificationService).saveNotification(event);
            verify(sseService).publish(savedNotification);
        }

        @Test
        @DisplayName("Should handle notification service exception")
        void shouldHandleNotificationServiceException() throws Exception {
            PushNotificationEvent event = createPushNotificationEvent();
            String payload = jsonMapper.writeValueAsString(event);

            when(notificationService.saveNotification(event)).thenThrow(new RuntimeException("Database error"));

            assertThatCode(() -> notificationEventListener.consumePushNotification(payload, TOPIC, PARTITION, OFFSET))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database error");

            verify(sseService, never()).publish(any());
        }
    }

    @Nested
    @DisplayName("DLT Handler Tests")
    class DltHandlerTests {

        @Test
        @DisplayName("Should handle DLQ message without throwing exception")
        void shouldHandleDlqMessage() {
            String payload = "{\"invalid\":\"data\"}";
            String dlqTopic = TOPIC + "-dlt";

            assertThatCode(() -> notificationEventListener.handlePushNotificationDlq(payload, dlqTopic))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should process DLQ with retry topic")
        void shouldProcessDlqWithRetryTopic() {
            String payload = "failed-payload";
            String retryTopic = TOPIC + "-retry-0";

            notificationEventListener.handlePushNotificationDlq(payload, retryTopic);

            assertThatCode(() -> notificationEventListener.handlePushNotificationDlq(payload, retryTopic))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Kafka Headers Tests")
    class KafkaHeadersTests {

        @Test
        @DisplayName("Should process message with correct Kafka headers")
        void shouldProcessWithKafkaHeaders() throws Exception {
            PushNotificationEvent event = createPushNotificationEvent();
            NotificationResponse savedNotification = createNotificationResponse(event);
            String payload = jsonMapper.writeValueAsString(event);
            String topic = "test-topic";
            int partition = 5;
            long offset = 999L;

            when(notificationService.saveNotification(event)).thenReturn(savedNotification);

            notificationEventListener.consumePushNotification(payload, topic, partition, offset);

            verify(notificationService).saveNotification(event);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null metadata in notification")
        void shouldHandleNullMetadata() throws Exception {
            PushNotificationEvent event = createPushNotificationEvent();
            NotificationResponse savedNotification = createNotificationResponse(event);
            
            String payload = jsonMapper.writeValueAsString(event);

            when(notificationService.saveNotification(event)).thenReturn(savedNotification);

            notificationEventListener.consumePushNotification(payload, TOPIC, PARTITION, OFFSET);

            verify(notificationService).saveNotification(event);
            verify(sseService).publish(savedNotification);
        }

        @Test
        @DisplayName("Should handle empty payload gracefully")
        void shouldHandleEmptyPayload() throws Exception {
            String emptyPayload = "";

            notificationEventListener.consumePushNotification(emptyPayload, TOPIC, PARTITION, OFFSET);

            verify(notificationService, never()).saveNotification(any());
            verify(sseService, never()).publish(any());
        }

        @Test
        @DisplayName("Should handle multiple rapid notifications")
        void shouldHandleMultipleRapidNotifications() throws Exception {
            PushNotificationEvent event1 = createPushNotificationEvent();
            PushNotificationEvent event2 = createPushNotificationEvent();
            NotificationResponse response1 = createNotificationResponse(event1);
            NotificationResponse response2 = createNotificationResponse(event2);
            String payload1 = jsonMapper.writeValueAsString(event1);
            String payload2 = jsonMapper.writeValueAsString(event2);

            when(notificationService.saveNotification(event1)).thenReturn(response1);
            when(notificationService.saveNotification(event2)).thenReturn(response2);

            notificationEventListener.consumePushNotification(payload1, TOPIC, PARTITION, OFFSET);
            notificationEventListener.consumePushNotification(payload2, TOPIC, PARTITION, OFFSET + 1);

            verify(notificationService, times(2)).saveNotification(any());
            verify(sseService, times(2)).publish(any());
        }
    }

    @Nested
    @DisplayName("Integration Flow Tests")
    class IntegrationFlowTests {

        @Test
        @DisplayName("Should complete full flow from Kafka to SSE")
        void shouldCompleteFullFlow() throws Exception {
            PushNotificationEvent event = createPushNotificationEvent();
            NotificationResponse savedNotification = createNotificationResponse(event);
            
            String payload = jsonMapper.writeValueAsString(event);

            when(notificationService.saveNotification(event)).thenReturn(savedNotification);

            notificationEventListener.consumePushNotification(payload, TOPIC, PARTITION, OFFSET);

            verify(jsonMapper).readValue(payload, PushNotificationEvent.class);
            verify(notificationService).saveNotification(event);
            verify(sseService).publish(savedNotification);
            
            var inOrder = inOrder(jsonMapper, notificationService, sseService);
            inOrder.verify(jsonMapper).readValue(anyString(), eq(PushNotificationEvent.class));
            inOrder.verify(notificationService).saveNotification(any());
            inOrder.verify(sseService).publish(any());
        }
    }

    // Helper methods
    
    private PushNotificationEvent createPushNotificationEvent() {
        var pn = new PushNotificationEvent();
        pn.setUserId(UUID.randomUUID());
        pn.setRecipientRole(RecipientRole.CLIENT);
        pn.setTitle("Push notification");
        pn.setBody("Test notification body");
        pn.setType("NEW_ORDER_TEST");
        return pn;
    }
    
    private NotificationResponse createNotificationResponse(PushNotificationEvent event) {
        return NotificationResponse.builder()
                .id(UUID.randomUUID())
                .userId(event.getUserId())
                .recipientRole(event.getRecipientRole())
                .title(event.getTitle())
                .body(event.getBody())
                .type(event.getType())
                .createdAt(Instant.now())
                .read(false)
                .archived(false)
                .build();
    }

    private NotificationResponse createNotificationResponse() {
        return NotificationResponse.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .recipientRole(RecipientRole.CLIENT)
                .title("Test Notification")
                .body("Test notification body")
                .type("NEW_ORDER_TEST")
                .createdAt(Instant.now())
                .read(false)
                .archived(false)
                .build();
    }

}