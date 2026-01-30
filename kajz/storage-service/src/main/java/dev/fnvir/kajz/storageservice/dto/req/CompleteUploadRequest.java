package dev.fnvir.kajz.storageservice.dto.req;

import jakarta.validation.constraints.NotNull;

/**
 * Request to complete an upload process.
 * 
 * @param fileId the ID of the file to complete the upload for
 */
public record CompleteUploadRequest(
        @NotNull
        Long fileId
){}
