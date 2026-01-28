package dev.fnvir.kajz.storageservice.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.sas.SasProtocol;

import dev.fnvir.kajz.storageservice.config.AzureBlobStorageProperties;
import dev.fnvir.kajz.storageservice.config.StorageProperties;
import dev.fnvir.kajz.storageservice.dto.UploadValidationResultDTO;
import dev.fnvir.kajz.storageservice.dto.res.InitiateUploadResponse;
import dev.fnvir.kajz.storageservice.enums.StorageProviderType;
import dev.fnvir.kajz.storageservice.model.FileUpload;
import dev.fnvir.kajz.storageservice.service.AbstractStorageProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "azure-blob")
public class AzureBlobStorageProvider extends AbstractStorageProvider {
    
    private final AzureBlobStorageProperties blobProperties;
    private final BlobContainerClient blobContainerClient;
    
    private static final boolean FORCE_HTTPS_ON_SAS = true; // make this configurable later
    
    public AzureBlobStorageProvider(
            StorageProperties storageProps,
            AzureBlobStorageProperties blobProperties
    ) {
        super(storageProps);
        
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(blobProperties.getConnectionString())
                .buildClient();
        
        this.blobContainerClient = blobServiceClient.getBlobContainerClient(blobProperties.getContainerName());
        this.blobProperties = blobProperties;
    }
    
    @PostConstruct
    void init() {
        if(!blobContainerClient.exists()) {
            if (Boolean.TRUE.equals(blobProperties.getAutoCreateContainer())) {
                log.info("Creating blob container: {}", blobContainerClient.getBlobContainerName());
                blobContainerClient.createIfNotExists();
            } else {
                throw new RuntimeException("Blob container doesn't exist."
                        + " Please create it first or enable auto-creation.");
            }
        }
    }
    
    @Override
    public StorageProviderType getProviderType() {
        return StorageProviderType.AZURE_BLOB;
    }
    
    @Override
    public InitiateUploadResponse initiateUpload(FileUpload file) {
        String blobName = file.getStoragePath();
        
        Instant sasExpiresAt = Instant.now().plus(UPLOAD_EXPIRY_TIME);
        
        String uploadSasUrl = generateUploadSasUrl(blobName, file.getMimeType(), sasExpiresAt);
        
        return InitiateUploadResponse.builder()
                .fileId(file.getId())
                .uploadUrl(uploadSasUrl)
                .uploadHeaders(Map.of(
                    "x-ms-blob-type", "BlockBlob",
                    "Content-Type", file.getMimeType(),
                    "Content-Length", file.getContentSize()
                ))
                .expiresAt(sasExpiresAt)
                .build();
    }
    
    /**
     * Generate a short-lived SAS URL for client to upload a single blob.
     * blobName should be pre-determined (e.g. product/uuid.jpg).
     */
    private String generateUploadSasUrl(String blobName, String contentType, Instant expiresAt) {
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

        // Permissions: write + create (no read/delete)
        BlobSasPermission perms = new BlobSasPermission()
                .setWritePermission(true)
                .setCreatePermission(true);

        OffsetDateTime expiry = expiresAt.atOffset(ZoneOffset.UTC);

        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiry, perms)
                .setContentType(contentType)
                .setProtocol(FORCE_HTTPS_ON_SAS ? SasProtocol.HTTPS_ONLY : SasProtocol.HTTPS_HTTP)
                .setStartTime(OffsetDateTime.now().minusSeconds(5));

        String sasToken = blobClient.generateSas(values);

        return blobClient.getBlobUrl() + "?" + sasToken;
    }

    @Override
    public UploadValidationResultDTO validateUploadCompletion(FileUpload file) {
        String blobName = file.getStoragePath();
        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        
        // verify file exists
        if (!blobClient.exists())
            return UploadValidationResultDTO.fileDoesntExist();
        
        BlobProperties properties = blobClient.getProperties();
        
        // validate uploaded file's content-length
        long filesize = properties.getBlobSize();
        boolean isValidBlobSize = super.validateFileSize(filesize);
        if (!isValidBlobSize) {
            return UploadValidationResultDTO.invalidContentLength();
        }
        
        // validate uploaded file's actual content-type
        // TODO: validate actual content-type in background worker asynchronously
        try (InputStream in = blobClient.openInputStream(new BlobRange(0, 8192L), null)) { // read first 8KB only
            boolean isValidMediaType = super.validateContentType(file.getFilename(), in);
            if(!isValidMediaType) {
                return UploadValidationResultDTO.invalidContentType();
            }
        } catch (IOException e) {
            log.error("Skipping content-type validation: IO error in input stream of blob. {}", e.getMessage());
//            return UploadValidationResultDTO.failed();
        }
        return UploadValidationResultDTO.success();
        
    }

}
