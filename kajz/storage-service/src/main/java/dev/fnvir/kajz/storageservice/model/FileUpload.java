package dev.fnvir.kajz.storageservice.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.Type;

import dev.fnvir.kajz.storageservice.model.enums.FileAccessLevel;
import dev.fnvir.kajz.storageservice.model.enums.UploadStatus;
import io.hypersistence.utils.hibernate.id.Tsid;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    
    private UUID ownerId;
    
    @Column(nullable = false)
    private String filename;
    
    @Column(nullable = false)
    private String mimeType;
    
    @Column(nullable = false)
    private Long contentSize;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileAccessLevel access;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UploadStatus status;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Size(max = 5)
    private Map<String, String> metadata = new HashMap<>();
    
}
