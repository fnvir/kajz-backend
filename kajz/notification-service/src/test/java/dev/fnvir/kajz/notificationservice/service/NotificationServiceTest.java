package dev.fnvir.kajz.notificationservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import dev.fnvir.kajz.notificationservice.dto.event.PushNotificationEvent;
import dev.fnvir.kajz.notificationservice.dto.res.CursorPageResponse;
import dev.fnvir.kajz.notificationservice.dto.res.NotificationResponse;
import dev.fnvir.kajz.notificationservice.mapper.NotificationMapper;
import dev.fnvir.kajz.notificationservice.model.Notification;
import dev.fnvir.kajz.notificationservice.model.enums.RecipientRole;
import dev.fnvir.kajz.notificationservice.repository.NotificationRepository;
import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Service Unit Tests")
public class NotificationServiceTest {
    
    @Mock
    private NotificationRepository notificationRepository;
    
    private NotificationService notificationService;
    
    @BeforeEach
    void setup() {
        notificationService = new NotificationService(notificationRepository, NotificationMapper.INSTANCE);
    }
    
    @Nested
    @DisplayName("Save Notification Tests")
    class SaveNotificationTests {
        
        @Test
        @DisplayName("Should successfully save valid notification")
        void shouldSuccessfullySaveNotification() {
            var event = createPushNotificationEvent();
            
            when(notificationRepository.saveAndFlush(any(Notification.class))).then(e -> mockPersist(e.getArgument(0)));
            
            NotificationResponse result = notificationService.saveNotification(event);
            
            assertNotNull(result.id());
            assertEquals(result.userId(), event.getUserId());
            assertEquals(result.recipientRole(), event.getRecipientRole());
            assertEquals(result.title(), event.getTitle());
            assertEquals(result.body(), event.getBody());
            assertFalse(result.read());
            assertFalse(result.archived());
            
            verify(notificationRepository).saveAndFlush(any(Notification.class));
        }
        
        // helpers
        
        Notification mockPersist(Notification notification) {
            if (notification.getUserId() == null) {
                throw new ConstraintViolationException("userId cannot be null", Set.of());
            }
            if (notification.getRecipientRole() == null) {
                throw new ConstraintViolationException("recipientRole cannot be null", Set.of());
            }
            if (notification.getTitle() == null) {
                throw new ConstraintViolationException("title cannot be null", Set.of());
            }
            if (notification.getMetadata() != null && notification.getMetadata().size() > 10) {
                throw new ConstraintViolationException("metadata size cannot exceed 10", Set.of());
            }
            notification.setId(UUID.randomUUID());
            notification.setCreatedAt(Instant.now());
            notification.setUpdatedAt(Instant.now());
            return notification;
        }
        
        private PushNotificationEvent createPushNotificationEvent() {
            var pn = new PushNotificationEvent();
            pn.setUserId(UUID.randomUUID());
            pn.setRecipientRole(RecipientRole.CLIENT);
            pn.setTitle("Push notification");
            pn.setBody("Test notification body");
            pn.setType("NEW_ORDER_TEST");
            return pn;
        }
    }
    
    @Nested
    @DisplayName("Get Notifications Tests")
    class GetNotificationsTests {

        private UUID testUserId;
        private Instant testCursor;
        private Notification testNotification;

        @BeforeEach
        void setUpGetTests() {
            testUserId = UUID.randomUUID();
            testCursor = Instant.now();
            testNotification = notification(testUserId, testCursor.minusSeconds(1000));
        }
        
        @Test
        @DisplayName("Should return notifications ordered by createdAt desc and next cursor")
        void shouldReturnNotificationsWithNextCursor() {
            UUID userId = UUID.randomUUID();
            Instant cursor = Instant.now();

            var n1 = notification(userId, cursor.minusSeconds(5));
            var n2 = notification(userId, cursor.minusSeconds(10));

            when(notificationRepository.findByUserIdBeforeCursor(
                    eq(userId),
                    isNull(),
                    eq(cursor),
                    any()
            )).thenReturn(List.of(n1, n2));

            CursorPageResponse<NotificationResponse> result =
                    notificationService.getNotificationsOfUser(userId, null, cursor, 2);

            assertEquals(2, result.content().size());
            assertEquals(n2.getCreatedAt(), result.nextCursor());

            verify(notificationRepository)
                    .findByUserIdBeforeCursor(eq(userId), isNull(), eq(cursor), any());
        }

