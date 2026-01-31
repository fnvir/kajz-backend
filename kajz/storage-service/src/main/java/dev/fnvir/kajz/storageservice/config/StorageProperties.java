package dev.fnvir.kajz.storageservice.config;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties("storage.file")
public class StorageProperties {

    /**
     * The list of allowed mime-types.
     */
    private Set<String> allowedTypes = Set.of(
            "image/png",
            "image/apng",
            "image/jpeg",
            "image/webp",
            "application/pdf"
    );

    /**
     * The maximum allowed size (in bytes) of a single file.
     * Default is 5MB.
     */
    private Long maxSize = 5L * 1024 * 1024;

}
