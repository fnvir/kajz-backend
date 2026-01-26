package dev.fnvir.kajz.storageservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Properties for AWS S3 buckets.
 */
@Data
@Component
@ConfigurationProperties("storage.aws.s3")
public class AwsS3Properties {
    
    /**
     * AWS access key.
     */
    private String accessKey;
    
    /**
     * AWS secret key.
     */
    private String secretKey;
    
    /**
     * AWS region for buckets.
     */
    private String region;
    
    /**
     * The name of the S3 bucket.
     */
    private String bucketName;
    
    /**
     * Whether to auto create the bucket if it doesn't exist.
     * Default is false.
     */
    private Boolean autoCreateBucket = false;

}