        @Test
        @DisplayName("Should get notifications with all parameters")
        void shouldGetNotificationsWithAllParameters() {
            when(notificationRepository.findByUserIdBeforeCursor(
                eq(testUserId),
                eq(RecipientRole.CLIENT),
                eq(testCursor),
                any(Limit.class)
            )).thenReturn(List.of(testNotification));

            CursorPageResponse<NotificationResponse> result = notificationService
                    .getNotificationsOfUser(testUserId, RecipientRole.CLIENT, testCursor, 20);

            assertNotNull(result);
            assertTrue(result.content().size() == 1);
            assertEquals(result.content().get(0).id(), testNotification.getId());
            assertEquals(result.content().get(0).title(),testNotification.getTitle());
            assertEquals(result.nextCursor(), testNotification.getCreatedAt());

            verify(notificationRepository).findByUserIdBeforeCursor(
                eq(testUserId),
                eq(RecipientRole.CLIENT),
                eq(testCursor),
                any(Limit.class)
            );
        }

        @Test
        @DisplayName("Should use current time when cursor is null")
        void shouldUseCurrentTimeWhenCursorIsNull() {
            when(notificationRepository.findByUserIdBeforeCursor(
                eq(testUserId),
                isNull(),
                any(Instant.class),
                any(Limit.class)
            )).thenReturn(List.of(testNotification));

            CursorPageResponse<NotificationResponse> result = notificationService
                    .getNotificationsOfUser(testUserId, null, null, 20);

            assertNotNull(result);
            verify(notificationRepository).findByUserIdBeforeCursor(
                eq(testUserId),
                isNull(),
                any(Instant.class),
                any(Limit.class)
            );
        }

        @Test
        @DisplayName("Should return empty result when no notifications found")
        void shouldReturnEmptyResultWhenNoNotificationsFound() {
            when(notificationRepository.findByUserIdBeforeCursor(
                any(UUID.class),
                any(),
                any(Instant.class),
                any(Limit.class)
            )).thenReturn(Collections.emptyList());

            CursorPageResponse<NotificationResponse> result = notificationService
                    .getNotificationsOfUser(testUserId, null, testCursor, 20);

            assertNotNull(result);
            assertTrue(result.content().isEmpty());
        }

        @Test
        @DisplayName("Should return null cursor when result is null")
        void shouldReturnNullCursorWhenResultIsNull() {
            when(notificationRepository.findByUserIdBeforeCursor(
                any(UUID.class),
                any(),
                any(Instant.class),
                any(Limit.class)
            )).thenReturn(List.of());

            CursorPageResponse<NotificationResponse> result = notificationService
                    .getNotificationsOfUser(testUserId, null, testCursor, 20);

            assertNotNull(result);
            assertNull(result.nextCursor());
        }

        @Test
        @DisplayName("Should pass limit to repository")
        void shouldPassLimitToRepository() {
            int customLimit = 50;
            when(notificationRepository.findByUserIdBeforeCursor(
                any(UUID.class),
                any(),
                any(Instant.class),
                eq(Limit.of(customLimit))
            )).thenReturn(Collections.emptyList());

            notificationService.getNotificationsOfUser(testUserId, null, testCursor, customLimit);

            verify(notificationRepository).findByUserIdBeforeCursor(
                eq(testUserId),
                isNull(),
                any(Instant.class),
                eq(Limit.of(customLimit))
            );
        }

        @Test
        @DisplayName("Should filter by recipient role")
        void shouldFilterByRecipientRole() {
            when(notificationRepository.findByUserIdBeforeCursor(
                eq(testUserId),
                eq(RecipientRole.WORKER),
                any(Instant.class),
                any(Limit.class)
            )).thenReturn(List.of(testNotification));

            notificationService.getNotificationsOfUser(
                testUserId, 
                RecipientRole.WORKER, 
                testCursor, 
                20
            );

            verify(notificationRepository).findByUserIdBeforeCursor(
                eq(testUserId),
                eq(RecipientRole.WORKER),
                any(Instant.class),
                any(Limit.class)
            );
        }
        
        private Notification notification(UUID userId, Instant createdAt) {
            var n = new Notification();
            n.setId(UUID.randomUUID());
            n.setUserId(userId);
            n.setRecipientRole(RecipientRole.CLIENT);
            n.setTitle("Title");
            n.setBody("Body");
            n.setCreatedAt(createdAt);
            return n;
        }
    }
    
}
