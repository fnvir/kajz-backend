package dev.fnvir.kajz.notificationservice.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.fnvir.kajz.notificationservice.dto.res.CursorPageResponse;
import dev.fnvir.kajz.notificationservice.dto.res.NotificationResponse;
import dev.fnvir.kajz.notificationservice.model.enums.RecipientRole;
import dev.fnvir.kajz.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Tag(name = "Notification Crud Controller", description = "API endpoints for notification CRUD operations")
@Validated
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    /**
     * Get notifications of a user with cursor-based pagination.
     * 
     * @param userId        the ID of the user
     * @param recipientRole (optional) the role of notification recipient user.
     *                      If null, then ignored.
     * @param cursor        (optional) the cursor for pagination
     * @param limit         (optional) the maximum number of notifications to retrieve
     *                      (default is 20, max is 100)
     * @return a paginated list of notifications for the user
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM') or #userId.toString() == authentication.name")
    public Mono<CursorPageResponse<NotificationResponse>> getNotificationsOfUser(
            @RequestParam UUID userId,
            @RequestParam(required = false) RecipientRole recipientRole,
            @RequestParam(required = false) Instant cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return Mono.fromCallable(() ->
            notificationService.getNotificationsOfUser(userId, recipientRole, cursor, limit)
        ).subscribeOn(Schedulers.boundedElastic());
    }

}
