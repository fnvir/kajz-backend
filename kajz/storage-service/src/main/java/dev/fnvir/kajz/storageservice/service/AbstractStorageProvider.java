package dev.fnvir.kajz.storageservice.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

import org.apache.tika.Tika;

import dev.fnvir.kajz.storageservice.config.StorageProperties;
import dev.fnvir.kajz.storageservice.dto.UploadValidationResultDTO;
import dev.fnvir.kajz.storageservice.dto.res.InitiateUploadResponse;
import dev.fnvir.kajz.storageservice.enums.StorageProviderType;
import dev.fnvir.kajz.storageservice.model.FileUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractStorageProvider {
    
    private final StorageProperties storageProperties;
    
    protected static final Duration UPLOAD_EXPIRY_TIME = Duration.ofMinutes(2);
    
    private final Tika tika = new Tika();
    
    public abstract InitiateUploadResponse initiateUpload(FileUpload file);
    
    public abstract StorageProviderType getProviderType();
    
    /**
     * Verify and validate the upload on the storage provider.
     */
    public abstract UploadValidationResultDTO validateUploadCompletion(FileUpload file);
    
    /**
     * Whether the content-length of the uploaded file is within the allowed size.
     * @param contentLength the size of the file in bytes.
     * @return true if the size is valid, false otherwise.
     */
    protected boolean validateFileSize(long contentLength) {
        return Long.compare(contentLength, storageProperties.getMaxSize()) <= 0;
    }
    
    /**
     * Whether the content-type of the uploaded file is valid according to the list
     * defined in the storage properties.
     * 
     * <p>
     * Make sure to close the inputStream properly.
     * </p>
     * 
     * @param filename    the name of the file.
     * @param inputStream an open input-stream to read the contents of the file.
     * @return true if the content-type is allowed, false otherwise.
     */
    protected boolean validateContentType(String filename, InputStream inputStream) {
        String mediaType = detectContentType(filename, inputStream);
        return storageProperties.getAllowedTypes().contains(mediaType.toLowerCase());
    }
    
    /**
     * Detect actual content type using Apache Tika.
     * 
     * @param filename    the name of the file.
     * @param inputStream an open input-stream of the file.
     * @return the detected content type, or application/octet-stream if detection fails.
     */
    private String detectContentType(String filename, InputStream inputStream) {
        try {
            String detected = tika.detect(inputStream, filename);
            return detected != null ? detected : "application/octet-stream";
        } catch (IOException e) {
            log.warn("Failed to detect content type for: {}! Error: {}", filename, e.getMessage());
            return "application/octet-stream";
        }
    }
}
