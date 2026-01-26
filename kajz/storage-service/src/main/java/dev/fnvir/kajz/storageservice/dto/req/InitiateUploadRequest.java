package dev.fnvir.kajz.storageservice.dto.req;

import dev.fnvir.kajz.storageservice.annotation.ValidFileUpload;
import dev.fnvir.kajz.storageservice.enums.FileAccessLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@ValidFileUpload
public record InitiateUploadRequest (
        @NotNull
        String mimeType,
        
        @NotNull
        Long fileSize,
        
        @NotNull
        FileAccessLevel accessLevel,
        
        @Size(max = 80)
        String purpose // e.g. profile-picture, gig-thumbnail, etc.
) {}
