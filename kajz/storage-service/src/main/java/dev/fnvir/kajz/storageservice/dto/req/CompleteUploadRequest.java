package dev.fnvir.kajz.storageservice.dto.req;

import jakarta.validation.constraints.NotNull;

public record CompleteUploadRequest(
        @NotNull
        Long fileId
){}
