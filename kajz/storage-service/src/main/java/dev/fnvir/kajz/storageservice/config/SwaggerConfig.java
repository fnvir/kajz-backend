package dev.fnvir.kajz.storageservice.config;

import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {
    
    private final OAuth2ResourceServerProperties oauth2Props;
    
    @Bean
    OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .version("1.0.0")
                        .title("Centralized File Storage Microservice")
                        .description("A multi-vendor platform where anyone can share their expertise and"
                                + " discover tailored solutions from independent experts for all sorts of taks.")
                        .contact(new Contact()
                                .name("Farhan Tanvir")
                                .url("https://github.com/fnvir")
                                .email("farhantaanvir@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList("OAuth2"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"))
                .components(new Components()
                        .addSecuritySchemes("OAuth2", createOAuthScheme())
                        .addSecuritySchemes("Bearer Auth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
    
    private SecurityScheme createOAuthScheme() {
        OAuthFlows flows = new OAuthFlows().implicit(createAuthorizationCodeFlow());
        return new SecurityScheme().type(SecurityScheme.Type.OAUTH2)
            .flows(flows);
    }
    
    private OAuthFlow createAuthorizationCodeFlow() {
        String issuerUri = oauth2Props.getJwt().getIssuerUri();
        return new OAuthFlow()
                .authorizationUrl(issuerUri + "/protocol/openid-connect/auth")
                .scopes(new Scopes());
    }

}
