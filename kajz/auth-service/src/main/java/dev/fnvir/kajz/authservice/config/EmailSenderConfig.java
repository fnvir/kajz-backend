package dev.fnvir.kajz.authservice.config;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import lombok.RequiredArgsConstructor;

/**
 * **This is temporary.**
 * This uses MS outlook email with oauth2. 
 * Will later create separate microservice for sending emails with lots of other options.
 */
@Configuration
@RequiredArgsConstructor
public class EmailSenderConfig {
    
    private final EmailProperties emailProperties;

    @Bean
    JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        mailSender.setHost("smtp.office365.com");
        mailSender.setPort(587);
        mailSender.setUsername(emailProperties.getUsername());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.office365.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.username", emailProperties.getUsername());
        props.put("mail.smtp.auth.mechanisms", "XOAUTH2");
        props.put("mail.smtp.auth.login.disable", "true");
        props.put("mail.smtp.auth.plain.disable", "true");
//        props.put("mail.debug", "true");

        mailSender.setSession(Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                String accessToken = getAccessToken();
                return new PasswordAuthentication(emailProperties.getUsername(), accessToken);
            }
        }));
        return mailSender;
    }

    public String getAccessToken() {
        try {
            ConfidentialClientApplication app = ConfidentialClientApplication
                    .builder(emailProperties.getClientId(), ClientCredentialFactory.createFromSecret(emailProperties.getClientSecret()))
                    .authority("https://login.microsoftonline.com/" + emailProperties.getTenantId() + "/oauth2/v2.0/authorize").build();

            ClientCredentialParameters parameters = ClientCredentialParameters.builder(Set.of(emailProperties.getScope())).build();
            CompletableFuture<IAuthenticationResult> future = app.acquireToken(parameters);
            IAuthenticationResult result = future.get();
            return result.accessToken();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}