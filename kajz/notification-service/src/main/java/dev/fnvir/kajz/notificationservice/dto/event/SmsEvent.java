package dev.fnvir.kajz.notificationservice.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for sending an SMS.
 * 
 * @param to      Recipient phone number in E.164 format.
 * @param message Content of the SMS message (max 1600 characters).
 */
public record SmsEvent(
        @NotBlank(message = "Recipient phone number cannot be empty")
        @Pattern(regexp = "^\\+[1-9]\\d{9,14}$", message = "Phone number must be in E.164 format (e.g., +1234567890)")
        String to,
        
        @NotBlank(message = "Sms body cannot be empty")
        @Size(max = 1600, message = "Sms content must not exceed 1600 characters")
        String message
) {}
