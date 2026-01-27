package dev.fnvir.kajz.storageservice.service.impl;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import dev.fnvir.kajz.storageservice.config.AwsS3Properties;
import dev.fnvir.kajz.storageservice.dto.res.InitiateUploadResponse;
import dev.fnvir.kajz.storageservice.model.FileUpload;
import dev.fnvir.kajz.storageservice.service.AbstractStorageProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "aws-s3")
public class S3StorageProvider extends AbstractStorageProvider {
    
    private final AwsS3Properties s3Properties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    
    public S3StorageProvider(AwsS3Properties s3Properties) {
        var credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3Properties.getAccessKey(), s3Properties.getSecretKey())
        );
        
        this.s3Client = S3Client.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(credentialsProvider)
                .build();
        
        this.s3Presigner = S3Presigner.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(credentialsProvider)
                .build();
        
        this.bucketName = s3Properties.getBucketName();
        
        this.s3Properties = s3Properties;
    }
    
    @PostConstruct
    void init() {
        boolean bucketExists = checkBucketExists(bucketName);
        if (!bucketExists) {
            if (Boolean.TRUE.equals(s3Properties.getAutoCreateBucket())) {
                createBucket(bucketName);
            } else {
                throw new IllegalStateException("S3 bucket doesn't exist."
                        + " Please create it first or enable auto-creation.");
            }
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (this.s3Client != null) {
            s3Client.close();
        }
        if (this.s3Presigner != null) {
            s3Presigner.close();
        }
    }
    
    /**
     * Check if a S3 bucket exists.
     */
    private boolean checkBucketExists(String bucket) {
        try {
            HeadBucketRequest headRequest = HeadBucketRequest.builder()
                .bucket(bucket)
                .build();
            s3Client.headBucket(headRequest);
            log.info("S3 bucket already exists: {}", bucket);
            return true;
        } catch (NoSuchBucketException e) {
            log.info("S3 bucket doesn't exist: {}", bucket);
            return false;
        }
    }
    
    private void createBucket(String bucket) {
        try {
            CreateBucketRequest createRequest = CreateBucketRequest.builder()
                .bucket(bucket)
                .build();
            s3Client.createBucket(createRequest);
            log.info("S3 bucket created: {}", bucket);
        } catch (S3Exception e) {
            log.error("Failed to create S3 bucket: {}", bucket, e);
            throw new RuntimeException("Failed to create S3 bucket", e);
        }
    }

    @Override
    public InitiateUploadResponse initiateUpload(FileUpload file) {
        String objectName = generateStorageKey(file.getFilename(), file.getAccess(), file.getOwnerId());
        
        PresignedPutObjectRequest presignedReq = generatePresignedUploadUrl(
                objectName,
                file.getMimeType(),
                file.getContentSize()
        );
        
        return InitiateUploadResponse.builder()
                .fileId(file.getId())
                .uploadUrl(presignedReq.url().toString())
                .uploadHeaders(Map.of(
                        "Content-Type", file.getMimeType(),
                        "Content-Length", file.getContentSize()
                ))
                .expiresAt(presignedReq.expiration())
                .build();
    }
    
    private PresignedPutObjectRequest generatePresignedUploadUrl(String storageKey, String contentType, Long fileSize) {
        var putReq = PutObjectRequest.builder()
                .key(storageKey)
                .contentType(contentType)
                .contentLength(fileSize)
                .bucket(bucketName)
                .build();
        
        var presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(UPLOAD_EXPIRY_TIME)
                .putObjectRequest(putReq)
                .build();
        
        return s3Presigner.presignPutObject(presignReq);
    }

}
