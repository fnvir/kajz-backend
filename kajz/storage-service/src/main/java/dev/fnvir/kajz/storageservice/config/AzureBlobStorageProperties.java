package dev.fnvir.kajz.storageservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Properties for Azure Blob Storage.
 */
@Data
@Component
@ConfigurationProperties("storage.azure.blob")
public class AzureBlobStorageProperties {
    
    /**
     * The connection string to connect to the service.
     */
    private String connectionString;

    /**
     * Name of the container.
     */
    private String containerName;
    
    /**
     * Whether to auto create the container if it doesn't exist.
     * Default is false.
     */
    private Boolean autoCreateContainer = false;
    
}
