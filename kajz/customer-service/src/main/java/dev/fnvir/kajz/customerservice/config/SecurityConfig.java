package dev.fnvir.kajz.customerservice.config;

import java.util.Collection;
import java.util.HashSet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(req -> req
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(
                            "/customer-service/swagger-ui/**", "/customer-service/swagger-ui.html", 
                            "/customer-service/v3/api-docs/**", "/customer-service/v3/api-docs.yaml")
                    .permitAll()
                    .requestMatchers("/actuator/**", "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            )
            .requestCache(requestCacheSpec -> requestCacheSpec.requestCache(new NullRequestCache()))
            .sessionManagement(management -> management
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                    .jwtAuthenticationConverter(keycloakJwtAuthConverter())))
            ;
        
        return http.build();
    }
    
    JwtAuthenticationConverter keycloakJwtAuthConverter() {
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
        return jwtAuthenticationConverter;
    }

}
