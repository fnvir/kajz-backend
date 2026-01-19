package dev.fnvir.kajz.notificationservice.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.fnvir.kajz.notificationservice.model.Notification;
import dev.fnvir.kajz.notificationservice.model.enums.RecipientRole;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdAndRecipientRole(UUID userId, RecipientRole recipientRole, Pageable pageable);
    
    @Query("""
            FROM Notification n
            WHERE
               n.userId = :userId
             AND
               n.recipientRole = :recipientRole
             AND
               n.createdAt < :cursor
            ORDER BY n.createdAt DESC
            """)
    List<Notification> findByUserIdBeforeCursor(
            @NonNull UUID userId,
            @NonNull RecipientRole recipientRole,
            @NonNull Instant cursor,
            Limit limit
    );
    
}

