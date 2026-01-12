package dev.fnvir.kajz.authservice.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import dev.fnvir.kajz.authservice.dto.UserDTO;
import dev.fnvir.kajz.authservice.dto.req.UserSignupRequest;
import dev.fnvir.kajz.authservice.dto.res.UserResponse;
import dev.fnvir.kajz.authservice.exception.ApiException;
import dev.fnvir.kajz.authservice.exception.NotFoundException;
import dev.fnvir.kajz.authservice.util.ResponseUtils;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeycloakService Unit Tests")
public class KeycloakServiceTest {
    
    private static final String USER_REALM = "test-realm";
    private static final String TEST_USER_ID = UUID.randomUUID().toString();
    private static final String TEST_EMAIL = "test@example.com";
    
    @Mock
    private Keycloak keycloak;
    
    @InjectMocks
    private KeycloakService keycloakService;
    
    // common stubs
    private RealmResource realmResource;
    private UsersResource usersResource;
    private UserResource userResource;
    private RolesResource rolesResource;
    private RoleResource roleResource;
    private RoleMappingResource roleMappingResource;
    private RoleScopeResource roleScopeResource;
    
    @Captor
    private ArgumentCaptor<UserRepresentation> userCaptor;
    
    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(keycloakService, "userRealm", USER_REALM);
        
        var roleCache = new ConcurrentHashMap<String, RoleRepresentation>(
            Map.of(
                KeycloakService.BUYER_ROLE, new RoleRepresentation(KeycloakService.BUYER_ROLE, "", false),
                KeycloakService.SELLER_ROLE, new RoleRepresentation(KeycloakService.SELLER_ROLE, "", false)
            )
        );
        ReflectionTestUtils.setField(keycloakService, "roleCache", roleCache);
        
