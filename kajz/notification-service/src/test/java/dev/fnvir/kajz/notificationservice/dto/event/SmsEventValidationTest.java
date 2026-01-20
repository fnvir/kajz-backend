package dev.fnvir.kajz.notificationservice.dto.event;

import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@DisplayName("SmsEvent Validation Tests")
class SmsEventValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Valid SmsEvent Tests")
    class ValidSmsEventTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "+1234567890",
                "+12345678901",
                "+447911123456",
                "+919876543210",
                "+8613812345678",
                "+61412345678"
        })
        @DisplayName("Should pass validation with valid E.164 phone numbers")
        void shouldPassValidationWithValidE164PhoneNumbers(String phoneNumber) {
            SmsEvent event = new SmsEvent(phoneNumber, "Test message");

            Set<ConstraintViolation<SmsEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should pass validation with minimum length message")
        void shouldPassValidationWithMinimumLengthMessage() {
            SmsEvent event = new SmsEvent("+1234567890", "A");

            Set<ConstraintViolation<SmsEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should pass validation with maximum length message")
        void shouldPassValidationWithMaximumLengthMessage() {
            String maxLengthMessage = "A".repeat(1600);
            SmsEvent event = new SmsEvent("+1234567890", maxLengthMessage);

            Set<ConstraintViolation<SmsEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should pass validation with Unicode message")
        void shouldPassValidationWithUnicodeMessage() {
            SmsEvent event = new SmsEvent("+1234567890", "Hello 你好 مرحبا 🎉");

            Set<ConstraintViolation<SmsEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Invalid Phone Number Tests")
    class InvalidPhoneNumberTests {

        @Test
        @DisplayName("Should fail validation with null phone number")
        void shouldFailValidationWithNullPhoneNumber() {
            SmsEvent event = new SmsEvent(null, "Test message");

            Set<ConstraintViolation<SmsEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).isNotEmpty();
            Assertions.assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("to"));
        }

        @Test
        @DisplayName("Should fail validation with empty phone number")
        void shouldFailValidationWithEmptyPhoneNumber() {
            SmsEvent event = new SmsEvent("", "Test message");

            Set<ConstraintViolation<SmsEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Should fail validation with blank phone number")
        void shouldFailValidationWithBlankPhoneNumber() {
            SmsEvent event = new SmsEvent("   ", "Test message");

            Set<ConstraintViolation<SmsEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).isNotEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "1234567890",         // Missing +
                "+123456789",         // Too short (9 digits, need 10-15)
                "+1234567890123456",  // Too long (16 digits, max is 15)
                "+0123456789",        // Starts with 0 after +
                "++1234567890",       // Double +
                "+12-3456-7890",      // Contains dashes
                "+12 3456 7890",      // Contains spaces
                "+12.3456.7890",      // Contains dots
                "+abcdefghij",        // Contains letters
                "phone:+1234567890"   // Contains prefix
        })
        @DisplayName("Should fail validation with invalid phone number formats")
        void shouldFailValidationWithInvalidPhoneNumberFormats(String phoneNumber) {
            SmsEvent event = new SmsEvent(phoneNumber, "Test message");

            Set<ConstraintViolation<SmsEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Invalid Message Tests")
    class InvalidMessageTests {

        @Test
        @DisplayName("Should fail validation with null message")
        void shouldFailValidationWithNullMessage() {
            SmsEvent event = new SmsEvent("+1234567890", null);

            Set<ConstraintViolation<SmsEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).isNotEmpty();
            Assertions.assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("message"));
        }

        @Test
        @DisplayName("Should fail validation with empty message")
        void shouldFailValidationWithEmptyMessage() {
            SmsEvent event = new SmsEvent("+1234567890", "");

            Set<ConstraintViolation<SmsEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Should fail validation with blank message")
        void shouldFailValidationWithBlankMessage() {
            SmsEvent event = new SmsEvent("+1234567890", "   ");

            Set<ConstraintViolation<SmsEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Should fail validation with message exceeding 1600 characters")
        void shouldFailValidationWithMessageExceeding1600Characters() {
            String tooLongMessage = "A".repeat(1601);
            SmsEvent event = new SmsEvent("+1234567890", tooLongMessage);

            Set<ConstraintViolation<SmsEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).hasSize(1);
            Assertions.assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("message");
        }
    }

    @Nested
    @DisplayName("Multiple Violations Tests")
    class MultipleViolationsTests {

        @Test
        @DisplayName("Should report multiple violations for invalid phone and message")
        void shouldReportMultipleViolationsForInvalidPhoneAndMessage() {
            SmsEvent event = new SmsEvent("invalid", "");

            Set<ConstraintViolation<SmsEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Should report all violations when both fields are null")
        void shouldReportAllViolationsWhenBothFieldsAreNull() {
            SmsEvent event = new SmsEvent(null, null);

            Set<ConstraintViolation<SmsEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).hasSizeGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Record Immutability Tests")
    class RecordImmutabilityTests {

        @Test
        @DisplayName("Should maintain immutability of SmsEvent record")
        void shouldMaintainImmutabilityOfSmsEventRecord() {
            SmsEvent event = new SmsEvent("+1234567890", "Test message");

            Assertions.assertThat(event.to()).isEqualTo("+1234567890");
            Assertions.assertThat(event.message()).isEqualTo("Test message");
        }

        @Test
        @DisplayName("Should support equals and hashCode from record")
        void shouldSupportEqualsAndHashCodeFromRecord() {
            SmsEvent event1 = new SmsEvent("+1234567890", "Test");
            SmsEvent event2 = new SmsEvent("+1234567890", "Test");
            SmsEvent event3 = new SmsEvent("+9876543210", "Test");

            Assertions.assertThat(event1).isEqualTo(event2);
            Assertions.assertThat(event1).isNotEqualTo(event3);
            Assertions.assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
        }
    }
}
