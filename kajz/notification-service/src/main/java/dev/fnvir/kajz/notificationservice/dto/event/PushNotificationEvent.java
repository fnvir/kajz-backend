package dev.fnvir.kajz.notificationservice.dto.event;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import dev.fnvir.kajz.notificationservice.model.enums.RecipientRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for creating a push notification event.
 */
@Data
public class PushNotificationEvent {

    /**
     * The ID of the user to send the notification.
     */
    @NotNull
    private UUID userId;

    /**
     * The type of recipient (e.g., SELLER, CUSTOMER).
     */
    @NotNull
    private RecipientRole recipientRole;

    /**
     * The title of the notification.
     */
    @NotBlank(message = "Title is required")
    private String title;

    /**
     * The body content of the notification.
     */
    private String body;

    /**
     * The type/category of the notification.
     * 
     * <pre>
     *     Examples: NEW_ORDER, PROMOTION, SYSTEM_ALERT
     * </pre>
     */
    private String type;

    /**
     * URL or app deep link to open when the notification is clicked.
     */
    private String clickAction;

    /**
     * Additional metadata for the notification.
     */
    @Size(max = 10)
    @Valid
    private Map<@Size(max = 50) String, @Size(max = 100) String> metadata = new LinkedHashMap<>();

}
