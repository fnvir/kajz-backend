package dev.fnvir.kajz.authservice.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class KeycloakConfig {
    
    private final KeycloakProperties props;
    
    @Bean
    Keycloak keycloak() {
        return KeycloakBuilder.builder()
          .serverUrl(props.getServerUrl())
          .realm(props.getAdminRealm())
          .clientId(props.getAdminClientId())
          .grantType(props.getGrantType())
          .username(props.getAdminUsername())
          .password(props.getAdminPassword())
          .build();
    }

}
