package dev.fnvir.kajz.authservice.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import dev.fnvir.kajz.authservice.dto.UserDTO;
import dev.fnvir.kajz.authservice.dto.req.UserSignupRequest;
import dev.fnvir.kajz.authservice.dto.res.UserResponse;
import dev.fnvir.kajz.authservice.exception.ApiException;
import dev.fnvir.kajz.authservice.util.ResponseUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {
    
    private final Keycloak keycloak;
    
    private static final String BUYER_ROLE = "buyer";
    private static final String SELLER_ROLE = "seller";
    
    private final Map<String, RoleRepresentation> roleCache = new ConcurrentHashMap<>(2);
    
    @Value("${keycloak.user-realm}")
    private String userRealm;

    public UserDTO createUser(@Valid UserSignupRequest signupReq) {
        try {
            return createKeycloakUser(signupReq);
        } catch (RestClientResponseException e) {
            var body = e.getResponseBodyAs(new ParameterizedTypeReference<Map<String, Object>>() {});
            throw new ApiException(e.getStatusCode(), body);
        }
    }
    
    private UserDTO createKeycloakUser(UserSignupRequest signupReq) {
        var realm = keycloak.realm(userRealm);
        
        // create user representation
        UserRepresentation user = new UserRepresentation();
        user.setUsername(signupReq.username());
        user.setEmail(signupReq.email());
        user.setAttributes(Map.of(
                "dateOfBirth", List.of(signupReq.dateOfBirth().toString()),
                "firstName", List.of(signupReq.firstName()),
                "lastName", List.of(signupReq.lastName())
        ));
        user.setEnabled(true);
        user.setEmailVerified(false);
        
        // set password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(signupReq.password());
        
        user.setCredentials(List.of(credential));
        
        // create user
        var response = realm.users().create(user);
        if (response.getStatus() != 201) {
            int status = response.getStatus();
            var body = ResponseUtils.jakartaResponseToMap(response);
            response.close();
            throw new ApiException(status, body);
        }
        
        // fetch created user's ID
        String createdId = response.getHeaderString("Location");
        createdId = createdId.substring(createdId.lastIndexOf('/') + 1); // Location header ends with /{id}
        response.close();
        
        // attach role
        realm.users().get(createdId).roles().realmLevel().add(List.of(getRoleFromCache(BUYER_ROLE)));
        
        return UserDTO.builder()
                .id(createdId)
                .username(signupReq.username())
                .email(signupReq.email())
                .firstName(signupReq.firstName())
                .lastName(signupReq.lastName())
                .roles(Set.of(BUYER_ROLE))
                .build();
    }
    
    @Scheduled(initialDelay = 30, fixedRate = 2 * 60 * 60, timeUnit = TimeUnit.SECONDS)
    void refreshRoleCache() {
        var realm = keycloak.realm(userRealm);
        
        RoleRepresentation customerRole = realm.roles().get(BUYER_ROLE).toRepresentation();
        roleCache.put(BUYER_ROLE, customerRole);
        
        RoleRepresentation sellerRole = realm.roles().get(SELLER_ROLE).toRepresentation();
        roleCache.put(SELLER_ROLE, sellerRole);
    }
    
    private RoleRepresentation getRoleFromCache(String role) {
        if (!(role.equals(BUYER_ROLE) || role.equals(SELLER_ROLE)))
            return null;
        if (!roleCache.containsKey(role)) {
            refreshRoleCache();
        }
        return roleCache.get(role);
    }

    public UserResponse findUser(UUID userId) {
        try {
            var userResource = keycloak.realm(userRealm).users().get(userId.toString());
            var user = userResource.toRepresentation();
            return UserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .emailVerified(user.isEmailVerified() != null && user.isEmailVerified())
                    .build();
        } catch (jakarta.ws.rs.ClientErrorException e) {
            throw new ApiException(e.getResponse().getStatus(), e.getMessage());
        }
    }

}