        realmResource = mock(RealmResource.class);
        usersResource = mock(UsersResource.class);
        userResource = mock(UserResource.class);
        rolesResource = mock(RolesResource.class);
        roleResource = mock(RoleResource.class);
        roleMappingResource = mock(RoleMappingResource.class);
        roleScopeResource = mock(RoleScopeResource.class);
    }
    
    @Nested
    @DisplayName("createUser tests")
    class CreateUserTests {
        
        private UserSignupRequest validSignupReq;
        
        @BeforeEach
        void init() {
            validSignupReq = new UserSignupRequest(
                    "John",
                    "Doe",
                    TEST_EMAIL,
                    "password123",
                    LocalDate.now().minusYears(20)
            );
        }

        @Test
        @DisplayName("Should create user successfully when all ok")
        void shouldCreateUserSuccessfully() {
            Response mockResponse = mock(Response.class);
            
            when(mockResponse.getStatus()).thenReturn(201);
            when(mockResponse.getHeaderString("Location")).thenReturn("http://keycloak/users/" + TEST_USER_ID);

            RoleRepresentation buyerRole = new RoleRepresentation();
            buyerRole.setName(KeycloakService.BUYER_ROLE);

            when(keycloak.realm(USER_REALM)).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.create(any(UserRepresentation.class))).thenReturn(mockResponse);
            when(usersResource.get(TEST_USER_ID)).thenReturn(userResource);
            when(userResource.roles()).thenReturn(roleMappingResource);
            when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
            lenient().when(realmResource.roles()).thenReturn(rolesResource);

            UserDTO result = keycloakService.createUser(validSignupReq);

            assertNotNull(result);
            assertEquals(result.getEmail(), validSignupReq.email());
            assertEquals(result.getFirstName(), validSignupReq.firstName());
            assertEquals(result.getLastName(), validSignupReq.lastName());
            assertNotNull(result.getRoles());
        }

        @Test
        @DisplayName("Should pass user properties correctly to Keycloak client")
        void shouldSetUserPropertiesCorrectly() {
            Response mockResponse = mock(Response.class);
            when(mockResponse.getStatus()).thenReturn(201);
            when(mockResponse.getHeaderString("Location")).thenReturn("http://keycloak/users/" + TEST_USER_ID);


            when(keycloak.realm(USER_REALM)).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            
            when(usersResource.create(userCaptor.capture())).thenReturn(mockResponse);
            
            when(usersResource.get(TEST_USER_ID)).thenReturn(userResource);
            when(userResource.roles()).thenReturn(roleMappingResource);
            when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
            
            lenient().when(realmResource.roles()).thenReturn(rolesResource);
            lenient().when(rolesResource.get(KeycloakService.BUYER_ROLE)).thenReturn(roleResource);
            
            keycloakService.createUser(validSignupReq);

            UserRepresentation capturedUser = userCaptor.getValue();
            assertEquals(capturedUser.getEmail(), validSignupReq.email());
            assertTrue(capturedUser.isEnabled());
            assertFalse(capturedUser.isEmailVerified());
        }

        @Test
        @DisplayName("Should throw ApiException when user creation fails in keycloak")
        void shouldThrowApiExceptionWhenUserCreationFails() {
            try (MockedStatic<ResponseUtils> mocked = Mockito.mockStatic(ResponseUtils.class)) {
                Response mockResponse = mock(Response.class);
                when(mockResponse.getStatus()).thenReturn(400);
                
                mocked.when(() -> ResponseUtils.jakartaResponseToMap(mockResponse))
                    .thenReturn(Map.of());
                
                when(keycloak.realm(USER_REALM)).thenReturn(realmResource);
                when(realmResource.users()).thenReturn(usersResource);
                when(usersResource.create(any(UserRepresentation.class))).thenReturn(mockResponse);
                
                assertThatThrownBy(() -> keycloakService.createUser(validSignupReq))
                .isInstanceOf(ApiException.class);
            }
        }
    }
    
    
    @Nested
    @DisplayName("verifyUserEmail tests")
    class VerifyUserEmailTests {

        @Test
        @DisplayName("Should verify user email successfully")
        void shouldVerifyUserEmailSuccessfully() {
            UserRepresentation user = new UserRepresentation();
            user.setId(TEST_USER_ID);
            user.setEmail(TEST_EMAIL);
            user.setEmailVerified(false);

            when(keycloak.realm(USER_REALM)).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.searchByEmail(TEST_EMAIL, true)).thenReturn(List.of(user));
            when(usersResource.get(TEST_USER_ID)).thenReturn(userResource);

            keycloakService.verifyUserEmail(TEST_EMAIL);

            verify(userResource).update(userCaptor.capture());
            assertTrue(userCaptor.getValue().isEmailVerified());
        }

        @Test
        @DisplayName("Should not update if email already verified")
        void shouldNotUpdateIfEmailAlreadyVerified() {
            UserRepresentation user = new UserRepresentation();
            user.setId(TEST_USER_ID);
            user.setEmail(TEST_EMAIL);
            user.setEmailVerified(true);

            when(keycloak.realm(USER_REALM)).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.searchByEmail(TEST_EMAIL, true)).thenReturn(List.of(user));
            
            keycloakService.verifyUserEmail(TEST_EMAIL);
            
            verify(userResource, Mockito.never()).update(any());
        }
        
        @Test
        @DisplayName("Should throw NotFoundException if user not found")
        void shouldThrowNotFoundIfNoUserWithThatEmail() {
            when(keycloak.realm(USER_REALM)).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.searchByEmail(TEST_EMAIL, true)).thenReturn(List.of()); // return empty list
            
            assertThrows(NotFoundException.class, () -> keycloakService.verifyUserEmail(TEST_EMAIL));
            
        }
    }
    
    
    @Nested
    @DisplayName("findUser tests")
    class FindUserTests {
        
        private UserRepresentation user;
        
        @BeforeEach
        void setup() {
            user = new UserRepresentation();
            user.setId(TEST_USER_ID);
            user.setEmail(TEST_EMAIL);
            user.setUsername("test.username");
            user.setFirstName("Test");
            user.setLastName("User");
            user.setEmailVerified(true);
        }

        @Test
        @DisplayName("Should find user by ID successfully")
        void shouldFindUserByIdSuccessfully() {
            UUID userId = UUID.fromString(TEST_USER_ID);

            when(keycloak.realm(USER_REALM)).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.get(TEST_USER_ID)).thenReturn(userResource);
            when(userResource.toRepresentation()).thenReturn(user);

            UserResponse result = keycloakService.findUser(userId);

            assertNotNull(result);
            assertEquals(result.getId(), TEST_USER_ID);
            assertEquals(result.getEmail(), TEST_EMAIL);
            assertEquals(result.getUsername(), user.getUsername());
            assertTrue(result.isEmailVerified());
        }

        @Test
        @DisplayName("Should throw ApiException when user not found by ID")
        void shouldThrowApiExceptionWhenUserNotFoundById() {
            final UUID userId = UUID.randomUUID();

            when(keycloak.realm(USER_REALM)).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.get(userId.toString())).thenReturn(userResource);
            when(userResource.toRepresentation()).thenThrow(new jakarta.ws.rs.NotFoundException());

            assertThrows(ApiException.class, () -> keycloakService.findUser(userId));
        }
    }

    @Nested
    @DisplayName("findByEmail tests")
    class FindByEmailTests {
        
        private UserRepresentation user;

        @BeforeEach
        void setup() {
            user = new UserRepresentation();
            user.setId(TEST_USER_ID);
            user.setEmail(TEST_EMAIL);
            user.setUsername("test.username");
            user.setFirstName("Test");
            user.setLastName("User");
            user.setEmailVerified(true);
        }

        @Test
        @DisplayName("Should find user by email successfully")
        void shouldFindUserByEmailSuccessfully() {

            when(keycloak.realm(USER_REALM)).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.searchByEmail(TEST_EMAIL, true)).thenReturn(List.of(user));

            UserResponse result = keycloakService.findByEmail(TEST_EMAIL);

            assertNotNull(result);
            assertEquals(result.getEmail(), TEST_EMAIL);
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found by email")
        void shouldThrowNotFoundExceptionWhenUserNotFoundByEmail() {
            when(keycloak.realm(USER_REALM)).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.searchByEmail(TEST_EMAIL, true)).thenReturn(List.of());

            assertThrows(NotFoundException.class, () -> keycloakService.findByEmail(TEST_EMAIL));
        }
    }

    @Nested
    @DisplayName("resetUserPassword tests")
    class ResetUserPasswordTests {
        
        @Captor
        private ArgumentCaptor<CredentialRepresentation> credentialCaptor;
        
        private UserRepresentation user;

        @BeforeEach
        void setup() {
            user = new UserRepresentation();
            user.setId(TEST_USER_ID);
            user.setEmail(TEST_EMAIL);
            user.setUsername("test.username");
            user.setFirstName("Test");
            user.setLastName("User");
            user.setEmailVerified(true);
        }

        @Test
        @DisplayName("Should reset user password by email successfully")
        void shouldResetUserPasswordByEmailSuccessfully() {
            String newPassword = "newSecurePassword123";

            when(keycloak.realm(USER_REALM)).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.searchByEmail(TEST_EMAIL, true)).thenReturn(List.of(user));
            when(usersResource.get(TEST_USER_ID)).thenReturn(userResource);

            keycloakService.resetUserPasswordByEmail(TEST_EMAIL, newPassword);

            verify(userResource).resetPassword(credentialCaptor.capture());
            CredentialRepresentation capturedCredential = credentialCaptor.getValue();
            
            assertEquals(capturedCredential.getValue(), newPassword);
            assertEquals(capturedCredential.getType(), CredentialRepresentation.PASSWORD);
            assertFalse(capturedCredential.isTemporary()); // user shouldn't need to change password again
        }

        @Test
        @DisplayName("Should reset user password by ID successfully")
        void shouldResetUserPasswordByIdSuccessfully() {
            String newPassword = "anotherPassword456";

            when(keycloak.realm(USER_REALM)).thenReturn(realmResource);
            when(realmResource.users()).thenReturn(usersResource);
            when(usersResource.get(TEST_USER_ID)).thenReturn(userResource);

            keycloakService.resetUserPassword(TEST_USER_ID, newPassword);

            verify(userResource).resetPassword(credentialCaptor.capture());
            assertEquals(credentialCaptor.getValue().getValue(), newPassword);
        }
    }

}
