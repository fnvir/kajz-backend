package dev.fnvir.kajz.notificationservice.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.fnvir.kajz.notificationservice.dto.event.PushNotificationEvent;
import dev.fnvir.kajz.notificationservice.dto.res.CursorPageResponse;
import dev.fnvir.kajz.notificationservice.dto.res.NotificationResponse;
import dev.fnvir.kajz.notificationservice.exception.ForbiddenException;
import dev.fnvir.kajz.notificationservice.exception.NotFoundException;
import dev.fnvir.kajz.notificationservice.mapper.NotificationMapper;
import dev.fnvir.kajz.notificationservice.model.Notification;
import dev.fnvir.kajz.notificationservice.model.enums.RecipientRole;
import dev.fnvir.kajz.notificationservice.repository.NotificationRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    
    @Transactional
    public NotificationResponse saveNotification(@Valid PushNotificationEvent dto) {
        var notification = notificationMapper.toEntity(dto);
        notification = notificationRepository.saveAndFlush(notification);
        return notificationMapper.toResponseDto(notification);
    }

    public CursorPageResponse<NotificationResponse> getNotificationsOfUser(UUID userId,
            RecipientRole recipientRole,
            Instant cursor,
            int limit
    ) {
        cursor = cursor == null ? Instant.now() : cursor;
        List<NotificationResponse> res = notificationRepository
                .findByUserIdBeforeCursor(userId, recipientRole, cursor, Limit.of(limit))
                .stream()
                .map(notificationMapper::toResponseDto)
                .toList();
        Instant nextCursor = res == null || res.isEmpty() ? null : res.getLast().createdAt();
        return new CursorPageResponse<>(res, nextCursor);
    }
    
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        var notification = getNotificationWithOwnerValidation(notificationId, userId);
        notification.setRead(true);
        notification = notificationRepository.save(notification);
        return notificationMapper.toResponseDto(notification);
    }
    
    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        var notification = getNotificationWithOwnerValidation(notificationId, userId);
        notificationRepository.delete(notification);
    }
    
    private Notification getNotificationWithOwnerValidation(UUID notificationId, UUID userId) {
        var notification = notificationRepository.findById(notificationId).orElseThrow(NotFoundException::new);
        if(!notification.getUserId().equals(userId))
            throw new ForbiddenException();
        return notification;
    }

}
