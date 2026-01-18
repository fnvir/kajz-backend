package dev.fnvir.kajz.notificationservice.service.email;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.mail.javamail.JavaMailSender;

import dev.fnvir.kajz.notificationservice.config.EmailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailSenderService Unit Tests")
class EmailSenderServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailProperties emailProperties;

    @Captor
    private ArgumentCaptor<MimeMessage> messageCaptor;

    @Spy
    @InjectMocks
    private EmailSenderService emailService;

    private MimeMessage mimeMessage;
    
    @BeforeEach
    void setUp() {
        mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(emailProperties.getSenderAddress()).thenReturn("sender@test.com");
    }

    @Nested
    @DisplayName("Synchronous Email Sending Tests")
    class SyncEmailTests {

        @Test
        @DisplayName("Should send email successfully with required fields only")
        void shouldSendEmailSuccessfullyWithRequiredFieldsOnly() {
            List<String> to = List.of("recipient@test.com");
            doNothing().when(mailSender).send(any(MimeMessage.class));

            emailService.sendEmail(to, null, null, "Test Subject", "Test Content", false, 3);

            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should send email with CC and BCC recipients")
        void shouldSendEmailWithCcAndBccRecipients() {
            List<String> to = List.of("recipient@test.com");
            List<String> cc = List.of("cc@test.com");
            List<String> bcc = List.of("bcc@test.com");
            doNothing().when(mailSender).send(any(MimeMessage.class));
            
            emailService.sendEmail(to, cc, bcc, "Subject", "Content", true, 1);

            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should send HTML email")
        void shouldSendHtmlEmail() {
            List<String> to = List.of("recipient@test.com");
            String htmlContent = "<html><body><h1>Hello</h1></body></html>";
            doNothing().when(mailSender).send(any(MimeMessage.class));

            emailService.sendEmail(to, null, null, "HTML Subject", htmlContent, true, 3);

            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should send email with high priority")
        void shouldSendEmailWithHighPriority() {
            List<String> to = List.of("recipient@test.com");
            doNothing().when(mailSender).send(any(MimeMessage.class));

            emailService.sendEmail(to, null, null, "Urgent", "Important message", false, 1);

            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should send email to multiple recipients")
        void shouldSendEmailToMultipleRecipients() {
            List<String> to = List.of("recipient1@test.com", "recipient2@test.com", "recipient3@test.com");
            doNothing().when(mailSender).send(any(MimeMessage.class));

            emailService.sendEmail(to, null, null, "Bulk Subject", "Bulk Content", false, 3);

            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should throw RuntimeException when MessagingException occurs")
        void shouldThrowRuntimeExceptionWhenMessagingExceptionOccurs() {
            List<String> to = List.of("recipient@test.com");
            doThrow(new RuntimeException(new MessagingException("SMTP Error")))
                    .when(mailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> 
                emailService.sendEmail(to, null, null, "Subject", "Content", false, 3))
                .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Asynchronous Email Sending Tests")
    class AsyncEmailTests {

        @Test
        @DisplayName("Should send email asynchronously and return true on success")
        void shouldSendEmailAsyncAndReturnTrueOnSuccess() {
            Set<String> to = Set.of("recipient@test.com");
            doNothing().when(mailSender).send(any(MimeMessage.class));

            StepVerifier.create(emailService.sendEmailMono(to, null, null, "Subject", "Content", false, 3))
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return false when email sending fails asynchronously")
        void shouldReturnFalseWhenEmailSendingFailsAsync() {
            Set<String> to = Set.of("recipient@test.com");
            doThrow(new RuntimeException(new MessagingException("SMTP Error")))
                    .when(mailSender).send(any(MimeMessage.class));

            StepVerifier.create(emailService.sendEmailMono(to, null, null, "Subject", "Content", false, 3))
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should send async email with all optional fields")
        void shouldSendAsyncEmailWithAllOptionalFields() {
            Set<String> to = Set.of("recipient@test.com");
            Set<String> cc = Set.of("cc@test.com");
            Set<String> bcc = Set.of("bcc@test.com");
            doNothing().when(mailSender).send(any(MimeMessage.class));

            StepVerifier.create(emailService.sendEmailMono(to, cc, bcc, "Subject", "<html>Content</html>", true, 2))
                    .expectNext(true)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty CC list")
        void shouldHandleEmptyCcList() {
            List<String> to = List.of("recipient@test.com");
            List<String> emptyCc = List.of();
            doNothing().when(mailSender).send(any(MimeMessage.class));

            emailService.sendEmail(to, emptyCc, null, "Subject", "Content", false, 3);

            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should handle empty BCC list")
        void shouldHandleEmptyBccList() {
            List<String> to = List.of("recipient@test.com");
            List<String> emptyBcc = List.of();
            doNothing().when(mailSender).send(any(MimeMessage.class));

            emailService.sendEmail(to, null, emptyBcc, "Subject", "Content", false, 3);

            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should handle lowest priority email")
        void shouldHandleLowestPriorityEmail() {
            List<String> to = List.of("recipient@test.com");
            doNothing().when(mailSender).send(any(MimeMessage.class));

            emailService.sendEmail(to, null, null, "Low Priority", "Content", false, 5);

            verify(mailSender).send(any(MimeMessage.class));
        }
    }
}
