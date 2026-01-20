package dev.fnvir.kajz.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import lombok.Data;

/** 
 * Configuration properties for Twilio to send SMS 
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sms.twilio")
public class TwilioConfig {
    
    /** Whether to enable twilio or not. */
    private boolean enabled = false;
    
    /** Twilio Account SID */
    private String accountSid;
    
    /** Twilio Auth Token */
    private String authToken;
    
    /** Twilio Phone Number */
    private String phoneNumber;
    
    
    @PostConstruct
    void init() {
        if (!StringUtils.hasText(this.accountSid) 
                || !StringUtils.hasText(this.authToken)
                || !StringUtils.hasText(this.phoneNumber)
        ) {
            // if any of the properties are not set, then disable automatically.
            this.enabled = false;
        }
    }
    
}