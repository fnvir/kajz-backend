package dev.fnvir.kajz.notificationservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import dev.fnvir.kajz.notificationservice.config.SecurityConfig;
import dev.fnvir.kajz.notificationservice.dto.res.CursorPageResponse;
import dev.fnvir.kajz.notificationservice.dto.res.NotificationResponse;
import dev.fnvir.kajz.notificationservice.model.enums.RecipientRole;
import dev.fnvir.kajz.notificationservice.service.NotificationService;

@WebFluxTest(controllers = NotificationController.class)
@Import(SecurityConfig.class)
@DisplayName("Notification Controller Tests")
public class NotificationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private NotificationService notificationService;

    @Nested
    @DisplayName("GET /notifications tests")
    class GetUserNotificationsTest {

        private UUID testUserId;
        private NotificationResponse testNotificationResponse;

        @BeforeEach
        void setUp() {
            testUserId = UUID.randomUUID();
            testNotificationResponse = createNotificationResponse(testUserId, RecipientRole.CLIENT);
        }

        @Test
        @DisplayName("Should return notifications when user accesses own notifications")
        void shouldReturnOwnNotificationsSuccessfully() {
            var response = new CursorPageResponse<>(
                List.of(testNotificationResponse),
                Instant.now()
            );

            when(notificationService.getNotificationsOfUser(eq(testUserId), isNull(), any(), eq(20)))
                .thenReturn(response);

            webTestClient
                .mutateWith(mockUser(testUserId.toString()).roles("NON_ADMIN"))
                .get()
                .uri(builder -> builder
                    .path("/notifications")
                    .queryParam("userId", testUserId)
                    .build()
                )
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[0].userId").isEqualTo(testUserId.toString())
                .jsonPath("$.content[0].title").isEqualTo("Test Notification")
                .jsonPath("$.nextCursor").exists();

            verify(notificationService).getNotificationsOfUser(eq(testUserId), isNull(), any(), eq(20));
        }

        @Test
        @DisplayName("Should return notifications when ADMIN accesses any user notifications")
        void shouldAllowAdminToAccessAnyUserNotifications() {
            var response = new CursorPageResponse<>(
                List.of(testNotificationResponse),
                Instant.now()
            );

            when(notificationService.getNotificationsOfUser(eq(testUserId), isNull(), any(), eq(20)))
                .thenReturn(response);

            webTestClient
                .mutateWith(mockUser(testUserId.toString()).roles("ADMIN"))
                .get()
                .uri(builder -> builder
                    .path("/notifications")
                    .queryParam("userId", testUserId)
                    .build()
                )
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[0].userId").isEqualTo(testUserId.toString());

            verify(notificationService).getNotificationsOfUser(eq(testUserId), isNull(), any(), eq(20));
        }

        @Test
        @DisplayName("Should deny access when user tries to access other user notifications")
        void shouldDenyAccessToOtherUserNotifications() {
            UUID otherUserId = UUID.randomUUID();

            webTestClient
                .mutateWith(mockUser(testUserId.toString()).roles("USER"))
                .get()
                .uri(builder -> builder
                    .path("/notifications")
                    .queryParam("userId", otherUserId)
                    .build()
                )
                .exchange()
                .expectStatus().isForbidden();

            verify(notificationService, never()).getNotificationsOfUser(any(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("Should deny access for unauthenticated requests")
        void shouldDenyUnauthenticatedAccess() {
            webTestClient
                .get()
                .uri(builder -> builder
                    .path("/notifications")
                    .queryParam("userId", testUserId)
                    .build()
                )
                .exchange()
                .expectStatus().isUnauthorized();

            verify(notificationService, never()).getNotificationsOfUser(any(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("Should return empty list when no notifications found")
        void shouldReturnEmptyListWhenNoNotificationsFound() {
            var emptyResponse = new CursorPageResponse<NotificationResponse>(
                Collections.emptyList(),
                null
            );

            when(notificationService.getNotificationsOfUser(eq(testUserId), isNull(), any(), eq(20)))
                .thenReturn(emptyResponse);

            webTestClient
                .mutateWith(mockUser(testUserId.toString()).roles("USER"))
                .get()
                .uri(builder -> builder
                    .path("/notifications")
                    .queryParam("userId", testUserId)
                    .build()
                )
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.content").isEmpty()
                .jsonPath("$.nextCursor").isEmpty();

            verify(notificationService).getNotificationsOfUser(eq(testUserId), isNull(), any(), eq(20));
        }

        @Test
        @DisplayName("Should use custom limit when provided")
        void shouldUseCustomLimitWhenProvided() {
            var response = new CursorPageResponse<>(
                List.of(testNotificationResponse),
                testNotificationResponse.createdAt()
            );

            when(notificationService.getNotificationsOfUser(eq(testUserId), isNull(), any(), eq(50)))
                .thenReturn(response);

            webTestClient
                .mutateWith(mockUser(testUserId.toString()).roles("ADMIN"))
                .get()
                .uri(builder -> builder
                    .path("/notifications")
                    .queryParam("userId", testUserId)
                    .queryParam("limit", 50)
                    .build()
                )
                .exchange()
                .expectStatus().isOk();

            verify(notificationService).getNotificationsOfUser(eq(testUserId), isNull(), any(), eq(50));
        }

        @Test
        @DisplayName("Should reject limit below minimum")
        void shouldRejectLimitBelowMinimum() {
            webTestClient
                .mutateWith(mockUser(testUserId.toString()).roles("ADMIN"))
                .get()
                .uri(builder -> builder
                    .path("/notifications")
                    .queryParam("userId", testUserId)
                    .queryParam("limit", 0)
                    .build()
                )
                .exchange()
                .expectStatus().isBadRequest();

            verify(notificationService, never()).getNotificationsOfUser(any(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("Should reject limit above maximum")
        void shouldRejectLimitAboveMaximum() {
            webTestClient
                .mutateWith(mockUser(testUserId.toString()).roles("ADMIN"))
                .get()
                .uri(builder -> builder
                    .path("/notifications")
                    .queryParam("userId", testUserId)
                    .queryParam("limit", 101)
                    .build()
                )
                .exchange()
                .expectStatus().isBadRequest();

            verify(notificationService, never()).getNotificationsOfUser(any(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("Should reject missing required userId parameter")
        void shouldRejectMissingUserId() {
            webTestClient
                .mutateWith(mockUser(testUserId.toString()).roles("ADMIN"))
                .get()
                .uri("/notifications")
                .exchange()
                .expectStatus().isBadRequest();

            verify(notificationService, never()).getNotificationsOfUser(any(), any(), any(), anyInt());
        }
    }

    private NotificationResponse createNotificationResponse(UUID userId, RecipientRole role) {
        return NotificationResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .recipientRole(role)
                .title("Test Notification")
                .body("Test Body")
                .type("TEST_TYPE")
                .clickAction("/test")
                .metadata(Map.of("key", "value"))
                .read(false)
                .archived(false)
                .createdAt(Instant.now())
                .build();
    }
}