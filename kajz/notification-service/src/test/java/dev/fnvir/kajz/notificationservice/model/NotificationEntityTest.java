package dev.fnvir.kajz.notificationservice.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.fnvir.kajz.notificationservice.model.enums.RecipientRole;

@DisplayName("Notification Entity Unit Tests")
class NotificationEntityTest {

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = new Notification();
    }

    @Nested
    @DisplayName("Default Value Tests")
    class DefaultValueTests {

        @Test
        @DisplayName("Should have read as false by default")
        void shouldHaveReadAsFalseByDefault() {
            assertFalse(notification.isRead());
        }

        @Test
        @DisplayName("Should have archived as false by default")
        void shouldHaveArchivedAsFalseByDefault() {
            assertFalse(notification.isArchived());
        }

        @Test
        @DisplayName("Should have empty metadata map by default")
        void shouldHaveEmptyMetadataMapByDefault() {
            assertNotNull(notification.getMetadata());
            assertTrue(notification.getMetadata().isEmpty());
        }
    }

    @Nested
    @DisplayName("Setter and Getter Tests")
    class SetterGetterTests {

        @Test
        @DisplayName("Should set and get id")
        void shouldSetAndGetId() {
            UUID id = UUID.randomUUID();

            notification.setId(id);

            assertEquals(notification.getId(), id);
        }

        @Test
        @DisplayName("Should set and get userId")
        void shouldSetAndGetUserId() {
            UUID userId = UUID.randomUUID();

            notification.setUserId(userId);

            assertEquals(notification.getUserId(), userId);
        }

        @Test
        @DisplayName("Should set and get recipientRole")
        void shouldSetAndGetRecipientRole() {
            notification.setRecipientRole(RecipientRole.ADMIN);

            assertEquals(notification.getRecipientRole(), RecipientRole.ADMIN);
        }

        @Test
        @DisplayName("Should set and get title")
        void shouldSetAndGetTitle() {
            notification.setTitle("Test Title");

            assertEquals(notification.getTitle(), "Test Title");
        }

        @Test
        @DisplayName("Should set and get body")
        void shouldSetAndGetBody() {
            notification.setBody("Test Body");

            assertEquals(notification.getBody(), "Test Body");
        }

        @Test
        @DisplayName("Should set and get type")
        void shouldSetAndGetType() {
            notification.setType("ORDER");

            assertEquals(notification.getType(), "ORDER");
        }

        @Test
        @DisplayName("Should set and get clickAction")
        void shouldSetAndGetClickAction() {
            notification.setClickAction("https://example.com/order/123");

            Assertions.assertThat(notification.getClickAction()).isEqualTo("https://example.com/order/123");
        }

        @Test
        @DisplayName("Should set and get read status")
        void shouldSetAndGetReadStatus() {
            notification.setRead(true);

            Assertions.assertThat(notification.isRead()).isTrue();
        }

        @Test
        @DisplayName("Should set and get archived status")
        void shouldSetAndGetArchivedStatus() {
            notification.setArchived(true);

            Assertions.assertThat(notification.isArchived()).isTrue();
        }

        @Test
        @DisplayName("Should set and get createdAt")
        void shouldSetAndGetCreatedAt() {
            Instant now = Instant.now();

            notification.setCreatedAt(now);

            Assertions.assertThat(notification.getCreatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should set and get updatedAt")
        void shouldSetAndGetUpdatedAt() {
            Instant now = Instant.now();

            notification.setUpdatedAt(now);

            Assertions.assertThat(notification.getUpdatedAt()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("Metadata Tests")
    class MetadataTests {

        @Test
        @DisplayName("Should set and get metadata")
        void shouldSetAndGetMetadata() {
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("orderId", "12345");
            metadata.put("amount", "100.00");

            notification.setMetadata(metadata);

            Assertions.assertThat(notification.getMetadata()).hasSize(2);
            Assertions.assertThat(notification.getMetadata().get("orderId")).isEqualTo("12345");
            Assertions.assertThat(notification.getMetadata().get("amount")).isEqualTo("100.00");
        }

        @Test
        @DisplayName("Should handle adding entries to metadata")
        void shouldHandleAddingEntriesToMetadata() {
            notification.getMetadata().put("key1", "value1");
            notification.getMetadata().put("key2", "value2");

            Assertions.assertThat(notification.getMetadata()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Static Factory Method Tests")
    class StaticFactoryMethodTests {

        @Test
        @DisplayName("Should create notification using of() method")
        void shouldCreateNotificationUsingOfMethod() {
            UUID id = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            RecipientRole role = RecipientRole.CLIENT;
            String title = "Test Notification";
            String body = "Test Body";
            String type = "PROMOTION";
            String clickAction = "https://example.com";
            Map<String, String> metadata = new LinkedHashMap<>();
            Instant now = Instant.now();

            Notification notification = Notification.of(
                    id, userId, role, title, body, type, clickAction, metadata, false, false, now, now
            );

            Assertions.assertThat(notification.getId()).isEqualTo(id);
            Assertions.assertThat(notification.getUserId()).isEqualTo(userId);
            Assertions.assertThat(notification.getRecipientRole()).isEqualTo(role);
            Assertions.assertThat(notification.getTitle()).isEqualTo(title);
            Assertions.assertThat(notification.getBody()).isEqualTo(body);
            Assertions.assertThat(notification.getType()).isEqualTo(type);
            Assertions.assertThat(notification.getClickAction()).isEqualTo(clickAction);
        }
    }

    @Nested
    @DisplayName("RecipientRole Enum Tests")
    class RecipientRoleEnumTests {

        @Test
        @DisplayName("Should have WORKER role")
        void shouldHaveWorkerRole() {
            Assertions.assertThat(RecipientRole.WORKER).isNotNull();
        }

        @Test
        @DisplayName("Should have CLIENT role")
        void shouldHaveClientRole() {
            Assertions.assertThat(RecipientRole.CLIENT).isNotNull();
        }

        @Test
        @DisplayName("Should have ADMIN role")
        void shouldHaveAdminRole() {
            Assertions.assertThat(RecipientRole.ADMIN).isNotNull();
        }

        @Test
        @DisplayName("Should have exactly 3 roles")
        void shouldHaveExactly3Roles() {
            Assertions.assertThat(RecipientRole.values()).hasSize(3);
        }
    }
}
