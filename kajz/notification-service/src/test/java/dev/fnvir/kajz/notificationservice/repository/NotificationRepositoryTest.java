package dev.fnvir.kajz.notificationservice.repository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import dev.fnvir.kajz.notificationservice.TestcontainersConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Limit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import dev.fnvir.kajz.notificationservice.model.Notification;
import dev.fnvir.kajz.notificationservice.model.enums.RecipientRole;
import jakarta.persistence.Table;

@DataJpaTest(showSql = false)
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "ENABLE_TC", matches = "true")
@ActiveProfiles("test")
@DisplayName("Notification Repository Tests")
public class NotificationRepositoryTest {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    
    @Nested
    @DisplayName("findByUserIdAfterCursor")
    class FindByUserIdAfterCursorTests {
        
        private UUID userId;
        private Instant cursor;
        
        private static final Random RANDOM = new Random();
        
        @BeforeEach
        void setUp() {
            userId = UUID.randomUUID();
            cursor = Instant.now().minus(RANDOM.nextInt(365 * 24), ChronoUnit.HOURS); // random cursor in the past
//            cursor = LocalDateTime.of(2026, 1, 1, 20, 0).toInstant(ZoneOffset.UTC);
        }
        
        @AfterEach
        void tearDown() {
            notificationRepository.deleteAll();
            notificationRepository.flush();
        }

        @Test
        @DisplayName("should return notifications created before cursor in descending order")
        void shouldReturnNotificationsBeforeCursorDescending() {
            // after cursor
            createAndSaveNewNotification(userId, RecipientRole.WORKER, "Future", cursor.plus(Duration.ofHours(2)));
            // at cursor
            createAndSaveNewNotification(userId, RecipientRole.WORKER, "Present", cursor);
            // before cursor
            Notification n2 = createAndSaveNewNotification(userId, RecipientRole.WORKER, "Old 2", cursor.minus(8, ChronoUnit.HOURS));
            Notification n1 = createAndSaveNewNotification(userId, RecipientRole.WORKER, "Old 1", cursor.minus(10, ChronoUnit.HOURS));
            
            notificationRepository.flush();
            
            List<Notification> result = notificationRepository.findByUserIdBeforeCursor(
                    userId, RecipientRole.WORKER, cursor, Limit.of(10)
            );
            
            Assertions.assertThat(result)
                    .hasSize(2)
                    .extracting(Notification::getTitle)
                    .containsExactly(n2.getTitle(), n1.getTitle());
        }

        @Test
        @DisplayName("should respect limit parameter")
        void shouldRespectLimit() {
            for (int i = 0; i < 10; i++) {
                createAndSaveNewNotification(
                        userId,
                        RecipientRole.WORKER,
                        "N" + i,
                        cursor.minus(i + 1, ChronoUnit.HOURS)
                );
            }
            notificationRepository.flush();

            List<Notification> result = notificationRepository.findByUserIdBeforeCursor(
                    userId, RecipientRole.WORKER, cursor, Limit.of(5)
            );

            Assertions.assertThat(result).hasSize(5);
        }

        @Test
        @DisplayName("should filter by userId correctly")
        void shouldFilterByUserId() {
            createAndSaveNewNotification(userId, RecipientRole.WORKER, "Worker", cursor.minus(1, ChronoUnit.HOURS));
            createAndSaveNewNotification(UUID.randomUUID(), RecipientRole.CLIENT, "Client", cursor.minus(1, ChronoUnit.HOURS));
            notificationRepository.flush();

            List<Notification> result = notificationRepository.findByUserIdBeforeCursor(
                    userId, RecipientRole.WORKER, cursor, Limit.of(10)
            );

            Assertions.assertThat(result).hasSize(1)
                    .first()
                    .extracting(Notification::getTitle)
                    .isEqualTo("Worker");
        }

