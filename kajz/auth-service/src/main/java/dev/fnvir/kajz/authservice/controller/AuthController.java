package dev.fnvir.kajz.authservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.fnvir.kajz.authservice.dto.UserDTO;
import dev.fnvir.kajz.authservice.dto.req.OtpVerifcationReq;
import dev.fnvir.kajz.authservice.dto.req.ResendEmailVerificationReq;
import dev.fnvir.kajz.authservice.dto.req.UserSignupRequest;
import dev.fnvir.kajz.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
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

}
