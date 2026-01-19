package dev.fnvir.kajz.notificationservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.fnvir.kajz.notificationservice.dto.event.PushNotificationEvent;
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

}
