package dev.fnvir.kajz.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "email")
public class EmailProperties {

    private String clientId;
    private String clientSecret;
    private String tenantId;
    private String scope;
    private String username;
    
}
