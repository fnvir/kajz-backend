package dev.fnvir.kajz.notificationservice.service.email;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serial;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.fnvir.kajz.notificationservice.config.KafkaTopicConfig;
import dev.fnvir.kajz.notificationservice.dto.event.EmailEvent;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailEventListener Unit Tests")
class EmailEventListenerTest {

    @Mock
    private EmailSenderService emailService;

    @Mock
    private JsonMapper jsonMapper;

    private EmailEventListener emailEventListener;

    @BeforeEach
    void setUp() {
        emailEventListener = new EmailEventListener(emailService, jsonMapper);
    }

    @Nested
    @DisplayName("Email Event Handling Tests")
    class EmailEventHandlingTests {

        @Test
        @DisplayName("Should process valid email event successfully")
        void shouldProcessValidEmailEventSuccessfully() throws Exception {
            String payload = """
                    {"to":["test@example.com"],"subject":"Test","content":"Body","isHtml":true,"priority":3}
                    """;
            EmailEvent emailEvent = createEmailEvent();
            when(jsonMapper.readValue(payload, EmailEvent.class)).thenReturn(emailEvent);

            emailEventListener.handleEmailEvent(payload, KafkaTopicConfig.EMAIL_TOPIC, 0, 100L);

            verify(emailService).sendEmail(
                    eq(emailEvent.getTo()),
                    eq(emailEvent.getCc()),
                    eq(emailEvent.getBcc()),
                    eq(emailEvent.getSubject()),
                    eq(emailEvent.getContent()),
                    eq(emailEvent.isHtml()),
                    eq(emailEvent.getPriority())
            );
        }

        @Test
        @DisplayName("Should process email event with CC and BCC")
        void shouldProcessEmailEventWithCcAndBcc() throws Exception {
            String payload = """
                    {"to":["test@example.com"],"cc":["cc@example.com"],"bcc":["bcc@example.com"],"subject":"Test","content":"Body","isHtml":true,"priority":1}
                    """;
            EmailEvent emailEvent = createEmailEventWithCcBcc();
            when(jsonMapper.readValue(payload, EmailEvent.class)).thenReturn(emailEvent);

            emailEventListener.handleEmailEvent(payload, KafkaTopicConfig.EMAIL_TOPIC, 1, 200L);

            verify(emailService).sendEmail(
                    eq(emailEvent.getTo()),
                    eq(emailEvent.getCc()),
                    eq(emailEvent.getBcc()),
                    eq(emailEvent.getSubject()),
                    eq(emailEvent.getContent()),
                    eq(true),
                    eq(1)
            );
        }

        @Test
        @DisplayName("Should not send email when deserialization returns null")
        void shouldNotSendEmailWhenDeserializationReturnsNull() throws Exception {
            String payload = "{}";
            when(jsonMapper.readValue(payload, EmailEvent.class)).thenReturn(null);

            emailEventListener.handleEmailEvent(payload, KafkaTopicConfig.EMAIL_TOPIC, 0, 100L);

            verify(emailService, never()).sendEmail(any(), any(), any(), any(), any(), anyBoolean(), anyInt());
        }

        @Test
        @DisplayName("Should skip event when JSON deserialization fails")
        void shouldSkipEventWhenJsonDeserializationFails() throws Exception {
            String invalidPayload = "invalid json";
            when(jsonMapper.readValue(anyString(), eq(EmailEvent.class)))
                    .thenThrow(new TestJacksonException("Parse error"));

            emailEventListener.handleEmailEvent(invalidPayload, KafkaTopicConfig.EMAIL_TOPIC, 0, 100L);

            verify(emailService, never()).sendEmail(any(), any(), any(), any(), any(), anyBoolean(), anyInt());
        }
    }

    @Nested
    @DisplayName("Dead Letter Queue Handler Tests")
    class DltHandlerTests {

        @Test
        @DisplayName("Should handle DLT event without throwing exception")
        void shouldHandleDltEventWithoutThrowingException() {
            String failedPayload = "failed event payload";

            emailEventListener.handleEmailDlt(failedPayload, "notification.email-dlt");
        }
    }

    private EmailEvent createEmailEvent() {
        EmailEvent event = new EmailEvent();
        event.setTo(Set.of("test@example.com"));
        event.setSubject("Test");
        event.setContent("Body");
        event.setHtml(true);
        event.setPriority(3);
        return event;
    }

    private EmailEvent createEmailEventWithCcBcc() {
        EmailEvent event = new EmailEvent();
        event.setTo(Set.of("test@example.com"));
        event.setCc(Set.of("cc@example.com"));
        event.setBcc(Set.of("bcc@example.com"));
        event.setSubject("Test");
        event.setContent("Body");
        event.setHtml(true);
        event.setPriority(1);
        return event;
    }

    private static class TestJacksonException extends JacksonException {
        @Serial
        private static final long serialVersionUID = 1L;

        TestJacksonException(String message) {
            super(message);
        }
    }
}
