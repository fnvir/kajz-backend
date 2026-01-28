package dev.fnvir.kajz.storageservice.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import dev.fnvir.kajz.storageservice.model.FileUpload;

public interface StorageRepository extends JpaRepository<FileUpload, Long> {
    
    @Query("""
            FROM FileUpload f
            WHERE
                (f.status = 'FAILED')
            OR 
                (f.createdAt <= :before AND f.completedAt IS NULL AND f.status = 'UPLOADING')
            ORDER BY f.createdAt
            """)
    List<FileUpload> findInvalidUploads(Instant before, Limit limit);
    
    /**
     * Find failed uploads or uploads pending for a long time.
     * 
     * An upload is considered invalid if it:
     * <ul>
     *   <li>Has status UPLOADING and is still unfinished 
     *       and was created before the given duration.</li>
     *   <li>Has status FAILED.</li>
     * </ul>
     *
     * @param pendingSince how long an upload may remain as UPLOADING before
     *                     being considered invalid
     * @param maxResults   max results to return
     * 
     * @return list of invalid uploads
     */
    default List<FileUpload> findInvalidUploadsPendingSince(Duration pendingSince, int maxResults) {
        Instant createdBefore = Instant.now().minus(pendingSince);
        return findInvalidUploads(createdBefore, Limit.of(maxResults));
    }
    
    @Modifying
    @Query("DELETE FROM FileUpload f WHERE f.deleted = true")
    int deleteAllSoftDeleted();

}
