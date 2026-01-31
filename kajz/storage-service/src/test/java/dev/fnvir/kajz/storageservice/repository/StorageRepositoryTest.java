package dev.fnvir.kajz.storageservice.repository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.junit.jupiter.Testcontainers;

import dev.fnvir.kajz.storageservice.TestConfigurations;
import dev.fnvir.kajz.storageservice.TestcontainersConfiguration;
import dev.fnvir.kajz.storageservice.config.AuditConfig;
import dev.fnvir.kajz.storageservice.enums.FileAccessLevel;
import dev.fnvir.kajz.storageservice.enums.UploadStatus;
import dev.fnvir.kajz.storageservice.model.FileUpload;
import jakarta.persistence.Table;

/**
 * Tests for {@link StorageRepository}.
 */
@DataJpaTest(showSql = false)
@Import({TestcontainersConfiguration.class, TestConfigurations.class, AuditConfig.class})
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "ENABLE_TC", matches = "true")
@ActiveProfiles("test")
@DisplayName("Storage Repository Tests")
public class StorageRepositoryTest {

    @Autowired
    private StorageRepository storageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @AfterEach
    void cleanUp() {
        storageRepository.deleteAll();
    }
    
    @Test
    @DisplayName("findInvalidUploadsPendingSince returns failed and long-pending uploads only")
    void findInvalidUploadsPendingSince_returnsExpected() {
        Duration pendingSince = Duration.ofHours(10);
        Instant timestamp = Instant.now().minus(pendingSince);

        var failed = newUpload(
            UUID.randomUUID(),
            "failed.jpg",
            FileAccessLevel.PRIVATE,
            UploadStatus.FAILED
        );
        
        var oldUploading = newUpload(
            UUID.randomUUID(),
            "old_uploading.jpg",
            FileAccessLevel.PRIVATE,
            UploadStatus.UPLOADING
        );
        
        var recentUploading = newUpload(
            UUID.randomUUID(),
            "recent_uploading.jpg",
            FileAccessLevel.PRIVATE,
            UploadStatus.UPLOADING
        );
        
        storageRepository.saveAllAndFlush(List.of(failed, oldUploading, recentUploading));
        
        setCreatedAt(failed, timestamp.minusSeconds(10)); // before the timestamp
        setCreatedAt(oldUploading, timestamp); // exactly at the timestamp
        setCreatedAt(recentUploading, timestamp.plusSeconds(69)); // after the timestamp
        
        List<FileUpload> result = storageRepository.findInvalidUploadsPendingSince(pendingSince, 10);
        
        Assertions.assertThat(result)
            .hasSize(2)
            .containsExactly(failed, oldUploading); // sorted by createdAt
    }

    @Test
    @DisplayName("deleteAllSoftDeleted removes only soft-deleted uploads")
    void deleteAllSoftDeleted_removesOnlySoftDeleted() {
        List<FileUpload> entities = new ArrayList<>();
        int softDeletedCount = 5;
        int activeCount = 3;
        for(int i=0; i<softDeletedCount; i++) {
            var file = newUpload(
                UUID.randomUUID(),
                "file_" + i + ".jpg",
                FileAccessLevel.PRIVATE,
                UploadStatus.UPLOADED
            );
            file.setDeleted(true);
            entities.add(file);
        }

        for(int i=0; i<activeCount; i++) {
            var file = newUpload(
                UUID.randomUUID(),
                "active_file_" + i + ".jpg",
                FileAccessLevel.PRIVATE,
                UploadStatus.UPLOADED
            );
            entities.add(file);
        }
        
        storageRepository.saveAllAndFlush(entities);

        int deletedRows = storageRepository.deleteAllSoftDeleted();
        Assertions.assertThat(deletedRows).isEqualTo(softDeletedCount);
    }

    private FileUpload newUpload(
            UUID ownerId,
            String filename,
            FileAccessLevel access,
            UploadStatus status
    ) {
        FileUpload f = new FileUpload();
        f.setOwnerId(ownerId);
        f.setFilename(filename);
        f.setStoragePath("random/" + filename);
        f.setMimeType("image/jpeg");
        f.setContentSize(123456L);
        f.setAccess(access);
        f.setStatus(status);
        return f;
    }
    
    private FileUpload setCreatedAt(FileUpload file, Instant createdAt) {
        storageRepository.flush();
        
        String tableName = getTableName(FileUpload.class);
        jdbcTemplate.update("UPDATE %s SET created_at = ? WHERE id = ?".formatted(tableName), Timestamp.from(createdAt), file.getId());
        
        ReflectionTestUtils.setField(file, "createdAt", createdAt);
        
        return file;
    }
    
    private String getTableName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table tableAnnotation = entityClass.getAnnotation(Table.class);
            String tableName = tableAnnotation.name();
            if (!tableName.isEmpty()) {
                return tableName;
            }
        }
        // Default behavior if @Table is not present or name is empty
        return entityClass.getSimpleName();
    }
}
