package dev.fnvir.kajz.notificationservice.service.sms;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.fnvir.kajz.notificationservice.config.KafkaTopicConfig;
import dev.fnvir.kajz.notificationservice.dto.event.SmsEvent;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsEventListener Unit Tests")
class SmsEventListenerTest {

    @Mock
    private SmsSenderService smsService;

    @Mock
    private JsonMapper jsonMapper;

    private SmsEventListener smsEventListener;

    @BeforeEach
    void setUp() {
        smsEventListener = new SmsEventListener(smsService, jsonMapper);
    }

    @Nested
    @DisplayName("SMS Event Handling Tests")
    class SmsEventHandlingTests {

        @Test
        @DisplayName("Should process valid SMS event successfully")
        void shouldProcessValidSmsEventSuccessfully() throws Exception {
            String payload = """
                    {"to":"+1234567890","message":"Test SMS message"}
                    """;
            SmsEvent smsEvent = new SmsEvent("+1234567890", "Test SMS message");
            when(jsonMapper.readValue(payload, SmsEvent.class)).thenReturn(smsEvent);

            smsEventListener.handleSmsEvent(payload, KafkaTopicConfig.SMS_TOPIC, 0, 100L);

            verify(smsService).sendSms("+1234567890", "Test SMS message");
        }

        @Test
        @DisplayName("Should process SMS event with international number")
        void shouldProcessSmsEventWithInternationalNumber() throws Exception {
            String payload = """
                    {"to":"+447911123456","message":"International SMS"}
                    """;
            SmsEvent smsEvent = new SmsEvent("+447911123456", "International SMS");
            when(jsonMapper.readValue(payload, SmsEvent.class)).thenReturn(smsEvent);

            smsEventListener.handleSmsEvent(payload, KafkaTopicConfig.SMS_TOPIC, 1, 200L);

            verify(smsService).sendSms("+447911123456", "International SMS");
        }

        @Test
        @DisplayName("Should not send SMS when deserialization returns null")
        void shouldNotSendSmsWhenDeserializationReturnsNull() throws Exception {
            String payload = "{}";
            when(jsonMapper.readValue(payload, SmsEvent.class)).thenReturn(null);

            smsEventListener.handleSmsEvent(payload, KafkaTopicConfig.SMS_TOPIC, 0, 100L);

            verify(smsService, never()).sendSms(anyString(), anyString());
        }

        @Test
        @DisplayName("Should skip event when JSON deserialization fails")
        void shouldSkipEventWhenJsonDeserializationFails() throws Exception {
            String invalidPayload = "invalid json";
            when(jsonMapper.readValue(anyString(), eq(SmsEvent.class)))
                    .thenThrow(new TestJacksonException("Parse error"));

            smsEventListener.handleSmsEvent(invalidPayload, KafkaTopicConfig.SMS_TOPIC, 0, 100L);

            verify(smsService, never()).sendSms(anyString(), anyString());
        }

        @Test
        @DisplayName("Should handle event from different partitions")
        void shouldHandleEventFromDifferentPartitions() throws Exception {
            String payload = """
                    {"to":"+1234567890","message":"Partition test"}
                    """;
            SmsEvent smsEvent = new SmsEvent("+1234567890", "Partition test");
            when(jsonMapper.readValue(payload, SmsEvent.class)).thenReturn(smsEvent);

            smsEventListener.handleSmsEvent(payload, KafkaTopicConfig.SMS_TOPIC, 0, 100L);
            smsEventListener.handleSmsEvent(payload, KafkaTopicConfig.SMS_TOPIC, 1, 200L);
            smsEventListener.handleSmsEvent(payload, KafkaTopicConfig.SMS_TOPIC, 2, 300L);

            verify(smsService, org.mockito.Mockito.times(3)).sendSms("+1234567890", "Partition test");
        }
    }

    @Nested
    @DisplayName("Dead Letter Queue Handler Tests")
    class DltHandlerTests {

        @Test
        @DisplayName("Should handle DLT event without throwing exception")
        void shouldHandleDltEventWithoutThrowingException() {
            String failedPayload = "failed sms payload";

            smsEventListener.handleSmsDlt(failedPayload, "notification.sms-dlt");
        }

        @Test
        @DisplayName("Should handle DLT event with retry topic suffix")
        void shouldHandleDltEventWithRetryTopicSuffix() {
            String failedPayload = """
                    {"to":"invalid","message":"Failed message"}
                    """;

            smsEventListener.handleSmsDlt(failedPayload, "notification.sms-0-dlt");
        }
    }

    private static class TestJacksonException extends JacksonException {
        @Serial
        private static final long serialVersionUID = 383410719118196426L;

        TestJacksonException(String message) {
            super(message);
        }
    }
}
