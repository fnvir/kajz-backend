package dev.fnvir.kajz.storageservice.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dev.fnvir.kajz.storageservice.dto.req.InitiateUploadRequest;
import dev.fnvir.kajz.storageservice.dto.res.InitiateUploadResponse;
import dev.fnvir.kajz.storageservice.enums.UploadStatus;
import dev.fnvir.kajz.storageservice.model.FileUpload;
import dev.fnvir.kajz.storageservice.repository.StorageRepository;
import io.hypersistence.tsid.TSID;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {
    
    private final StorageRepository storageRepository;
    private final StorageProvider storageProvider;
    
    @Transactional
    public InitiateUploadResponse initiateUploadProcess(UUID uploaderId, @Valid InitiateUploadRequest req) {
        String fileExt = StringUtils.getFilenameExtension(req.filename());
        String filename = String.join("-",
                req.purpose().strip().replaceAll("\\s+", "-").replaceAll("-+", "-").toLowerCase(),
                TSID.fast().toLowerCase()
        );
        String filenameWithExt = filename + (fileExt == null ? "" : ("." + fileExt));
        
        FileUpload file = new FileUpload();
        file.setOwnerId(uploaderId);
        file.setFilename(filenameWithExt);
        file.setAccess(req.accessLevel());
        file.setMimeType(req.mimeType());
        file.setContentSize(req.fileSize());
        file.setStatus(UploadStatus.UPLOADING);
        file = storageRepository.saveAndFlush(file);
        
        
        return storageProvider.initiateUpload(file);
    }

}
