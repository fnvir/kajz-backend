package dev.fnvir.kajz.notificationservice.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import dev.fnvir.kajz.notificationservice.model.enums.RecipientRole;
import dev.fnvir.kajz.notificationservice.util.UuidV7Generator;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Notification entity representing a user notification.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class Notification {
    
    /** The id of the notification. */
    @Id
    @UuidGenerator(algorithm = UuidV7Generator.class)
    private UUID id;

    /**
     * The id of the user to whom the notification is sent.
     */
    @Column(nullable = false)
    private UUID userId;
    
    /**
     * The role of the recipient user.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RecipientRole recipientRole;
    
    /** 
     * Notification title.
     */
    @Column(nullable = false)
    private String title;

    /** 
     * Notification body/content.
     */
    private String body;

    /**
     * Notification type/category.
     */
    private String type; // e.g., ORDER, PROMOTION, SYSTEM

    /**
     * Action to be taken when the notification is clicked.
     */
    private String clickAction; // URL or deep link
    
    /**
     * Additional metadata associated with the notification.
     */
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @Size(max = 10)
    private Map<String, String> metadata = new LinkedHashMap<>();
    
    /**
     * Indicates whether the notification has been read.
     */
    private boolean read;
    
    /**
     * Indicates whether the notification has been archived.
     */
    private boolean archived;
    
    /** Creation timestamp. */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    /** Last update timestamp. */
    @UpdateTimestamp
    private Instant updatedAt;
}
