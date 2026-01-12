package dev.fnvir.kajz.authservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.fnvir.kajz.authservice.dto.res.UserResponse;
import dev.fnvir.kajz.authservice.exception.ApiException;
import dev.fnvir.kajz.authservice.exception.NotFoundException;
import dev.fnvir.kajz.authservice.service.KeycloakService;

@WebMvcTest(UserController.class)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KeycloakService keycloakService;

    private static final String BASE_PATH = "/users";
    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "testuser";

    @Nested
    @DisplayName("GET /users/{userId}")
    class GetUserByIdEndpointTests {

        @Test
        @DisplayName("Should return user successfully with 200 status")
        void shouldReturnUserSuccessfullyWith200Status() throws Exception {
            UserResponse expectedUser = UserResponse.builder()
                    .id(TEST_USER_ID.toString())
                    .username(TEST_USERNAME)
                    .email(TEST_EMAIL)
                    .firstName("John")
                    .lastName("Doe")
                    .emailVerified(true)
                    .build();

            when(keycloakService.findUser(TEST_USER_ID)).thenReturn(expectedUser);

            mockMvc.perform(get(BASE_PATH + "/" + TEST_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TEST_USER_ID.toString()))
                    .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.emailVerified").value(true));

            verify(keycloakService).findUser(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should return user with emailVerified false")
        void shouldReturnUserWithEmailVerifiedFalse() throws Exception {
            UserResponse expectedUser = UserResponse.builder()
                    .id(TEST_USER_ID.toString())
                    .username(TEST_USERNAME)
                    .email(TEST_EMAIL)
                    .firstName("Jane")
                    .lastName("Smith")
                    .emailVerified(false)
                    .build();

            when(keycloakService.findUser(TEST_USER_ID)).thenReturn(expectedUser);

            mockMvc.perform(get(BASE_PATH + "/" + TEST_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.emailVerified").value(false));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            UUID nonExistentUserId = UUID.randomUUID();

            when(keycloakService.findUser(nonExistentUserId))
                    .thenThrow(new NotFoundException());

            mockMvc.perform(get(BASE_PATH + "/" + nonExistentUserId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when userId is invalid UUID format")
        void shouldReturn400WhenUserIdIsInvalidUuidFormat() throws Exception {
            mockMvc.perform(get(BASE_PATH + "/invalid-uuid"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle ApiException from KeycloakService")
        void shouldHandleApiExceptionFromKeycloakService() throws Exception {
            when(keycloakService.findUser(any(UUID.class)))
                    .thenThrow(new ApiException(500, "Internal server error"));

            mockMvc.perform(get(BASE_PATH + "/" + TEST_USER_ID))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("Should return complete user response structure")
        void shouldReturnCompleteUserResponseStructure() throws Exception {
            UserResponse expectedUser = UserResponse.builder()
                    .id(TEST_USER_ID.toString())
                    .username("complete.user.12345678")
                    .email("complete@example.com")
                    .firstName("Complete")
                    .lastName("User")
                    .emailVerified(true)
                    .build();

            when(keycloakService.findUser(TEST_USER_ID)).thenReturn(expectedUser);

            mockMvc.perform(get(BASE_PATH + "/" + TEST_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.username").exists())
                    .andExpect(jsonPath("$.email").exists())
                    .andExpect(jsonPath("$.firstName").exists())
                    .andExpect(jsonPath("$.lastName").exists())
                    .andExpect(jsonPath("$.emailVerified").exists());
        }

        @Test
        @DisplayName("Should handle null fields in user response gracefully")
        void shouldHandleNullFieldsInUserResponseGracefully() throws Exception {
            UserResponse expectedUser = UserResponse.builder()
                    .id(TEST_USER_ID.toString())
                    .username(TEST_USERNAME)
                    .email(TEST_EMAIL)
                    .firstName(null)
                    .lastName(null)
                    .emailVerified(false)
                    .build();

            when(keycloakService.findUser(TEST_USER_ID)).thenReturn(expectedUser);

            mockMvc.perform(get(BASE_PATH + "/" + TEST_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TEST_USER_ID.toString()))
                    .andExpect(jsonPath("$.firstName").doesNotExist())
                    .andExpect(jsonPath("$.lastName").doesNotExist());
        }

        @Test
        @DisplayName("Should use correct content type in response")
        void shouldUseCorrectContentTypeInResponse() throws Exception {
            UserResponse expectedUser = UserResponse.builder()
                    .id(TEST_USER_ID.toString())
                    .username(TEST_USERNAME)
                    .email(TEST_EMAIL)
                    .emailVerified(true)
                    .build();

            when(keycloakService.findUser(TEST_USER_ID)).thenReturn(expectedUser);

            mockMvc.perform(get(BASE_PATH + "/" + TEST_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isMap());
        }
    }
}
