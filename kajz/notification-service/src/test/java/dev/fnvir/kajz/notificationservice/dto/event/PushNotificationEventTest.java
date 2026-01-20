package dev.fnvir.kajz.notificationservice.dto.event;

import java.util.Set;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.fnvir.kajz.notificationservice.model.enums.RecipientRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class PushNotificationEventTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private PushNotificationEvent createValidEvent() {
        PushNotificationEvent event = new PushNotificationEvent();
        event.setUserId(UUID.randomUUID());
        event.setRecipientRole(RecipientRole.CLIENT);
        event.setTitle("Order Shipped");
        event.setBody("Your order is on the way!");
        event.setType("SHIPPING_UPDATE");
        return event;
    }

    @Test
    @DisplayName("Should pass validation when all required fields are valid")
    void validEvent_ShouldHaveNoViolations() {
        PushNotificationEvent event = createValidEvent();
        
        Set<ConstraintViolation<PushNotificationEvent>> violations = validator.validate(event);
        
        Assertions.assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when userId is null")
    void nullUserId_ShouldTriggerViolation() {
        PushNotificationEvent event = createValidEvent();
        event.setUserId(null);

        Set<ConstraintViolation<PushNotificationEvent>> violations = validator.validate(event);

        Assertions.assertThat(violations).hasSize(1);
        Assertions.assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("userId");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("Should fail validation when title is blank or null")
    void blankTitle_ShouldTriggerViolation(String invalidTitle) {
        PushNotificationEvent event = createValidEvent();
        event.setTitle(invalidTitle);

        Set<ConstraintViolation<PushNotificationEvent>> violations = validator.validate(event);

        Assertions.assertThat(violations).hasSize(1);
        Assertions.assertThat(violations.iterator().next().getMessage()).isEqualTo("Title is required");
    }

    @Test
    @DisplayName("Should fail when metadata exceeds maximum size of 10")
    void metadataTooLarge_ShouldTriggerViolation() {
        PushNotificationEvent event = createValidEvent();
        for (int i = 0; i < 11; i++) {
            event.getMetadata().put("key" + i, "value" + i);
        }

        Set<ConstraintViolation<PushNotificationEvent>> violations = validator.validate(event);

        Assertions.assertThat(violations).hasSize(1);
        Assertions.assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("metadata");
    }

    @Test
    @DisplayName("Should fail when metadata key or value exceeds character limits")
    void metadataContentTooLong_ShouldTriggerViolations() {
        PushNotificationEvent event = createValidEvent();
        // Key > 50 chars or Value > 100 chars
        event.getMetadata().put("a".repeat(51), "validValue");
        event.getMetadata().put("validKey", "b".repeat(101));

        Set<ConstraintViolation<PushNotificationEvent>> violations = validator.validate(event);

        // Expecting 2 violations because @Valid triggers nested validation
        Assertions.assertThat(violations).hasSize(2);
    }

    @Test
    @DisplayName("Verify Lombok @Data: equals and hashCode")
    void equalsAndHashCode_ShouldWorkCorrectlly() {
        UUID id = UUID.randomUUID();
        PushNotificationEvent event1 = createValidEvent();
        event1.setUserId(id);

        PushNotificationEvent event2 = createValidEvent();
        event2.setUserId(id);

        Assertions.assertThat(event1).isEqualTo(event2);
        Assertions.assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
        
        event2.setTitle("Different Title");
        Assertions.assertThat(event1).isNotEqualTo(event2);
    }
}