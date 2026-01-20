package dev.fnvir.kajz.notificationservice.config;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;

import dev.fnvir.kajz.notificationservice.config.EmailProperties.EmailServiceProvider;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import lombok.RequiredArgsConstructor;


@Configuration
@RequiredArgsConstructor
public class EmailSenderConfig {
    
    private final EmailProperties emailProperties;
    
    @Bean
    JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        mailSender.setHost(emailProperties.getHost());
        mailSender.setPort(emailProperties.getPort());
        mailSender.setUsername(emailProperties.getUsername());
        
        Properties props = mailSender.getJavaMailProperties();
        props.putAll(emailProperties.getProperties());
        
        if(emailProperties.getProvider() == EmailServiceProvider.MICROSOFT) {
            // since microsoft email needs oauth
            mailSender.setSession(Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    String accessToken = getMicrosoftAccessToken();
                    return new PasswordAuthentication(emailProperties.getUsername(), accessToken);
                }
            }));
        } else {
            // use smtp basic auth
            mailSender.setPassword(emailProperties.getPassword());
            mailSender.setJavaMailProperties(props);
        }
        
        return mailSender;
    }
    
    public String getMicrosoftAccessToken() {
        var msProps = emailProperties.getMicrosoft();
        try {
            ConfidentialClientApplication app = ConfidentialClientApplication
                    .builder(msProps.getClientId(), ClientCredentialFactory.createFromSecret(msProps.getClientSecret()))
                    .authority("https://login.microsoftonline.com/" + msProps.getTenantId() + "/oauth2/v2.0/authorize")
                    .build();
            ClientCredentialParameters parameters = ClientCredentialParameters
                    .builder(Set.of(msProps.getScope()))
                    .build();
            IAuthenticationResult result = app.acquireToken(parameters).get();
            return result.accessToken();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
