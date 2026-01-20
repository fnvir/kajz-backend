package dev.fnvir.kajz.notificationservice.controller;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.fnvir.kajz.notificationservice.dto.res.NotificationResponse;
import dev.fnvir.kajz.notificationservice.model.enums.RecipientRole;
import dev.fnvir.kajz.notificationservice.service.push.PushNotificationSseService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * Controller for subscribing to push notifications via Server-Sent Events (SSE).
 */
@RestController
@RequestMapping("/notifications/sse")
@RequiredArgsConstructor
public class PushNotificationSseController {
    
    private final PushNotificationSseService notificationSseService;

    /**
     * Establishes an SSE (Server-Sent Event) stream for real-time notifications.<br>
     * The notifications are filtered based on the user's role, ensuring each client
     * receives only notifications relevant to their platform (e.g., seller app
     * receives seller notifications, customer app receives customer notifications,
     * admin-panel receives ADMIN notifications, etc.).
     * 
     * <p>
     * Supports Last-Event-ID reconnection, which can be provided either as a header
     * or query parameter to support different client implementations. Header is
     * prioritized over param if both specified.
     * </p>
     * 
     * @param userId           the ID of the user
     * @param role             the role of the user to filter notifications
     * @param lastEventId      the last received event ID provided in the HTTP
     *                         header
     * @param lastEventIdParam the last received event ID provided as a query
     *                         parameter
     * @return a stream of Server-Sent Events containing filtered notifications
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotificationResponse>> subscribe(
            Authentication authentication,
            @RequestParam RecipientRole role,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
            @RequestParam(value = "Last-Event-ID", required = false) String lastEventIdParam
    ) {
        UUID authUserId = UUID.fromString(authentication.getName());
        return notificationSseService.subscribe(authUserId, role, lastEventId != null ? lastEventId : lastEventIdParam);
    }
}
