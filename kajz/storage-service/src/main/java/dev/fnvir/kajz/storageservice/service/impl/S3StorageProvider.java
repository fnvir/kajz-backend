package dev.fnvir.kajz.storageservice.service.impl;

import java.io.IOException;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dev.fnvir.kajz.storageservice.config.AwsS3Properties;
import dev.fnvir.kajz.storageservice.config.StorageProperties;
import dev.fnvir.kajz.storageservice.dto.UploadValidationResultDTO;
import dev.fnvir.kajz.storageservice.dto.res.InitiateUploadResponse;
import dev.fnvir.kajz.storageservice.enums.StorageProviderType;
import dev.fnvir.kajz.storageservice.model.FileUpload;
import dev.fnvir.kajz.storageservice.service.AbstractStorageProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
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
    
    public S3StorageProvider(StorageProperties storageProps, AwsS3Properties s3Properties) {
        super(storageProps);
        
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
    public StorageProviderType getProviderType() {
        return StorageProviderType.AWS_S3;
    }

    @Override
    public InitiateUploadResponse initiateUpload(FileUpload file) {
        String objectKey = file.getStoragePath();
        
        PresignedPutObjectRequest presignedReq = generatePresignedUploadUrl(
                objectKey,
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

    @Override
    public UploadValidationResultDTO validateUploadCompletion(FileUpload file) {
        String storageKey = file.getStoragePath();

        var headReq = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .build();
        try {
            // verify uploaded file exists
            HeadObjectResponse headRes = s3Client.headObject(headReq);
            
            // validate uploaded file's size
            boolean isValidFileSize = super.validateFileSize(headRes.contentLength());
            if(!isValidFileSize) {
                return UploadValidationResultDTO.invalidContentLength();
            }
        } catch (NoSuchKeyException e) {
            return UploadValidationResultDTO.fileDoesntExist();
        } catch (S3Exception e) {
            log.error("Failed to read object from S3 with: {}", storageKey);
            throw new RuntimeException(e);
        }
        
        // validate content-type
        var getReq = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .range("bytes=0-8191") // first 8KB only
                .build();
        
        try (ResponseInputStream<GetObjectResponse> in = s3Client.getObject(getReq)) {
            boolean isValidMediaType = super.validateContentType(file.getFilename(), in);
            if(!isValidMediaType) {
                return UploadValidationResultDTO.invalidContentType();
            }
        } catch (IOException e) {
            log.error("Skipping content-type validation: IO error in input stream of s3 object. {}", e.getMessage());
        }
        return UploadValidationResultDTO.success();
    }

    @Override
    public boolean deleteFile(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        var deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        try {
            s3Client.deleteObject(deleteRequest);
        } catch (S3Exception e) {
            return false;
        }
        
        return true;
    }
    
}
