package dev.fnvir.kajz.authservice.service;

import java.util.Set;

import org.springframework.stereotype.Service;

import dev.fnvir.kajz.authservice.dto.event.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final KafkaEventProducer eventProducer;
    
    /**
     * Publishes a email-notification event in kafka to send emails.
     * 
     *
     * @param to      List of recipient email addresses.
     * @param subject Subject of the email.
     * @param content Content of the email.
     * @param isHtml  Whether the content is HTML formatted.
     */
    public void sendEmail(String to, String subject, String content, boolean isHtml) {
        EmailEvent email = new EmailEvent();
        email.setTo(Set.of(to));
        email.setSubject(subject);
        email.setContent(content);
        email.setHtml(isHtml);
        email.setPriority(2);
        
        eventProducer.publishEmailNotification(email);
    }

}
