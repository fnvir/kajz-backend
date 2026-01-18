package dev.fnvir.kajz.notificationservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.fnvir.kajz.notificationservice.dto.event.PushNotificationEvent;
import dev.fnvir.kajz.notificationservice.dto.res.NotificationResponse;
import dev.fnvir.kajz.notificationservice.mapper.NotificationMapper;
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

}
