package dev.fnvir.kajz.storageservice.dto.res;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import lombok.Builder;

/**
 * Response containing presigned upload URL and metadata.
 * 
 * @param fileId        the ID of the file to be uploaded
 * @param uploadUrl     the pre-signed URL for uploading the file
 * @param expiresAt     the expiration timestamp of the upload URL
 * @param uploadHeaders additional headers to be sent when using the upload URL
 */
@Builder
public record InitiateUploadResponse (
        @JsonFormat(shape = Shape.STRING)
        Long fileId,
        String uploadUrl,
        Instant expiresAt,
        Map<String, Object> uploadHeaders
) {
    public InitiateUploadResponse {
        uploadHeaders = uploadHeaders != null ? uploadHeaders : Map.of();
    }
}
