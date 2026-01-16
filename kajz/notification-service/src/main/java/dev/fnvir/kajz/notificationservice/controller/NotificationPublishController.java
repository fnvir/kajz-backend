package dev.fnvir.kajz.notificationservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.fnvir.kajz.notificationservice.dto.event.EmailEvent;
import dev.fnvir.kajz.notificationservice.service.event.NotificationEventProducer;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Controller for publishing notifications by admin.
 */
@Validated
@Tag(name = "Notification Publish Controller (Admin Only)")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationPublishController {
    
    private final NotificationEventProducer notificationProducer;
    
    /**
     * To send an email manually through API.
     *
     * @param emailDto the email request payload
     * @return a response entity indicating the request was accepted (202 Accepted)
     */
    @PostMapping("/email")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public Mono<ResponseEntity<Void>> sendEmailNotification(@RequestBody @Valid EmailEvent emailDto) {
        return Mono.fromFuture(notificationProducer.publishEmailNotification(emailDto))
                .thenReturn(ResponseEntity.accepted().build());
    }
    
}