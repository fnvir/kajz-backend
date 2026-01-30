package dev.fnvir.kajz.storageservice.dto.res;

import java.util.UUID;

import dev.fnvir.kajz.storageservice.enums.FileAccessLevel;
import dev.fnvir.kajz.storageservice.enums.UploadStatus;
import lombok.Builder;

/**
 * Response DTO for file upload details.
 * 
 * @param fileId      the ID of the file upload
 * @param ownerId     the ID of the file owner
 * @param filename    the name of the file
 * @param mimeType    the MIME type of the file
 * @param contentSize the size of the file in bytes
 * @param access      the access level of the file
 * @param status      the upload status of the file
 * @param available   whether the file is ready for access
 */
@Builder
public record FileUploadResponse (
        String fileId,
        UUID ownerId,
        String filename,
        String mimeType,
        Long contentSize,
        FileAccessLevel access,
        UploadStatus status,
        Boolean available
) {}
