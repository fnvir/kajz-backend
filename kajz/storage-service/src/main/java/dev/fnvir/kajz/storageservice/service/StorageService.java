package dev.fnvir.kajz.storageservice.service;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dev.fnvir.kajz.storageservice.dto.req.CompleteUploadRequest;
import dev.fnvir.kajz.storageservice.dto.req.InitiateUploadRequest;
import dev.fnvir.kajz.storageservice.dto.res.CompleteUploadResponse;
import dev.fnvir.kajz.storageservice.dto.res.InitiateUploadResponse;
import dev.fnvir.kajz.storageservice.enums.FileAccessLevel;
import dev.fnvir.kajz.storageservice.enums.UploadStatus;
import dev.fnvir.kajz.storageservice.exception.ConflictException;
import dev.fnvir.kajz.storageservice.exception.ForbiddenException;
import dev.fnvir.kajz.storageservice.exception.NotFoundException;
import dev.fnvir.kajz.storageservice.mapper.FileUploadMapper;
import dev.fnvir.kajz.storageservice.model.FileUpload;
import dev.fnvir.kajz.storageservice.repository.StorageRepository;
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
    
    @Transactional
    public InitiateUploadResponse initiateUploadProcess(UUID uploaderId, @Valid InitiateUploadRequest req) {
        String fileExt = StringUtils.getFilenameExtension(req.filename());
        String filename = String.join("-",
                req.purpose().replaceAll("[^a-zA-Z0-9_-]", "_").replaceAll("_+", "_").toLowerCase(),
                TSID.fast().toLowerCase()
        );
        String filenameWithExt = filename + (fileExt == null ? "" : ("." + fileExt));
        
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
        
        var validationResult = storageProvider.validateUploadCompletion(file);
        if (!validationResult.isSuccess()) {
            switch (validationResult.getFailureReason()) {
                case FILE_DOESNT_EXIST -> throw new NotFoundException(validationResult.getMessage());
                default -> throw new ConflictException(validationResult.getMessage());
            }
        }
        
        file.setStatus(UploadStatus.UPLOADED);
        file.setCompletedAt(Instant.now());
        
        file = storageRepository.save(file);
        
        return fileUploadMapper.fileUploadToResponse(file);
    }
    
    private FileUpload findByIdAndVerifyOwnershipOrThrow(Long fileId, UUID userId) {
        var file = storageRepository.findById(fileId).orElseThrow(NotFoundException::new);
        if (!userId.equals(file.getOwnerId())) {
            throw new ForbiddenException("User doesn't have ownership of this file");
        }
        return file;
    }

}
