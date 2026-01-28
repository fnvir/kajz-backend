package dev.fnvir.kajz.storageservice.dto.res;

import java.time.Instant;

import dev.fnvir.kajz.storageservice.enums.FileAccessLevel;
import dev.fnvir.kajz.storageservice.enums.UploadStatus;
import lombok.Builder;

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