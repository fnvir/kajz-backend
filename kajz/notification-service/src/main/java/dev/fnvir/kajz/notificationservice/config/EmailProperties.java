package dev.fnvir.kajz.notificationservice.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "email")
public class EmailProperties {
    
    /**
     * The type of the email provider. Some settings will be pre-configured based on
     * this. If SMTP_BASIC is selected then no pre-configuration will be done.
     * <br><br>
     * 
     * <i>Default:</i> <strong>SMTP_BASIC</strong>.
     */
    @NotNull
    private EmailServiceProvider provider = EmailServiceProvider.SMTP_BASIC;
    
    /**
     * SMTP server host. For instance, 'smtp.example.com'.
     * Will be auto configured if GMAIL / MICROSOFT is selected as the provider and this is null.
     */
    private String host;
    
    /**
     * SMTP server port.
     * Will be auto configured if GMAIL / MICROSOFT is selected as the provider and this is null.
     */
    private Integer port;
    
    /**
     * Login user of the SMTP server.
     */
    @NotNull
    private String username;
    
    /**
     * Login password of the SMTP server. Will be ignored if the provider doesn't
     * support basic password auth (e.g. Microsoft).
     */
    private String password;
    
    /**
     * The sender address to send the email from. E.g., "company@example.com"
     */
    private String senderAddress;
    
    /**
     * Additional SMTP properties.
     */
    private Map<String, String> properties;

    /**
     * Properties for Microsoft 365/Outlook SMTP OAuth login to mail server.
     * 
     * <br><br>
     * See: <a href=
     * "https://learn.microsoft.com/en-us/exchange/client-developer/legacy-protocols/how-to-authenticate-an-imap-pop-smtp-application-by-using-oauth">Microsoft
     * SMTP connection using OAuth</a>
     */
    private MicrosoftEmailProperties microsoft;
    
    
    @PostConstruct
    void autoConfigure() {
        if (this.provider == EmailServiceProvider.SMTP_BASIC && (this.host == null || this.provider == null)) {
            throw new IllegalStateException(
                    "SMTP host and port must be set, or use a preconfigured provider.");
        }
        
        HashMap<String, String> props = new HashMap<>();
        
        if(this.provider == EmailServiceProvider.MICROSOFT) {
            this.host = "smtp.office365.com";
            this.port = 587;
            
            props.put("mail.smtp.auth.mechanisms", "XOAUTH2");
            props.put("mail.smtp.auth.login.disable", "true");
            props.put("mail.smtp.auth.plain.disable", "true");
        }
        
        if(this.provider == EmailServiceProvider.GMAIL) {
            this.host = "smtp.gmail.com";
            this.port = 587;
        }
        
        props.put("mail.smtp.host", this.host);
        props.put("mail.smtp.port", this.port+"");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        
        this.properties.putAll(props);
    }
    
    /**
     * The provider of the email service.
     */
    public static enum EmailServiceProvider {
        /** 
         * Any provider that supports SMTP Basic Auth (i.e. login to mail server with username and password).
         */
        SMTP_BASIC,
        
        /**
         * Microsoft 365 or Outlook (since Basic auth is deprecated by microsoft).
         */
        MICROSOFT,
        
        /**
         * Gmail provider (password should app-password).
         * See: <a href="https://support.google.com/mail/answer/185833?hl=en">app passwords</a>
         */
        GMAIL
    }
    
    @Data
    public static class MicrosoftEmailProperties {
        /** Client ID for login. */
        private String clientId;
        
        /** Client Secret. */
        private String clientSecret;
        
        /** Microsoft tenant ID. */
        private String tenantId;
        
        private final String scope = "https://outlook.office365.com/.default";
    }

}
