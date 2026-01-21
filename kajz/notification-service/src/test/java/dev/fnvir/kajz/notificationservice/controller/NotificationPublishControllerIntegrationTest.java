package dev.fnvir.kajz.notificationservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import dev.fnvir.kajz.notificationservice.TestcontainersConfiguration;
import dev.fnvir.kajz.notificationservice.dto.event.PushNotificationEvent;
import dev.fnvir.kajz.notificationservice.model.enums.RecipientRole;
import dev.fnvir.kajz.notificationservice.service.NotificationEventProducer;
import dev.fnvir.kajz.notificationservice.service.email.EmailSenderService;
import dev.fnvir.kajz.notificationservice.service.sms.SmsSenderService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "ENABLE_TC", matches = "true")
@AutoConfigureWebTestClient
@DisplayName("NotificationPublishController Integration Tests")
class NotificationPublishControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private EmailSenderService emailService;
    
    @MockitoBean
    private SmsSenderService smsService;

    @Nested
    @DisplayName("Email Endpoint Tests")
    class EmailEndpointTests {
        
        @BeforeEach
        void init() {
            doNothing().when(emailService)
            .sendEmail(anyCollection(), anyCollection(), anyCollection(), anyString(), anyString(),
                    anyBoolean(), anyInt());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should accept valid email notification request with ADMIN role")
        void shouldAcceptValidEmailNotificationRequestWithAdminRole() {
            webTestClient.post()
                    .uri("/notifications/publish/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue(createValidEmailRequest())
                    .exchange()
                    .expectStatus().isAccepted()
                    .expectBody().isEmpty();
        }

        @Test
        @WithMockUser(roles = "SYSTEM")
        @DisplayName("Should accept valid email notification request with SYSTEM role")
        void shouldAcceptValidEmailNotificationRequestWithSystemRole() {
            webTestClient.post()
                    .uri("/notifications/publish/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue(createValidEmailRequest())
                    .exchange()
                    .expectStatus().isAccepted();
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should reject email notification request without proper role")
        void shouldRejectEmailNotificationRequestWithoutProperRole() {
            webTestClient.post()
                    .uri("/notifications/publish/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue(createValidEmailRequest())
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("Should reject unauthenticated email notification request")
        void shouldRejectUnauthenticatedEmailNotificationRequest() {
            webTestClient.post()
                    .uri("/notifications/publish/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue(createValidEmailRequest())
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request for invalid email event")
        void shouldReturnBadRequestForInvalidEmailEvent() {
            webTestClient.post()
                    .uri("/notifications/publish/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue("""
                            {
                                "to": [],
                                "subject": "",
                                "content": ""
                            }
                            """)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request for invalid email format")
        void shouldReturnBadRequestForInvalidEmailFormat() {
            webTestClient.post()
                    .uri("/notifications/publish/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue("""
                            {
                                "to": ["invalid-email"],
                                "subject": "Test",
                                "content": "Content"
                            }
                            """)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should accept email with CC and BCC")
        void shouldAcceptEmailWithCcAndBcc() {
            webTestClient.post()
                    .uri("/notifications/publish/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue("""
                            {
                                "to": ["to@example.com"],
                                "cc": ["cc@example.com"],
                                "bcc": ["bcc@example.com"],
                                "subject": "Test Subject",
                                "content": "Test Content",
                                "isHtml": true,
                                "priority": 1
                            }
                            """)
                    .exchange()
                    .expectStatus().isAccepted();
        }
    }

    @Nested
    @DisplayName("SMS Endpoint Tests")
    class SmsEndpointTests {
        
        @BeforeEach
        void init() {
            when(smsService.sendSms(anyString(), anyString())).thenReturn("sent");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should accept valid SMS notification request with ADMIN role")
        void shouldAcceptValidSmsNotificationRequestWithAdminRole() {
            webTestClient.post()
                    .uri("/notifications/publish/sms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue(createValidSmsRequest())
                    .exchange()
                    .expectStatus().isAccepted()
                    .expectBody().isEmpty();
        }

        @Test
        @WithMockUser(roles = "SYSTEM")
        @DisplayName("Should accept valid SMS notification request with SYSTEM role")
        void shouldAcceptValidSmsNotificationRequestWithSystemRole() {
            webTestClient.post()
                    .uri("/notifications/publish/sms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue(createValidSmsRequest())
                    .exchange()
                    .expectStatus().isAccepted();
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should reject SMS notification request without proper role")
        void shouldRejectSmsNotificationRequestWithoutProperRole() {
            webTestClient.post()
                    .uri("/notifications/publish/sms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue(createValidSmsRequest())
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("Should reject unauthenticated SMS notification request")
        void shouldRejectUnauthenticatedSmsNotificationRequest() {
            webTestClient.post()
                    .uri("/notifications/publish/sms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue(createValidSmsRequest())
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request for invalid phone number format")
        void shouldReturnBadRequestForInvalidPhoneNumberFormat() {
            webTestClient.post()
                    .uri("/notifications/publish/sms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue("""
                            {
                                "to": "invalid-phone",
                                "message": "Test message"
                            }
                            """)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request for empty SMS message")
        void shouldReturnBadRequestForEmptySmsMessage() {
            webTestClient.post()
                    .uri("/notifications/publish/sms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue("""
                            {
                                "to": "+1234567890",
                                "message": ""
                            }
                            """)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request for missing phone number")
        void shouldReturnBadRequestForMissingPhoneNumber() {
            webTestClient.post()
                    .uri("/notifications/publish/sms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue("""
                            {
                                "message": "Test message"
                            }
                            """)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request for phone number without country code")
        void shouldReturnBadRequestForPhoneNumberWithoutCountryCode() {
            webTestClient.post()
                    .uri("/notifications/publish/sms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue("""
                            {
                                "to": "1234567890",
                                "message": "Test message"
                            }
                            """)
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }
    
    @Nested
    @DisplayName("Push Notification Endpoint Tests")
    class PushNotificationEndpointTests {

        @MockitoBean
        private NotificationEventProducer notificationProducer;

        @BeforeEach
        void init() {
            when(notificationProducer.sendPushNotification(any(PushNotificationEvent.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should accept valid push notification request with ADMIN role")
        void shouldAcceptValidPushNotificationRequestWithAdminRole() {
            webTestClient.post()
                    .uri("/notifications/publish/push")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue(createValidPushRequest())
                    .exchange()
                    .expectStatus().isAccepted()
                    .expectBody().isEmpty();
        }

        @Test
        @WithMockUser(roles = "SYSTEM")
        @DisplayName("Should accept valid push notification request with SYSTEM role")
        void shouldAcceptValidPushNotificationRequestWithSystemRole() {
            webTestClient.post()
                    .uri("/notifications/publish/push")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue(createValidPushRequest())
                    .exchange()
                    .expectStatus().isAccepted();
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should reject push notification request for unauthorized role")
        void shouldRejectPushNotificationRequestForUserRole() {
            webTestClient.post()
                    .uri("/notifications/publish/push")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue(createValidPushRequest())
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request when title is missing")
        void shouldReturnBadRequestWhenTitleIsMissing() {
            String invalidRequest = """
                    {
                        "userId": "%s",
                        "recipientRole": "CUSTOMER",
                        "body": "Missing title"
                    }
                    """.formatted(UUID.randomUUID());

            webTestClient.post()
                    .uri("/notifications/publish/push")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue(invalidRequest)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request when metadata exceeds max size")
        void shouldReturnBadRequestWhenMetadataTooLarge() {
            // Generating a request with 11 metadata entries (limit is 10)
            Map<String, String> largeMetadata = new HashMap<>();
            for (int i = 0; i < 11; i++) {
                largeMetadata.put("key" + i, "val" + i);
            }

            PushNotificationEvent event = new PushNotificationEvent();
            event.setUserId(UUID.randomUUID());
            event.setRecipientRole(RecipientRole.WORKER);
            event.setTitle("Promo");
            event.setMetadata(largeMetadata);

            webTestClient.post()
                    .uri("/notifications/publish/push")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Version", "1")
                    .bodyValue(event)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        private String createValidPushRequest() {
            return """
                    {
                        "userId": "%s",
                        "recipientRole": "CLIENT",
                        "title": "Welcome!",
                        "body": "Thanks for joining our platform.",
                        "type": "SYSTEM_ALERT",
                        "clickAction": "https://app.example.com/home",
                        "metadata": {
                            "source": "onboarding",
                            "priority": "high"
                        }
                    }
                    """.formatted(UUID.randomUUID());
        }
    }

    private String createValidEmailRequest() {
        return """
                {
                    "to": ["test@example.com"],
                    "subject": "Test Subject",
                    "content": "Test Content",
                    "isHtml": false
                }
                """;
    }

    private String createValidSmsRequest() {
        return """
                {
                    "to": "+1234567890",
                    "message": "Test SMS message"
                }
                """;
    }

}
