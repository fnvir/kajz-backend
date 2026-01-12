package dev.fnvir.kajz.authservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.fnvir.kajz.authservice.dto.UserDTO;
import dev.fnvir.kajz.authservice.dto.req.ForgotPasswordReq;
import dev.fnvir.kajz.authservice.dto.req.OtpVerifcationReq;
import dev.fnvir.kajz.authservice.dto.req.ResendEmailVerificationReq;
import dev.fnvir.kajz.authservice.dto.req.ResetPasswordRequest;
import dev.fnvir.kajz.authservice.dto.req.UserSignupRequest;
import dev.fnvir.kajz.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(path="/auth", version = "1")
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/signup")
    @Operation(summary = "Register a new user")
    public ResponseEntity<UserDTO> registerUser(@RequestBody @Valid UserSignupRequest signupReq) {
        return ResponseEntity.status(201).body(authService.register(signupReq));
    }
    
    @PostMapping("/resend-verification-email")
    @Operation(summary = "Manually send an email to verify the user's account, if not already verified.")
    public ResponseEntity<Void> resendVerificationEmail(@RequestBody @Valid ResendEmailVerificationReq req) {
        authService.resendVerificationEmail(req);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/verify-email")
    @Operation(summary = "Verify a user's account by verifying the OTP.")
    public ResponseEntity<Void> verifyEmail(@RequestBody @Valid OtpVerifcationReq req) {
        authService.verifyEmail(req);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/initiate-password-reset")
    @Operation(summary = "Initiate password reset process by sending a reset email")
    @ApiResponse(responseCode = "200", description = "Password reset email sent successfully.")
    @ApiResponse(responseCode = "404", description = "If no user is found with the provided email.")
    public ResponseEntity<String> forgotPassword(@RequestBody @Valid ForgotPasswordReq req) {
        authService.initiatePasswordReset(req);
        return ResponseEntity.ok("\"Password reset email sent\"");
    }
    
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using the reset OTP")
    @ApiResponse(responseCode = "204", description = "Password updated successfully.")
    @ApiResponse(responseCode = "403", description = "OTP invalid or expired.")
    @ApiResponse(responseCode = "404", description = "If no user is found with the provided email.")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.noContent().build();
    }

}
