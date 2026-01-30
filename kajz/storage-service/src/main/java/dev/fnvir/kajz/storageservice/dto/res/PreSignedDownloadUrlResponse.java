package dev.fnvir.kajz.storageservice.dto.res;

import java.time.Instant;

import lombok.Builder;

/**
 * Response payload for pre-sgined temp download URL for private files.
 * 
 * @param url       the temp download URL
 * @param expiresAt the timestamp at which this URL expires
 */
@Builder
public record PreSignedDownloadUrlResponse (
        String url,
        Instant expiresAt
) {}
