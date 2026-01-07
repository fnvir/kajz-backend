package dev.fnvir.kajz.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {
    private String serverUrl;
    private String adminRealm;
    private String adminClientId;
    private String adminUsername;
    private String adminPassword;
    private final String grantType = "password";
    private String userRealm;
    private String userClientId;
}
