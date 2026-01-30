package dev.fnvir.kajz.storageservice.service;

import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.Callable;

import org.springframework.scheduling.annotation.Async;

import dev.fnvir.kajz.storageservice.dto.UploadValidationResultDTO;
import dev.fnvir.kajz.storageservice.dto.res.InitiateUploadResponse;
import dev.fnvir.kajz.storageservice.enums.StorageProviderType;
import dev.fnvir.kajz.storageservice.model.FileUpload;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractStorageProvider {
    
    protected static final Duration UPLOAD_EXPIRY_TIME = Duration.ofMinutes(2);
    
    public abstract InitiateUploadResponse initiateUpload(FileUpload file);
    
    public abstract StorageProviderType getProviderType();
    
    /**
     * Verify and validate the upload on the storage provider.
     */
    public abstract UploadValidationResultDTO validateUploadCompletion(FileUpload file);
    
    /**
     * Delete the uploaded file from the storage provider.
     * 
     * @param file the file upload entity.
     * @return true if deletion was successful, false otherwise.
     */
    public boolean deleteUploadedFile(FileUpload file) {
        if(file.getCompletedAt() == null)
            return false;
        return deleteFile(file.getStoragePath());
    }

    /**
     * Delete a file from the storage provider (if it exists).
     * 
     * @param key the object-key (S3) / blob-name (Azure Blob) of the file to delete.
     * @return true if it was deleted successfully, else false (if it doesn't exist).
     */
    public abstract boolean deleteFile(String key);
    
    /**
     * Asynchronously delete a file from the storage provider.
     * This is the same as {@link #deleteFile(String)} but executes asynchronously.
     * 
     * @param key the object-key (S3) / blob-name (Azure Blob) to delete.
     */
    @Async
    public void deleteFileAsync(String key) {
        deleteFile(key);
    }
    
    /**
     * Stream a file from the storage provider. This is useful if there isn't direct
     * public access to the file or no CDN has been set up yet.
     * 
     * @param key the object-key (S3) / blob-name (Azure Blob) to delete.
     * @return a Callable that provides an InputStream to read the file.
     */
    public abstract Callable<InputStream> downloadFile(String key);
}
