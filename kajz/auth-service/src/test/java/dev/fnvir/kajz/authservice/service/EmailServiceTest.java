package dev.fnvir.kajz.authservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.fnvir.kajz.authservice.dto.event.EmailEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Unit Tests")
public class EmailServiceTest {

    @Mock
    private KafkaEventProducer eventProducer;

    @InjectMocks
    private EmailService emailService;

    private static final String TEST_EMAIL = "recipient@example.com";
    private static final String TEST_SUBJECT = "Test Subject";
    private static final String TEST_CONTENT = "<html><body>Test Content</body></html>";

    @Captor
    private ArgumentCaptor<EmailEvent> emailEventCaptor;

    @Nested
    @DisplayName("sendEmail tests")
    class SendEmailTests {

        @Test
        @DisplayName("Should publish email event to Kafka successfully")
        void shouldPublishEmailEventToKafkaSuccessfully() {
            when(eventProducer.publishEmailNotification(any(EmailEvent.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            emailService.sendEmail(TEST_EMAIL, TEST_SUBJECT, TEST_CONTENT, true);

            verify(eventProducer).publishEmailNotification(any(EmailEvent.class));
        }

        @Test
        @DisplayName("Should create EmailEvent with correct properties for single recipient")
        void shouldCreateEmailEventWithCorrectProperties() {
            when(eventProducer.publishEmailNotification(any(EmailEvent.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            emailService.sendEmail(TEST_EMAIL, TEST_SUBJECT, TEST_CONTENT, true);

            verify(eventProducer).publishEmailNotification(emailEventCaptor.capture());
            EmailEvent capturedEvent = emailEventCaptor.getValue();

            Assertions.assertThat(capturedEvent.getTo()).containsExactly(TEST_EMAIL);
            Assertions.assertThat(capturedEvent.getSubject()).isEqualTo(TEST_SUBJECT);
            Assertions.assertThat(capturedEvent.getContent()).isEqualTo(TEST_CONTENT);
            Assertions.assertThat(capturedEvent.isHtml()).isTrue();
            Assertions.assertThat(capturedEvent.getPriority()).isEqualTo(2);
        }

    }

    @Nested
    @DisplayName("Email content tests")
    class EmailContentTests {

        @Test
        @DisplayName("Should handle HTML content correctly")
        void shouldHandleHtmlContentCorrectly() {
            when(eventProducer.publishEmailNotification(any(EmailEvent.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            emailService.sendEmail(TEST_EMAIL, TEST_SUBJECT, TEST_CONTENT, true);

            verify(eventProducer).publishEmailNotification(emailEventCaptor.capture());
            EmailEvent capturedEvent = emailEventCaptor.getValue();

            Assertions.assertThat(capturedEvent.getContent()).isEqualTo(TEST_CONTENT);
            Assertions.assertThat(capturedEvent.isHtml()).isTrue();
        }

        @Test
        @DisplayName("Should handle plain text content correctly")
        void shouldHandlePlainTextContentCorrectly() {
            when(eventProducer.publishEmailNotification(any(EmailEvent.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            String plainText = "This is plain text content";
            emailService.sendEmail(TEST_EMAIL, TEST_SUBJECT, plainText, false);

            verify(eventProducer).publishEmailNotification(emailEventCaptor.capture());
            EmailEvent capturedEvent = emailEventCaptor.getValue();

            Assertions.assertThat(capturedEvent.getContent()).isEqualTo(plainText);
            Assertions.assertThat(capturedEvent.isHtml()).isFalse();
        }
    }

    @Nested
    @DisplayName("EmailEvent validation tests")
    class EmailEventValidationTests {

        @Test
        @DisplayName("Should create valid EmailEvent with all required fields")
        void shouldCreateValidEmailEventWithAllRequiredFields() {
            when(eventProducer.publishEmailNotification(any(EmailEvent.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            emailService.sendEmail(TEST_EMAIL, TEST_SUBJECT, TEST_CONTENT, true);

            verify(eventProducer).publishEmailNotification(emailEventCaptor.capture());
            EmailEvent capturedEvent = emailEventCaptor.getValue();

            Assertions.assertThat(capturedEvent.getTo()).isNotEmpty();
            Assertions.assertThat(capturedEvent.getSubject()).isNotBlank();
            Assertions.assertThat(capturedEvent.getContent()).isNotBlank();
        }

    }
}
