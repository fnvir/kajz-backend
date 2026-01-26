package dev.fnvir.kajz.storageservice.dto.res;

import java.time.Instant;
import java.util.Map;

import lombok.Builder;

/**
 * Response containing presigned upload URL and metadata.
 */
@Builder
public record InitiateUploadResponse (
        Long fileId,
        String uploadUrl,
        Instant expiresAt,
        Map<String, Object> uploadHeaders
) {}
