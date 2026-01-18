package dev.fnvir.kajz.notificationservice.dto.res;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import dev.fnvir.kajz.notificationservice.model.enums.RecipientRole;

/**
 * Payload for notification response.
 * 
 * @param id            the unique id of the notification
 * @param userId        the id of the user receiving the notification
 * @param recipientType the type of user (e.g., SELLER, CUSTOMER)
 * @param title         the title of the notification
 * @param body          the notification body/content
 * @param type          the type/category of the notification (e.g., new-order,
 *                      system-alert)
 * @param clickAction   the action to perform when the notification is clicked
 *                      (e.g., URL or app action)
 * @param read          the read status of the notification
 * @param archived      the archived status of the notification
 * @param createdAt     the timestamp when the notification was created
 */
public record NotificationResponse (
        UUID id,
        UUID userId,
        RecipientRole recipientRole,
        String title,
        String body,
        String type,
        String clickAction,
        Map<String, String> metadata,
        Boolean read,
        Boolean archived,
        Instant createdAt
) {}
