package dev.fnvir.kajz.authservice.dto.req;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest (
        
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Must be a valid email")
    String email,
    
    @JsonAlias("code")
    @NotNull(message = "OTP cannot be null.")
    String otp,
    
    @JsonAlias({"new_password", "newPassword"})
    @NotBlank
    @Size(min = 8, message = "Password must be atleast 8 characters")
    String password
        
) {}
