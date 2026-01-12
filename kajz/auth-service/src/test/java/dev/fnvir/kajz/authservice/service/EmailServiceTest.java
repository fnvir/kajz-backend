package dev.fnvir.kajz.authservice.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import dev.fnvir.kajz.authservice.config.EmailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Unit Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender emailSender;

    @Mock
    private EmailProperties emailProperties;

    @Mock
    private MimeMessage mimeMessage;

    @Captor
    private ArgumentCaptor<MimeMessage> messageCaptor;

    private EmailService emailService;

    private static final String TEST_EMAIL = "recipient@example.com";
    private static final String TEST_SUBJECT = "Test Subject";
    private static final String TEST_CONTENT = "<html><body>Test Content</body></html>";
    private static final String SENDER_EMAIL = "sender@example.com";

    @BeforeEach
    void setUp() {
        emailService = new EmailService(emailSender, emailProperties);
    }

    @Nested
    @DisplayName("sendEmailAsync (single recipient) tests")
    class SendEmailAsyncSingleRecipientTests {

        @Test
        @DisplayName("Should send email to single recipient successfully")
        void shouldSendEmailToSingleRecipientSuccessfully() {
            when(emailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailProperties.getUsername()).thenReturn(SENDER_EMAIL);

            emailService.sendEmailAsync(TEST_EMAIL, TEST_SUBJECT, TEST_CONTENT, true);

            verify(emailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should create MimeMessage with correct properties")
        void shouldCreateMimeMessageWithCorrectProperties() {
            when(emailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailProperties.getUsername()).thenReturn(SENDER_EMAIL);

            emailService.sendEmailAsync(TEST_EMAIL, TEST_SUBJECT, TEST_CONTENT, true);

            verify(emailSender).createMimeMessage();
            verify(emailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should throw RuntimeException when email sending fails")
        void shouldThrowRuntimeExceptionWhenEmailSendingFails() {
            when(emailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailProperties.getUsername()).thenReturn(SENDER_EMAIL);
            doThrow(new RuntimeException("Mail server error")).when(emailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> emailService.sendEmailAsync(TEST_EMAIL, TEST_SUBJECT, TEST_CONTENT, true))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("sendEmailAsync (multiple recipients) tests")
    class SendEmailAsyncMultipleRecipientsTests {

        @Test
        @DisplayName("Should send email to multiple recipients successfully")
        void shouldSendEmailToMultipleRecipientsSuccessfully() {
            when(emailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailProperties.getUsername()).thenReturn(SENDER_EMAIL);

            List<String> recipients = List.of("user1@example.com", "user2@example.com");
            List<String> cc = List.of("cc@example.com");
            List<String> bcc = List.of("bcc@example.com");

            emailService.sendEmailAsync(recipients, cc, bcc, TEST_SUBJECT, TEST_CONTENT, true, 1);

            verify(emailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should handle null CC and BCC lists")
        void shouldHandleNullCcAndBccLists() {
            when(emailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailProperties.getUsername()).thenReturn(SENDER_EMAIL);

            List<String> recipients = List.of(TEST_EMAIL);

            emailService.sendEmailAsync(recipients, null, null, TEST_SUBJECT, TEST_CONTENT, true, 2);

            verify(emailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should handle empty CC and BCC lists")
        void shouldHandleEmptyCcAndBccLists() {
            when(emailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailProperties.getUsername()).thenReturn(SENDER_EMAIL);

            List<String> recipients = List.of(TEST_EMAIL);

            emailService.sendEmailAsync(recipients, List.of(), List.of(), TEST_SUBJECT, TEST_CONTENT, false, 3);

            verify(emailSender).send(mimeMessage);
        }

    }

    @Nested
    @DisplayName("Email content tests")
    class EmailContentTests {

        @Test
        @DisplayName("Should send HTML content when isHtml is true")
        void shouldSendHtmlContentWhenIsHtmlTrue() {
            when(emailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailProperties.getUsername()).thenReturn(SENDER_EMAIL);

            emailService.sendEmailAsync(TEST_EMAIL, TEST_SUBJECT, TEST_CONTENT, true);

            verify(emailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should send plain text content when isHtml is false")
        void shouldSendPlainTextContentWhenIsHtmlFalse() {
            when(emailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailProperties.getUsername()).thenReturn(SENDER_EMAIL);

            String plainText = "This is plain text content";
            emailService.sendEmailAsync(TEST_EMAIL, TEST_SUBJECT, plainText, false);

            verify(emailSender).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("Error handling tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should wrap MessagingException in RuntimeException")
        void shouldWrapMessagingExceptionInRuntimeException() {
            when(emailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(emailProperties.getUsername()).thenReturn(SENDER_EMAIL);
            doThrow(new RuntimeException(new MessagingException("SMTP error")))
                    .when(emailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> emailService.sendEmailAsync(TEST_EMAIL, TEST_SUBJECT, TEST_CONTENT, true))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
