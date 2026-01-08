package dev.fnvir.kajz.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "email")
public class EmailProperties {

    private final String clientId;
    private final String clientSecret;
    private final String tenantId;
    private final String scope;
    private final String username;
    
}
