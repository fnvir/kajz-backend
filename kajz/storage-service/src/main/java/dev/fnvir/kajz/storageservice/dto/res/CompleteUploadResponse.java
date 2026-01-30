package dev.fnvir.kajz.storageservice.dto.res;

import java.time.Instant;

import dev.fnvir.kajz.storageservice.enums.FileAccessLevel;
import dev.fnvir.kajz.storageservice.enums.UploadStatus;
import lombok.Builder;

/**
 * Response returned upon completing an upload process.
 * 
 * @param fileId        the ID of the uploaded file
 * @param filename      the name of the uploaded file
 * @param contentType   the MIME type of the uploaded file
 * @param contentLength the size of the uploaded file in bytes
 * @param status        the status of the upload
 * @param accessLevel   the access level of the uploaded file
 * @param startedAt     the timestamp when the upload started
 * @param completedAt   the timestamp when the upload was completed
 */
@Builder
public record CompleteUploadResponse (
        String fileId,
        String filename,
        String contentType,
        Long contentLength,
        UploadStatus status,
        FileAccessLevel accessLevel,
        Instant startedAt,
        Instant completedAt
) {}