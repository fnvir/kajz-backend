package dev.fnvir.kajz.authservice.service;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import dev.fnvir.kajz.authservice.config.EmailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender emailSender;
    private final EmailProperties emailProperties;
    
    private static final String DISPLAY_NAME = "Kajz";
    
    
    /**
     * Asynchronously send an email to a single address with a priority of 2.
     *
     * @param to      List of recipient email addresses.
     * @param subject Subject of the email.
     * @param content Content of the email.
     * @param isHtml  Whether the content is HTML formatted.
     */
    @Async
    public void sendEmailAsync(String to, String subject, String content, boolean isHtml) {
        try {
            sendEmailSync(List.of(to), null, null, subject, content, isHtml, 2);
            log.debug("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Email sending failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Asynchronously send an email.
     *
     * @param to      List of recipient email addresses.
     * @param cc      List of CC email addresses (optional).
     * @param bcc     List of BCC email addresses (optional).
     * @param subject Subject of the email.
     * @param content Content of the email.
     * @param isHtml  Whether the content is HTML formatted.
     * @param priority The priority of the email (between 1 (highest) and 5 (lowest))
     */
    @Async
    public void sendEmailAsync(Collection<String> to, Collection<String> cc, Collection<String> bcc, String subject, String content, boolean isHtml, int priority) {
        try {
            sendEmailSync(to, cc, bcc, subject, content, isHtml, priority);
            log.debug("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Email sending failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
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
            helper.setFrom(emailProperties.getUsername(), DISPLAY_NAME);
        } catch (UnsupportedEncodingException e) {
            log.warn("Email sender display name has unsupported encoding: {}", DISPLAY_NAME);
            helper.setFrom(emailProperties.getUsername());
        }
        
        if (!CollectionUtils.isEmpty(cc))
            helper.setCc(cc.toArray(new String[0]));
        if (!CollectionUtils.isEmpty(bcc))
            helper.setBcc(bcc.toArray(new String[0]));
        
        emailSender.send(message);
    }
    
}
