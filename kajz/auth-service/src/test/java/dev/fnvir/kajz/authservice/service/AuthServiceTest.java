package dev.fnvir.kajz.authservice.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import dev.fnvir.kajz.authservice.dto.UserDTO;
import dev.fnvir.kajz.authservice.dto.req.ForgotPasswordReq;
import dev.fnvir.kajz.authservice.dto.req.OtpVerifcationReq;
import dev.fnvir.kajz.authservice.dto.req.ResendEmailVerificationReq;
import dev.fnvir.kajz.authservice.dto.req.ResetPasswordRequest;
import dev.fnvir.kajz.authservice.dto.req.UserSignupRequest;
import dev.fnvir.kajz.authservice.dto.res.UserResponse;
import dev.fnvir.kajz.authservice.exception.ApiException;
import dev.fnvir.kajz.authservice.exception.ConflictException;
import dev.fnvir.kajz.authservice.exception.NotFoundException;
import dev.fnvir.kajz.authservice.service.OtpService.OtpType;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {
    
    @Mock
    private KeycloakService keycloakService;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<Context> contextCaptor;
    
    private static final String TEST_EMAIL = "test@test.test";
    
    @Nested
    @DisplayName("Register user tests")
    class RegisterUserTests {
        
        private UserSignupRequest signupReq;
        private UserDTO userResponse;
        
        private static final String TEST_OTP = "123456";
        
        @BeforeEach
        void setup() {
            
            signupReq = new UserSignupRequest(
                    "Farhan",
                    "Tanvir",
                    "farhan@example.test",
                    "test1234",
                    LocalDate.parse("2000-12-01")
            );
            
            userResponse = UserDTO.builder()
                    .id(UUID.randomUUID().toString())
                    .firstName(signupReq.firstName())
                    .lastName(signupReq.lastName())
                    .email(signupReq.email())
                    .roles(Set.of("test_role"))
                    .build();
        }
        
        @Test
        @DisplayName("Should register user successfully when age is 18 or older")
        void shouldRegisterUserSuccessfullyWhenAgeIs18OrOlder() {
            when(keycloakService.createUser(signupReq)).thenReturn(userResponse);
            when(otpService.generateAndSaveOtp(eq(OtpType.EMAIL_VERFICATION), anyString())).thenReturn(TEST_OTP);
            
            when(templateEngine.process(eq("account-verification-email"), any(Context.class)))
                    .thenReturn("<html>OTP: 123456</html>");
            
            UserDTO result = authService.register(signupReq);
            
            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals(result.getEmail(), signupReq.email());
            assertEquals(result.getFirstName(), signupReq.firstName());
            assertEquals(result.getLastName(), signupReq.lastName());
            assertNotNull(result.getRoles());
            
            verify(keycloakService).createUser(signupReq);
        }
        
        @Test
        @DisplayName("Should throw ConflictException when user is under 18")
        void shouldThrowConflictExceptionWhenUserIsUnder18() {
            UserSignupRequest youngSignupReq = new UserSignupRequest(
                    "Young",
                    "User",
                    "test@test.test",
                    "password",
                    LocalDate.now().minusYears(17) // 17 years old
            );

            assertThrows(ConflictException.class, () -> authService.register(youngSignupReq));
        }

        @Test
        @DisplayName("Should send verification email after registration")
        void shouldSendVerificationEmailAfterRegistration() {
            String emailContent = "<html>Your OTP is 123456</html>";

            when(keycloakService.createUser(signupReq)).thenReturn(userResponse);
            when(otpService.generateAndSaveOtp(eq(OtpType.EMAIL_VERFICATION), anyString())).thenReturn(TEST_OTP);
            when(templateEngine.process(eq("account-verification-email"), any(Context.class)))
                    .thenReturn(emailContent);
            
            authService.register(signupReq);
            
            verify(emailService).sendEmail(
                    eq(signupReq.email()),
                    eq("Verify your account"),
                    anyString(),
                    eq(true)
            );
        }

        @Test
        @DisplayName("Should pass OTP code to email template")
        void shouldPassOtpCodeToEmailTemplate() {
            when(keycloakService.createUser(signupReq)).thenReturn(userResponse);
            when(otpService.generateAndSaveOtp(eq(OtpType.EMAIL_VERFICATION), anyString())).thenReturn(TEST_OTP);
            when(templateEngine.process(eq("account-verification-email"), contextCaptor.capture()))
                    .thenReturn("<html>OTP</html>");

            authService.register(signupReq);

            Context capturedContext = contextCaptor.getValue();
            assertEquals(capturedContext.getVariable("otpCode"), TEST_OTP);
        }
    }
    
    @Nested
    @DisplayName("verifyEmail tests")
    class VerifyEmailTests {
        
        private static final String TEST_OTP = "345678";
        
        @Test
        @DisplayName("Should verify email successfully when OTP is valid")
        void shouldVerifyEmailSuccessfullyWhenOtpIsValid() {
            OtpVerifcationReq req = new OtpVerifcationReq(TEST_EMAIL, TEST_OTP);
            
            when(otpService.verifyOtp(OtpType.EMAIL_VERFICATION, req.email(), req.otp())).thenReturn(true);
            
            authService.verifyEmail(req);
            
            verify(keycloakService).verifyUserEmail(TEST_EMAIL);
        }
        
        @Test
        @DisplayName("Should throw ConflictException when OTP doesn't match")
        void shouldThrowConflictExceptionWhenOtpIsInvalid() {
            OtpVerifcationReq req = new OtpVerifcationReq(TEST_EMAIL, "wrong-otp");

            when(otpService.verifyOtp(OtpType.EMAIL_VERFICATION, req.email(), "wrong-otp")).thenReturn(false);

            assertThrows(ConflictException.class, () -> authService.verifyEmail(req));

            verify(keycloakService, never()).verifyUserEmail(anyString());
        }

    }
    
    @Nested
    @DisplayName("resendVerificationEmail tests")
    class ResendVerificationEmailTests {
        
        private ResendEmailVerificationReq req;
        
        @BeforeEach
        void setup() {
            req = new ResendEmailVerificationReq(UUID.randomUUID());
        }

        @Test
        @DisplayName("Should resend verification email when user is not verified")
        void shouldResendVerificationEmailWhenUserIsNotVerified() {

            final String testOtp = "123456";
            
            UserResponse unverifiedUser = UserResponse.builder()
                    .id(req.userId().toString())
                    .email(TEST_EMAIL)
                    .emailVerified(false)
                    .build();
            
            when(keycloakService.findUser(req.userId())).thenReturn(unverifiedUser);
            when(otpService.generateAndSaveOtp(eq(OtpType.EMAIL_VERFICATION), eq(unverifiedUser.getEmail()))).thenReturn(testOtp);
            when(templateEngine.process(eq("account-verification-email"), any(Context.class)))
                    .thenReturn("<html>OTP</html>");

            authService.resendVerificationEmail(req);

            verify(emailService).sendEmail(eq(TEST_EMAIL), anyString(), anyString(), eq(true));
        }

        @Test
        @DisplayName("Should throw ConflictException when user is already verified")
        void shouldThrowConflictExceptionWhenUserIsAlreadyVerified() {
            UserResponse verifiedUser = UserResponse.builder()
                    .id(req.userId().toString())
                    .email(TEST_EMAIL)
                    .emailVerified(true)
                    .build();

            when(keycloakService.findUser(req.userId())).thenReturn(verifiedUser);

            var exceptionThrown = assertThrows(ConflictException.class, () -> authService.resendVerificationEmail(req));
            assertTrue(exceptionThrown.getMessage().equals("User's account is already verified."));

            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString(), anyBoolean());
        }
    }
    
    @Nested
    @DisplayName("initiatePasswordReset tests")
    class InitiatePasswordResetTests {
        
        private ForgotPasswordReq req;
        private UserResponse matchedUser;
        
        private static final String FORGOT_PASSWORD_OTP = "978654";
        
        @BeforeEach
        void init() {
            req = new ForgotPasswordReq(TEST_EMAIL);
            
            matchedUser = UserResponse.builder()
                    .id(UUID.randomUUID().toString())
                    .email(TEST_EMAIL)
                    .build();
        }

        @Test
        @DisplayName("Should initiate password reset successfully")
        void shouldInitiatePasswordResetSuccessfully() {
            when(keycloakService.findByEmail(TEST_EMAIL)).thenReturn(matchedUser);
            when(otpService.generateAndSaveOtp(eq(OtpType.PASSWORD_RECOVERY), eq(TEST_EMAIL))).thenReturn(FORGOT_PASSWORD_OTP);
            when(templateEngine.process(eq("password-reset-email"), any(Context.class)))
                    .thenReturn("<html>Reset OTP</html>");

            authService.initiatePasswordReset(req);

            verify(keycloakService).findByEmail(TEST_EMAIL);
            verify(otpService).generateAndSaveOtp(OtpType.PASSWORD_RECOVERY, TEST_EMAIL);
            verify(emailService).sendEmail(eq(TEST_EMAIL), anyString(), anyString(), eq(true));
        }

        @Test
        @DisplayName("Should pass OTP to password reset email template")
        void shouldPassOtpToPasswordResetEmailTemplate() {
            when(keycloakService.findByEmail(TEST_EMAIL)).thenReturn(matchedUser);
            when(otpService.generateAndSaveOtp(eq(OtpType.PASSWORD_RECOVERY), eq(TEST_EMAIL))).thenReturn(FORGOT_PASSWORD_OTP);
            when(templateEngine.process(eq("password-reset-email"), contextCaptor.capture()))
                    .thenReturn("<html>Reset</html>");

            authService.initiatePasswordReset(req);

            Context capturedContext = contextCaptor.getValue();
            assertEquals(capturedContext.getVariable("otp"), FORGOT_PASSWORD_OTP);
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found")
        void shouldPropagateNotFoundExceptionWhenUserNotFound() {
            ForgotPasswordReq req = new ForgotPasswordReq("nonexistent@example.com");

            when(keycloakService.findByEmail("nonexistent@example.com")).thenThrow(NotFoundException.class);
            
            assertThrows(NotFoundException.class, ()-> authService.initiatePasswordReset(req));

            verify(otpService, never()).generateAndSaveOtp(any(), anyString());
        }
    }

    @Nested
    @DisplayName("resetPassword tests")
    class ResetPasswordTests {
        
        private static final String RESET_PASSWORD_OTP = "112233";

        @Test
        @DisplayName("Should reset password successfully when OTP is valid")
        void shouldResetPasswordSuccessfullyWhenOtpIsValid() {
            String newPassword = "newSecurePassword123";
            
            var req = new ResetPasswordRequest(TEST_EMAIL, RESET_PASSWORD_OTP, newPassword);

            when(otpService.verifyOtp(OtpType.PASSWORD_RECOVERY, TEST_EMAIL, RESET_PASSWORD_OTP)).thenReturn(true);

            authService.resetPassword(req);

            verify(keycloakService).resetUserPasswordByEmail(TEST_EMAIL, newPassword);
        }

        @Test
        @DisplayName("Should throw ApiException with 403 status code when OTP is invalid")
        void shouldThrowApiExceptionWhenOtpIsInvalid() {
            var req = new ResetPasswordRequest(TEST_EMAIL, "wrong-otp", "newPassword123");

            when(otpService.verifyOtp(OtpType.PASSWORD_RECOVERY, TEST_EMAIL, "wrong-otp")).thenReturn(false);

            assertThatThrownBy(() -> authService.resetPassword(req))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseStatus", HttpStatus.FORBIDDEN)
                    .hasMessageContaining("Code is invalid or expired");

            verify(keycloakService, never()).resetUserPasswordByEmail(anyString(), anyString());
        }

    }
    

}
