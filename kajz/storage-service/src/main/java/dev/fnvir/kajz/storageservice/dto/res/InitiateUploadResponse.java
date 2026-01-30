package dev.fnvir.kajz.storageservice.dto.res;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import lombok.Builder;

/**
 * Response containing presigned upload URL and metadata.
 */
@Builder
public record InitiateUploadResponse (
        @JsonFormat(shape = Shape.STRING)
        Long fileId,
        String uploadUrl,
        Instant expiresAt,
        Map<String, Object> uploadHeaders
) {}
