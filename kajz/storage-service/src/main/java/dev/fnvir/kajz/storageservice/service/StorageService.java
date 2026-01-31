package dev.fnvir.kajz.storageservice.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ETag;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import dev.fnvir.kajz.storageservice.dto.StreamFileDto;
import dev.fnvir.kajz.storageservice.dto.req.CompleteUploadRequest;
import dev.fnvir.kajz.storageservice.dto.req.InitiateUploadRequest;
import dev.fnvir.kajz.storageservice.dto.res.CompleteUploadResponse;
import dev.fnvir.kajz.storageservice.dto.res.FileUploadResponse;
import dev.fnvir.kajz.storageservice.dto.res.InitiateUploadResponse;
import dev.fnvir.kajz.storageservice.dto.res.PreSignedDownloadUrlResponse;
import dev.fnvir.kajz.storageservice.enums.FileAccessLevel;
import dev.fnvir.kajz.storageservice.enums.UploadStatus;
import dev.fnvir.kajz.storageservice.exception.ApiException;
import dev.fnvir.kajz.storageservice.exception.ConflictException;
import dev.fnvir.kajz.storageservice.exception.ForbiddenException;
import dev.fnvir.kajz.storageservice.exception.NotFoundException;
import dev.fnvir.kajz.storageservice.mapper.FileUploadMapper;
import dev.fnvir.kajz.storageservice.model.FileUpload;
import dev.fnvir.kajz.storageservice.repository.StorageRepository;
import dev.fnvir.kajz.storageservice.util.SecurityContextUtils;
import dev.fnvir.kajz.storageservice.util.UuidEncodeUtils;
import io.hypersistence.tsid.TSID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {
    
    private final StorageRepository storageRepository;
    private final AbstractStorageProvider storageProvider;
    private final FileUploadMapper fileUploadMapper;
    
    private TransactionTemplate readOnlyTransaction;
    
    @Autowired
    protected void setTransaction(PlatformTransactionManager transactionManager) {
        TransactionTemplate tpl = new TransactionTemplate(transactionManager);
        tpl.setReadOnly(true);
        this.readOnlyTransaction = tpl;
    }
    
    
    @Transactional
    public InitiateUploadResponse initiateUploadProcess(UUID uploaderId, @Valid InitiateUploadRequest req) {
        String filenameWithExt = generateFilenameWithExt(req.filename(), req.purpose());
        String storagePath = generateStoragePath(filenameWithExt, req.accessLevel(), uploaderId);
        
        FileUpload file = new FileUpload();
        file.setOwnerId(uploaderId);
        file.setFilename(filenameWithExt);
        file.setStoragePath(storagePath);
        file.setAccess(req.accessLevel());
        file.setMimeType(req.mimeType());
        file.setContentSize(req.fileSize());
        file.setStatus(UploadStatus.UPLOADING);
        file = storageRepository.saveAndFlush(file);
        
        return storageProvider.initiateUpload(file);
    }
    
    private String generateFilenameWithExt(String originalFilename, String purpose) {
        String uploadIntent = purpose != null ? purpose : "upload";
        String fileExt = Optional.ofNullable(StringUtils.getFilenameExtension(originalFilename))
                .orElse("bin"); // default to ".bin" format
        String newName = String.join("-",
                uploadIntent.replaceAll("[^a-zA-Z0-9_-]", "_").replaceAll("_+", "_").toLowerCase(),
                TSID.fast().toLowerCase()
        );
        return newName + "." + fileExt;
    }
    
    private String generateStoragePath(String filename, FileAccessLevel accessLevel, @Nullable UUID ownerId) {
        String subfolder = ownerId == null ? "common" : UuidEncodeUtils.encodeCrockford(ownerId);
        return String.join("/",
                    accessLevel == FileAccessLevel.PRIVATE ? "private" : "public",
                    subfolder,
                    filename
                ).strip();
    }

    @Transactional
    public CompleteUploadResponse verifyAndCompleteUpload(UUID userId, @Valid CompleteUploadRequest req) {
        var file = findByIdAndVerifyOwnershipOrThrow(req.fileId(), userId);
        
        if(file.getStatus() != UploadStatus.UPLOADING || file.getCompletedAt() != null) {
            throw new ConflictException("Already completed post-upload validation!");
        }
        
        var validationResult = storageProvider.validateUploadCompletion(file);
        if (!validationResult.isSuccess()) {
            switch (validationResult.getFailureReason()) {
                case FILE_DOESNT_EXIST -> throw new NotFoundException(validationResult.getMessage());
                default -> {
                    storageProvider.deleteFileAsync(file.getStoragePath());
                    throw new ConflictException(validationResult.getMessage());
                }
            }
        }
        
        file.setStatus(UploadStatus.VALIDATED);
        file.setCompletedAt(Instant.now());
        file.setETag(validationResult.getETag());
        file = storageRepository.save(file);
        
        return fileUploadMapper.toUploadCompleteResponse(file);
    }
    
    private FileUpload findByIdAndVerifyOwnershipOrThrow(Long fileId, UUID userId) {
        var file = storageRepository.findById(fileId).orElseThrow(NotFoundException::new);
        if (!userId.equals(file.getOwnerId())) {
            throw new ForbiddenException("User doesn't have ownership of this file");
        }
        return file;
    }
    
    public StreamFileDto downloadFileValidatingAccess(Long fileId, String eTag) {
        var fileRecord = readOnlyTransaction.execute(_ -> findByIdAndValidateAccess(fileId, "ADMIN", "SYSTEM"));
        
        if (!fileRecord.isAvailable()) {
            throw new NotFoundException("File isn't active.");
        }
        
        if (fileRecord.getETag() != null && eTag != null
                && ETag.quoteETagIfNecessary(fileRecord.getETag()).equals(ETag.quoteETagIfNecessary(eTag))
        ) {
            return StreamFileDto.builder().etag(eTag.toString()).build();
        }
        
        String storagePath = fileRecord.getStoragePath();
        
        return StreamFileDto.builder()
                .filename(fileRecord.getFilename())
                .contentLength(fileRecord.getContentSize())
                .contentType(fileRecord.getMimeType())
                .etag(fileRecord.getETag())
                .inputStreamProvider(storageProvider.downloadFile(storagePath))
                .build();
        
    }
    
    private FileUpload findByIdAndValidateAccess(Long fileId, String... allowedRoles) {
        FileUpload file = storageRepository.findById(fileId).orElseThrow(NotFoundException::new);
        
        if (file.getAccess() != FileAccessLevel.PUBLIC) {
            if (!SecurityContextUtils.isAuthenticated())
                throw new ApiException(HttpStatus.UNAUTHORIZED);
            
            if (file.getAccess() == FileAccessLevel.PRIVATE) {
                if (!SecurityContextUtils.matchesUserIdOrHasAnyRole(file.getOwnerId(), allowedRoles))
                    throw new ForbiddenException("Not authorized to access this file");
            }
        }
        
        return file;
    }

    public PreSignedDownloadUrlResponse generateTempDownloadUrl(Long fileId, UUID userId) {
        var file = findByIdAndVerifyOwnershipOrThrow(fileId, userId);
        if (!file.isAvailable())
            throw new NotFoundException("File not validated or has been deleted");
        return storageProvider.generatePreSignedDownloadUrl(file.getStoragePath(), Duration.ofMinutes(3));
    }

    @Transactional
    public void deleteFile(Long fileId, UUID userId) {
        var f = findByIdAndValidateAccess(fileId, "ADMIN", "SYSTEM"); // only owner and admins can delete files
        storageRepository.delete(f);
        
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    storageProvider.deleteFileAsync(f.getStoragePath());
                }
            }
        );
    }

    public FileUploadResponse getFileInfo(Long fileId) {
        var f = readOnlyTransaction.execute(_ -> findByIdAndValidateAccess(fileId, "ADMIN", "SYSTEM"));
        return fileUploadMapper.toResponseDto(f);
    }

}
