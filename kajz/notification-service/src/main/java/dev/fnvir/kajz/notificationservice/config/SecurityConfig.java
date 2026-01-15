package dev.fnvir.kajz.notificationservice.config;

import java.util.Collection;
import java.util.HashSet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {
    
    @Bean
    SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        http
            .cors(ServerHttpSecurity.CorsSpec::disable)
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .authorizeExchange(req -> req
                    .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .pathMatchers(
                            "/notification-service/swagger-ui/**", "/notification-service/swagger-ui.html", 
                            "/notification-service/v3/api-docs/**", "/notification-service/v3/api-docs.yaml")
                    .permitAll()
                    .pathMatchers("/actuator/**", "/error")
                    .permitAll()
                    .anyExchange()
                    .authenticated()
            )
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .requestCache(requestCacheSpec -> requestCacheSpec.requestCache(NoOpServerRequestCache.getInstance()))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                    .jwtAuthenticationConverter(keycloakJwtAuthConverter())))
            ;
        
        return http.build();
    }
    
    ReactiveJwtAuthenticationConverterAdapter keycloakJwtAuthConverter() {
        var jwtAuthenticationConverter = new JwtAuthenticationConverter();
        final JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new HashSet<>();
            // default scope authorities (SCOPE_xxx)
            Collection<GrantedAuthority> scopeAuthorities = scopesConverter.convert(jwt);
            if (scopeAuthorities != null) {
                authorities.addAll(scopeAuthorities);
            }
            // realm_access.roles -> ROLE_<role>
            if (jwt.getClaimAsMap("realm_access").get("roles") instanceof Collection<?> realmRoles) {
                for (var r : realmRoles)
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase()));
            }
            return authorities;
        });
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }

}
