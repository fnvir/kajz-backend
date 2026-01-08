package dev.fnvir.kajz.authservice.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordReq (
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Must be a valid email")
        String email
) {}