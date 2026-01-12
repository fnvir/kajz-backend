package dev.fnvir.kajz.authservice.dto.req;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ResendEmailVerificationReq (
    @NotNull(message = "User ID cannot be blank")
    UUID userId
) {}
