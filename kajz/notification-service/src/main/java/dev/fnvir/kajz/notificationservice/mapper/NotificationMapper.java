package dev.fnvir.kajz.notificationservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import dev.fnvir.kajz.notificationservice.dto.event.PushNotificationEvent;
import dev.fnvir.kajz.notificationservice.dto.res.NotificationResponse;
import dev.fnvir.kajz.notificationservice.model.Notification;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    
    NotificationMapper INSTANCE = Mappers.getMapper(NotificationMapper.class);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "read", ignore = true)
    @Mapping(target = "archived", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Notification toEntity(PushNotificationEvent dto);
    
    NotificationResponse toResponseDto(Notification notification);
}
