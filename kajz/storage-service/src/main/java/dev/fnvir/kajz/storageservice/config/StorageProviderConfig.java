package dev.fnvir.kajz.storageservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import dev.fnvir.kajz.storageservice.enums.StorageProviderType;
import lombok.Data;

@Data
@Component
@ConfigurationProperties("storage")
public class StorageProviderConfig {
    
    /**
     * The type/name of the storage provider (e.g. S3).
     */
    private StorageProviderType provider;

}
