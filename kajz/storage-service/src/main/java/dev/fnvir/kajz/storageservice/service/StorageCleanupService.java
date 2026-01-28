package dev.fnvir.kajz.storageservice.service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.fnvir.kajz.storageservice.model.FileUpload;
import dev.fnvir.kajz.storageservice.repository.StorageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageCleanupService {
    
    private final StorageRepository storageRepository;
    private final AbstractStorageProvider storageProvider;
    
    @Scheduled(initialDelay = 2, fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    @Transactional
    protected void cleanupInvalidUploads() {
        List<FileUpload> uploads = storageRepository.findInvalidUploadsPendingSince(Duration.ofMinutes(5), 99);
        for (var f : uploads) {
            if (f.getStoragePath() != null && !f.isDeleted())
                storageProvider.deleteFileAsync(f.getStoragePath());
            f.setDeleted(true); // soft delete
        }
        storageRepository.saveAll(uploads);
    }
    
    @Transactional
    @Scheduled(initialDelay = 15, fixedRate = 60, timeUnit = TimeUnit.MINUTES)
    protected void deleteSoftDeletedEntries() {
        storageRepository.deleteAllSoftDeleted();
    }

}
