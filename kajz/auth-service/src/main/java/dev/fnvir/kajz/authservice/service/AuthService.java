package dev.fnvir.kajz.authservice.service;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import dev.fnvir.kajz.authservice.dto.UserDTO;
import dev.fnvir.kajz.authservice.dto.req.ForgotPasswordReq;
import dev.fnvir.kajz.authservice.dto.req.OtpVerifcationReq;
import dev.fnvir.kajz.authservice.dto.req.ResendEmailVerificationReq;
import dev.fnvir.kajz.authservice.dto.req.ResetPasswordRequest;
import dev.fnvir.kajz.authservice.dto.req.UserSignupRequest;
import dev.fnvir.kajz.authservice.exception.ApiException;
import dev.fnvir.kajz.authservice.exception.ConflictException;
import dev.fnvir.kajz.authservice.service.OtpService.OtpType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final KeycloakService keycloakService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;

    public UserDTO register(@Valid UserSignupRequest signupReq) {
        if (Period.between(signupReq.dateOfBirth(), LocalDate.now()).getYears() < 18)
            throw new ConflictException("User must be at least 18 years old.");
        
        var user = keycloakService.createUser(signupReq);
        
        sendVerificationEmail(user.getEmail());
        
        return user;
    }
    
    public void verifyEmail(OtpVerifcationReq req) {
        boolean match = otpService.verifyOtp(OtpType.EMAIL_VERFICATION, req.email(), req.otp());
        if(!match) {
            throw new ConflictException("OTP is invalid or expired.");
        }
        keycloakService.verifyUserEmail(req.email());
    }
    
    public void resendVerificationEmail(@Valid ResendEmailVerificationReq req) {
        var user = keycloakService.findUser(req.userId());
        if(user.isEmailVerified()) {
            throw new ConflictException("User's account is already verified.");
        }
        sendVerificationEmail(user.getEmail());
    }

    private void sendVerificationEmail(String userEmail) {
        String otp = otpService.generateAndSaveOtp(OtpType.EMAIL_VERFICATION, userEmail);
        
        Context context = new Context();
        context.setVariable("otpCode", otp);
        String emailContent = templateEngine.process("account-verification-email", context);
        
        emailService.sendEmailAsync(userEmail, "Verify your account", emailContent, true);
    }

    public void initiatePasswordReset(@Valid ForgotPasswordReq req) {
        // validate email (will send 404 if no user found)
        keycloakService.findByEmail(req.email());
        // generate otp
        String otp = otpService.generateAndSaveOtp(OtpType.PASSWORD_RECOVERY, req.email());
        // generate email content
        Context context = new Context();
        context.setVariable("otp", otp);
        String emailContent = templateEngine.process("password-reset-email", context);
        // send email
        emailService.sendEmailAsync(req.email(), "Reset your password", emailContent, true);
    }
    
    public void resetPassword(@Valid ResetPasswordRequest req) {
        if (!otpService.verifyOtp(OtpType.PASSWORD_RECOVERY, req.email(), req.otp())) {
            throw new ApiException(403, "Code is invalid or expired");
        }
        keycloakService.resetUserPasswordByEmail(req.email(), req.password());
    }

}
