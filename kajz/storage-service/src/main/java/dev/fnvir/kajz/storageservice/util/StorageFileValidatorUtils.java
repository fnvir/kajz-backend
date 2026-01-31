package dev.fnvir.kajz.storageservice.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import dev.fnvir.kajz.storageservice.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageFileValidatorUtils {
    
    private final StorageProperties storageProps;
    private final transient Set<MimeType> cachedAllowedMimeTypes = new HashSet<>();
    
    private static final Tika TIKA = new Tika();
    
    @PostConstruct
    void compileAllowedMimeTypes() {
        for (var e : storageProps.getAllowedTypes()) {
            cachedAllowedMimeTypes.add(MimeTypeUtils.parseMimeType(e));
        }
    }
    
    /**
     * Whether the content-length of a file is within the allowed size.
     * 
     * @param contentLength the size of the file in bytes.
     * @return true if the size is valid, false otherwise.
     */
    public boolean isValidFileSize(long contentLength) {
        return Long.compare(contentLength, storageProps.getMaxSize()) <= 0;
    }
    
    public boolean isValidMimeType(String mimeType) {
        return isValidMimeType(MimeTypeUtils.parseMimeType(mimeType));
    }
    
    public boolean isValidMimeType(MimeType mimeType) {
        return cachedAllowedMimeTypes.contains(mimeType);
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
    public boolean isValidMimeType(String filename, InputStream inputStream) {
        String mediaType = detectMediaType(filename, inputStream);
        return isValidMimeType(mediaType);
    }
    
    /**
     * Detect actual media type of a file using Apache Tika.
     * 
     * @param filename    the name of the file.
     * @param inputStream an open input-stream of the file.
     * @return the detected content type, or application/octet-stream if detection fails.
     */
    public String detectMediaType(String filename, InputStream inputStream) {
        try {
            String detected = TIKA.detect(inputStream, filename);
            return detected != null ? detected : "application/octet-stream";
        } catch (IOException e) {
            log.warn("Failed to detect content type for: {}! Error: {}", filename, e.getMessage());
            return "application/octet-stream";
        }
    }
    
    

}
