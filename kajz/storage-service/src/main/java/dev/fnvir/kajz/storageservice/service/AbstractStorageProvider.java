package dev.fnvir.kajz.storageservice.service;

import java.time.Duration;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import dev.fnvir.kajz.storageservice.dto.res.InitiateUploadResponse;
import dev.fnvir.kajz.storageservice.enums.FileAccessLevel;
import dev.fnvir.kajz.storageservice.enums.StorageProviderType;
import dev.fnvir.kajz.storageservice.model.FileUpload;
import dev.fnvir.kajz.storageservice.util.UuidEncodeUtils;

public abstract class AbstractStorageProvider {
    
    protected static final Duration UPLOAD_EXPIRY_TIME = Duration.ofMinutes(2);
    
    protected String generateStorageKey(String filename, FileAccessLevel accessLevel, @Nullable UUID ownerId) {
        String subfolder = ownerId == null ? "common" : UuidEncodeUtils.encodeCrockford(ownerId);
        return String.join("/",
                    accessLevel == FileAccessLevel.PRIVATE ? "private" : "public",
                    subfolder,
                    filename
                ).strip();
    }
    
    public abstract InitiateUploadResponse initiateUpload(FileUpload file);
    
    public abstract StorageProviderType getProviderType();
    
}
