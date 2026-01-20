package dev.fnvir.kajz.notificationservice.service.email;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.Collection;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import dev.fnvir.kajz.notificationservice.config.EmailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSenderService {
    
    private final JavaMailSender emailSender;
    private final EmailProperties emailProperties;
    
    private static final String DISPLAY_NAME = "Kajz";
    
    /**
     * Synchronously sends an email.
     *
     * @param to       List of recipient email addresses.
     * @param cc       List of CC email addresses (optional).
     * @param bcc      List of BCC email addresses (optional).
     * @param subject  Subject of the email.
     * @param content  Content of the email.
     * @param isHtml   Whether the content is HTML formatted.
     * @param priority The priority of the email (between 1 (highest) and 5 (lowest))
     * @throws MessagingException if there is an error sending the email.
     */
    public void sendEmail(Collection<String> to, Collection<String> cc, Collection<String> bcc, String subject, String content, boolean isHtml, int priority) {
        try {
            sendEmailSync(to, cc, bcc, subject, content, isHtml, priority);
            log.debug("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Email sending failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Asynchronously sends an email with reactive Mono.
     *
     * @param to      List of recipient email addresses.
     * @param cc      List of CC email addresses (optional).
     * @param bcc     List of BCC email addresses (optional).
     * @param subject Subject of the email.
     * @param content Content of the email.
     * @param isHtml  Whether the content is HTML formatted.
     * @param priority The priority of the email (between 1 (highest) and 5 (lowest))
     * @return Mono that completes when the email is sent.
     */
    public Mono<Boolean> sendEmailMono(Collection<String> to, Collection<String> cc, Collection<String> bcc, String subject, String content, boolean isHtml, int priority) {
        return Mono.fromCallable(() -> {
            sendEmailSync(to, cc, bcc, subject, content, isHtml, priority);
            return true;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .retryWhen(Retry.backoff(2, Duration.ofSeconds(30))
                .filter(e -> e instanceof MessagingException))
        .doOnSuccess(_ -> log.debug("Email asynchronously sent successfully to: {}", to))
        .doOnError(MessagingException.class, e -> log.error("Failed to send email", e.getMessage()))
        .doOnError(Throwable.class, e -> log.error("Unexpected error while sending email", e.getMessage()))
        .onErrorReturn(false);
    }

    /**
     * Helper method to send email synchronously.
     *
     * @param to       List of recipient email addresses.
     * @param cc       List of CC email addresses (optional).
     * @param bcc      List of BCC email addresses (optional).
     * @param subject  Subject of the email.
     * @param content  Content of the email.
     * @param priority Priority of the email (between 1 (highest) and 5 (lowest))
     * @param isHtml   Whether the content is HTML formatted.
     * @throws MessagingException if there is an error sending the email.
     */
    private void sendEmailSync(
            Collection<String> to,
            Collection<String> cc,
            Collection<String> bcc,
            String subject,
            String content,
            boolean isHtml,
            int priority
    ) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setTo(to.toArray(new String[0]));
        helper.setSubject(subject);
        helper.setText(content, isHtml);
        helper.setPriority(priority);
        try {
            helper.setFrom(emailProperties.getSenderAddress(), DISPLAY_NAME);
        } catch (UnsupportedEncodingException e) {
            log.warn("Email sender display name has unsupported encoding: {}", DISPLAY_NAME);
            helper.setFrom(emailProperties.getSenderAddress());
        }
        
        if (!CollectionUtils.isEmpty(cc))
            helper.setCc(cc.toArray(new String[0]));
        if (!CollectionUtils.isEmpty(bcc))
            helper.setBcc(bcc.toArray(new String[0]));
        
        emailSender.send(message);
    }

}
