package dev.fnvir.kajz.authservice.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OtpVerifcationReq(
        @NotBlank(message = "User ID cannot be blank.")
        @Email
        String email,
        
        @NotBlank(message = "OTP cannot be empty.")
        @Schema(example = "123456")
        String otp
) {}
