package dev.fnvir.kajz.storageservice.dto.req;

import dev.fnvir.kajz.storageservice.annotation.ValidFileUpload;
import dev.fnvir.kajz.storageservice.enums.FileAccessLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for initiating a file upload.
 * 
 * @param filename    the name of the file to be uploaded
 * @param mimeType    the MIME type of the file
 * @param fileSize    the size of the file in bytes
 * @param accessLevel the access level of the file (e.g., PUBLIC, PRIVATE)
 * @param purpose     the purpose of the file (e.g., profile-picture,
 *                    gig-thumbnail)
 */
@ValidFileUpload
public record InitiateUploadRequest (
        @Size(max = 500)
        String filename,
        
        @NotNull
        String mimeType,
        
        @NotNull
        Long fileSize,
        
        @NotNull
        FileAccessLevel accessLevel,
        
        @Size(max = 80)
        String purpose // e.g. profile-picture, gig-thumbnail, etc.
) {}
