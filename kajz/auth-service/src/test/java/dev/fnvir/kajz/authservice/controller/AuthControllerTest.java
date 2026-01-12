package dev.fnvir.kajz.authservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.fnvir.kajz.authservice.dto.UserDTO;
import dev.fnvir.kajz.authservice.dto.req.ForgotPasswordReq;
import dev.fnvir.kajz.authservice.dto.req.OtpVerifcationReq;
import dev.fnvir.kajz.authservice.dto.req.ResendEmailVerificationReq;
import dev.fnvir.kajz.authservice.dto.req.ResetPasswordRequest;
import dev.fnvir.kajz.authservice.dto.req.UserSignupRequest;
import dev.fnvir.kajz.authservice.exception.ApiException;
import dev.fnvir.kajz.authservice.exception.ConflictException;
import dev.fnvir.kajz.authservice.exception.NotFoundException;
import dev.fnvir.kajz.authservice.service.AuthService;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_PATH = "/auth";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_OTP = "123456";
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @Nested
    @DisplayName("POST /auth/signup")
    class SignupEndpointTests {

        @Test
        @DisplayName("Should register user successfully and return 201")
        void shouldRegisterUserSuccessfullyAndReturn201() throws Exception {
            UserSignupRequest request = new UserSignupRequest(
                    "John",
                    "Doe",
                    TEST_EMAIL,
                    "password123",
                    LocalDate.of(2000, 1, 1)
            );

            UserDTO expectedUser = UserDTO.builder()
                    .id(TEST_USER_ID.toString())
                    .username("john.doe.abc12345")
                    .email(TEST_EMAIL)
                    .firstName("John")
                    .lastName("Doe")
                    .roles(Set.of("buyer"))
                    .build();

            when(authService.register(any(UserSignupRequest.class))).thenReturn(expectedUser);

            mockMvc.perform(post(BASE_PATH + "/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"));

            verify(authService).register(any(UserSignupRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        void shouldReturn400WhenEmailIsInvalid() throws Exception {
            String invalidRequest = """
                    {
                        "firstName": "John",
                        "lastName": "Doe",
                        "email": "invalid-email",
                        "password": "password123",
                        "dateOfBirth": "2000-01-01"
                    }
                    """;

            mockMvc.perform(post(BASE_PATH + "/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when password is too short")
        void shouldReturn400WhenPasswordIsTooShort() throws Exception {
            String invalidRequest = """
                    {
                        "firstName": "John",
                        "lastName": "Doe",
                        "email": "test@example.com",
                        "password": "short",
                        "dateOfBirth": "2000-01-01"
                    }
                    """;

            mockMvc.perform(post(BASE_PATH + "/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when firstName is blank")
        void shouldReturn400WhenFirstNameIsBlank() throws Exception {
            String invalidRequest = """
                    {
                        "firstName": "",
                        "lastName": "Doe",
                        "email": "test@example.com",
                        "password": "password123",
                        "dateOfBirth": "2000-01-01"
                    }
                    """;

            mockMvc.perform(post(BASE_PATH + "/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when dateOfBirth is missing")
        void shouldReturn400WhenDateOfBirthIsMissing() throws Exception {
            String invalidRequest = """
                    {
                        "firstName": "John",
                        "lastName": "Doe",
                        "email": "test@example.com",
                        "password": "password123"
                    }
                    """;

            mockMvc.perform(post(BASE_PATH + "/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 409 when user is under 18")
        void shouldReturn409WhenUserIsUnder18() throws Exception {
            UserSignupRequest request = new UserSignupRequest(
                    "John",
                    "Doe",
                    TEST_EMAIL,
                    "password123",
                    LocalDate.now().minusYears(17)
            );

            when(authService.register(any(UserSignupRequest.class)))
                    .thenThrow(new ConflictException("User must be at least 18 years old."));

            mockMvc.perform(post(BASE_PATH + "/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /auth/resend-verification-email")
    class ResendVerificationEmailEndpointTests {

        @Test
        @DisplayName("Should resend verification email and return 204")
        void shouldResendVerificationEmailAndReturn204() throws Exception {
            ResendEmailVerificationReq request = new ResendEmailVerificationReq(TEST_USER_ID);

            doNothing().when(authService).resendVerificationEmail(any(ResendEmailVerificationReq.class));

            mockMvc.perform(post(BASE_PATH + "/resend-verification-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(authService).resendVerificationEmail(any(ResendEmailVerificationReq.class));
        }

        @Test
        @DisplayName("Should return 400 when userId is null")
        void shouldReturn400WhenUserIdIsNull() throws Exception {
            String invalidRequest = """
                    {
                        "userId": null
                    }
                    """;

            mockMvc.perform(post(BASE_PATH + "/resend-verification-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 409 when user is already verified")
        void shouldReturn409WhenUserIsAlreadyVerified() throws Exception {
            ResendEmailVerificationReq request = new ResendEmailVerificationReq(TEST_USER_ID);

            doThrow(new ConflictException("User's account is already verified."))
                    .when(authService).resendVerificationEmail(any(ResendEmailVerificationReq.class));

            mockMvc.perform(post(BASE_PATH + "/resend-verification-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /auth/verify-email")
    class VerifyEmailEndpointTests {

        @Test
        @DisplayName("Should verify email and return 204")
        void shouldVerifyEmailAndReturn204() throws Exception {
            OtpVerifcationReq request = new OtpVerifcationReq(TEST_EMAIL, TEST_OTP);

            doNothing().when(authService).verifyEmail(any(OtpVerifcationReq.class));

            mockMvc.perform(post(BASE_PATH + "/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(authService).verifyEmail(any(OtpVerifcationReq.class));
        }

        @Test
        @DisplayName("Should return 400 when email is blank")
        void shouldReturn400WhenEmailIsBlank() throws Exception {
            String invalidRequest = """
                    {
                        "email": "",
                        "otp": "123456"
                    }
                    """;

            mockMvc.perform(post(BASE_PATH + "/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when OTP is blank")
        void shouldReturn400WhenOtpIsBlank() throws Exception {
            String invalidRequest = """
                    {
                        "email": "test@example.com",
                        "otp": ""
                    }
                    """;

            mockMvc.perform(post(BASE_PATH + "/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 409 when OTP is invalid")
        void shouldReturn409WhenOtpIsInvalid() throws Exception {
            OtpVerifcationReq request = new OtpVerifcationReq(TEST_EMAIL, "wrong-otp");

            doThrow(new ConflictException("OTP is invalid or expired."))
                    .when(authService).verifyEmail(any(OtpVerifcationReq.class));

            mockMvc.perform(post(BASE_PATH + "/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /auth/initiate-password-reset")
    class InitiatePasswordResetEndpointTests {

        @Test
        @DisplayName("Should initiate password reset and return 200")
        void shouldInitiatePasswordResetAndReturn200() throws Exception {
            ForgotPasswordReq request = new ForgotPasswordReq(TEST_EMAIL);

            doNothing().when(authService).initiatePasswordReset(any(ForgotPasswordReq.class));

            mockMvc.perform(post(BASE_PATH + "/initiate-password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("\"Password reset email sent\""));

            verify(authService).initiatePasswordReset(any(ForgotPasswordReq.class));
        }

        @Test
        @DisplayName("Should return 400 when email is blank")
        void shouldReturn400WhenEmailIsBlank() throws Exception {
            String invalidRequest = """
                    {
                        "email": ""
                    }
                    """;

            mockMvc.perform(post(BASE_PATH + "/initiate-password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        void shouldReturn400WhenEmailIsInvalid() throws Exception {
            String invalidRequest = """
                    {
                        "email": "not-an-email"
                    }
                    """;

            mockMvc.perform(post(BASE_PATH + "/initiate-password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            ForgotPasswordReq request = new ForgotPasswordReq("nonexistent@example.com");

            doThrow(new NotFoundException())
                    .when(authService).initiatePasswordReset(any(ForgotPasswordReq.class));

            mockMvc.perform(post(BASE_PATH + "/initiate-password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /auth/reset-password")
    class ResetPasswordEndpointTests {

        @Test
        @DisplayName("Should reset password and return 204")
        void shouldResetPasswordAndReturn204() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest(TEST_EMAIL, TEST_OTP, "newPassword123");

            doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

            mockMvc.perform(post(BASE_PATH + "/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(authService).resetPassword(any(ResetPasswordRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when email is blank")
        void shouldReturn400WhenEmailIsBlank() throws Exception {
            String invalidRequest = """
                    {
                        "email": "",
                        "otp": "123456",
                        "password": "newPassword123"
                    }
                    """;

            mockMvc.perform(post(BASE_PATH + "/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when password is too short")
        void shouldReturn400WhenPasswordIsTooShort() throws Exception {
            String invalidRequest = """
                    {
                        "email": "test@example.com",
                        "otp": "123456",
                        "password": "short"
                    }
                    """;

            mockMvc.perform(post(BASE_PATH + "/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when OTP is invalid")
        void shouldReturn403WhenOtpIsInvalid() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest(TEST_EMAIL, "wrong-otp", "newPassword123");

            doThrow(new ApiException(403, "Code is invalid or expired"))
                    .when(authService).resetPassword(any(ResetPasswordRequest.class));

            mockMvc.perform(post(BASE_PATH + "/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest("nonexistent@example.com", TEST_OTP, "newPassword123");

            doThrow(new NotFoundException())
                    .when(authService).resetPassword(any(ResetPasswordRequest.class));

            mockMvc.perform(post(BASE_PATH + "/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }
}