        @Test
        @DisplayName("should filter by recipientRole correctly")
        void shouldFilterByRecipientRole() {
            createAndSaveNewNotification(userId, RecipientRole.WORKER, "Worker Role", cursor.minus(1, ChronoUnit.HOURS));
            createAndSaveNewNotification(userId, RecipientRole.CLIENT, "Client Role", cursor.minus(1, ChronoUnit.HOURS));
            notificationRepository.flush();

            List<Notification> result = notificationRepository.findByUserIdBeforeCursor(
                    userId, RecipientRole.WORKER, cursor, Limit.of(10)
            );

            Assertions.assertThat(result).hasSize(1)
                    .first()
                    .extracting(Notification::getRecipientRole)
                    .isEqualTo(RecipientRole.WORKER);
        }

        @Test
        @DisplayName("should return empty list when no notifications before cursor")
        void shouldReturnEmptyWhenNoNotificationsBeforeCursor() {
            createAndSaveNewNotification(userId, RecipientRole.WORKER, "Recent 1", cursor.plus(Duration.ofHours(2)));
            createAndSaveNewNotification(userId, RecipientRole.WORKER, "Recent 2", cursor.plus(Duration.ofHours(3)));
            notificationRepository.flush();

            List<Notification> result = notificationRepository.findByUserIdBeforeCursor(
                    userId, RecipientRole.WORKER, cursor, Limit.of(10)
            );

            Assertions.assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should exclude notifications exactly at cursor timestamp")
        void shouldExcludeNotificationsAtCursorTimestamp() {
            createAndSaveNewNotification(userId, RecipientRole.WORKER, "At Cursor", cursor);
            createAndSaveNewNotification(userId, RecipientRole.WORKER, "Before Cursor", cursor.minus(1, ChronoUnit.MILLIS));
            notificationRepository.flush();

            List<Notification> result = notificationRepository.findByUserIdBeforeCursor(
                    userId, RecipientRole.WORKER, cursor, Limit.of(10)
            );

            Assertions.assertThat(result).hasSize(1)
                    .first()
                    .extracting(Notification::getTitle)
                    .isEqualTo("Before Cursor");
        }

        @Test
        @DisplayName("should handle notifications with identical timestamps")
        void shouldHandleIdenticalTimestamps() {
            Instant sameTime = cursor.minus(5, ChronoUnit.HOURS);

            createAndSaveNewNotification(userId, RecipientRole.WORKER, "N1", sameTime);
            createAndSaveNewNotification(userId, RecipientRole.WORKER, "N2", sameTime);
            createAndSaveNewNotification(userId, RecipientRole.WORKER, "N3", sameTime);
            notificationRepository.flush();

            List<Notification> result = notificationRepository.findByUserIdBeforeCursor(
                    userId, RecipientRole.WORKER, cursor, Limit.of(10)
            );

            Assertions.assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("should work with minimum limit of 1")
        void shouldWorkWithMinimumLimit() {
            createAndSaveNewNotification(userId, RecipientRole.WORKER, "N1", cursor.minus(3, ChronoUnit.HOURS));
            createAndSaveNewNotification(userId, RecipientRole.WORKER, "N2", cursor.minus(2, ChronoUnit.HOURS));
            notificationRepository.flush();

            List<Notification> result = notificationRepository.findByUserIdBeforeCursor(
                    userId, RecipientRole.WORKER, cursor, Limit.of(1)
            );

            Assertions.assertThat(result).hasSize(1)
                    .first()
                    .extracting(Notification::getTitle)
                    .isEqualTo("N2");
        }
    }

    // Helpers
    
    private Notification createAndSaveNewNotification(UUID userId, RecipientRole role, String title, Instant createdAt) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setRecipientRole(role);
        n.setTitle(title);
        n.setBody("Body for " + title);
        n.setType("EXAMPLE");
        n = notificationRepository.saveAndFlush(n);
        
        // force override default createdAt
        String tableName = getTableName(Notification.class);
        jdbcTemplate.update("UPDATE %s SET created_at = ? WHERE id = ?".formatted(tableName), Timestamp.from(createdAt), n.getId());
        
        n.setCreatedAt(createdAt);
        
        return n;
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
