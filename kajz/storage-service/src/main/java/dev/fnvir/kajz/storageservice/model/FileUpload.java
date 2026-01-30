package dev.fnvir.kajz.storageservice.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;

import dev.fnvir.kajz.storageservice.enums.FileAccessLevel;
import dev.fnvir.kajz.storageservice.enums.UploadStatus;
import io.hypersistence.utils.hibernate.id.Tsid;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "file_uploads")
@Getter @Setter
public class FileUpload extends Auditable {
    
    @Id
    @Tsid
    private Long id;
    
    /** 
     * The ID of the owner of the file.
     */
    private UUID ownerId;
    
    @Column(nullable = false)
    private String filename;
    
    /**
     * The path where the file is stored in the storage provider.
     */
    @Column(nullable = false)
    private String storagePath;
    
    /**
     * The MIME type of the file.
     */
    @Column(nullable = false)
    private String mimeType;
    
    /**
     * The size of the file in bytes.
     */
    @Column(nullable = false)
    private Long contentSize;
    
    /**
     * The ETag of the uploaded file.
     */
    private String eTag;
    
    /**
     * The access level of the file.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileAccessLevel access;
    
    /**
     * The status of the upload.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UploadStatus status;
    
    /**
     * Additional metadata associated with the file upload.
     */
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Size(max = 5)
    private Map<String, String> metadata = new HashMap<>();
    
    /**
     * The timestamp at which this upload was completed.
     */
    private Instant completedAt;
    
    /**
     * Whether the file has been marked as deleted.
     */
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean deleted = false;
    
    /**
     * Check whether the file is available for use. It is considered
     * active/available when:
     * <ul>
     *   <li>The file isn't marked as deleted.</li>
     *   <li>The uploading has been completed (completedAt != null).</li>
     *   <li>The file has been validated.</li>
     * </ul>
     * 
     * @return true if the file is available to serve, else false.
     */
    @Transient
    public boolean isAvailable() {
        return !deleted && completedAt != null && status == UploadStatus.VALIDATED;
    }
    
}
