package dev.fnvir.kajz.notificationservice.dto.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

@DisplayName("EmailEvent Validation Tests")
class EmailEventValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Valid EmailEvent Tests")
    class ValidEmailEventTests {

        @Test
        @DisplayName("Should pass validation with required fields only")
        void shouldPassValidationWithRequiredFieldsOnly() {
            EmailEvent event = new EmailEvent();
            event.setTo(Set.of("test@example.com"));
            event.setSubject("Test Subject");
            event.setContent("Test Content");

            Set<ConstraintViolation<EmailEvent>> violations = validator.validate(event);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should pass validation with all fields")
        void shouldPassValidationWithAllFields() {
            EmailEvent event = new EmailEvent();
            event.setTo(Set.of("to@example.com"));
            event.setCc(Set.of("cc@example.com"));
            event.setBcc(Set.of("bcc@example.com"));
            event.setSubject("Full Test Subject");
            event.setContent("<html><body>HTML Content</body></html>");
            event.setHtml(true);
            event.setPriority(1);

            Set<ConstraintViolation<EmailEvent>> violations = validator.validate(event);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should pass validation with multiple recipients")
        void shouldPassValidationWithMultipleRecipients() {
            EmailEvent event = new EmailEvent();
            event.setTo(Set.of("user1@example.com", "user2@example.com", "user3@example.com"));
            event.setSubject("Subject");
            event.setContent("Content");

            Set<ConstraintViolation<EmailEvent>> violations = validator.validate(event);

            assertTrue(violations.isEmpty());
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 4, 5})
        @DisplayName("Should accept valid priority values 1-5")
        void shouldAcceptValidPriorityValues(int priority) {
            EmailEvent event = createValidEmailEvent();
            event.setPriority(priority);

            Set<ConstraintViolation<EmailEvent>> violations = validator.validate(event);

            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Invalid EmailEvent Tests")
    class InvalidEmailEventTests {

        @Test
        @DisplayName("Should fail validation with null 'to' field")
        void shouldFailValidationWithNullToField() {
            EmailEvent event = new EmailEvent();
            event.setTo(null);
            event.setSubject("Subject");
            event.setContent("Content");

            Set<ConstraintViolation<EmailEvent>> violations = validator.validate(event);

            assertTrue(violations.size() == 1);
            assertEquals(violations.iterator().next().getPropertyPath().toString(), "to");
        }

        @Test
        @DisplayName("Should fail validation with empty 'to' set")
        void shouldFailValidationWithEmptyToSet() {
            EmailEvent event = new EmailEvent();
            event.setTo(Set.of());
            event.setSubject("Subject");
            event.setContent("Content");

            Set<ConstraintViolation<EmailEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Should fail validation with invalid email in 'to' field")
        void shouldFailValidationWithInvalidEmailInToField() {
            EmailEvent event = new EmailEvent();
            event.setTo(Set.of("invalid-email"));
            event.setSubject("Subject");
            event.setContent("Content");

            Set<ConstraintViolation<EmailEvent>> violations = validator.validate(event);

            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation with blank subject")
        void shouldFailValidationWithBlankSubject() {
            EmailEvent event = new EmailEvent();
            event.setTo(Set.of("test@example.com"));
            event.setSubject("");
            event.setContent("Content");

            Set<ConstraintViolation<EmailEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).hasSize(1);
            Assertions.assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("subject");
        }

        @Test
        @DisplayName("Should fail validation with blank content")
        void shouldFailValidationWithBlankContent() {
            EmailEvent event = new EmailEvent();
            event.setTo(Set.of("test@example.com"));
            event.setSubject("Subject");
            event.setContent("");

            Set<ConstraintViolation<EmailEvent>> violations = validator.validate(event);

            assertTrue(violations.size() == 1);
            assertEquals(violations.iterator().next().getPropertyPath().toString(), "content");
        }

        @Test
        @DisplayName("Should fail validation with subject exceeding 200 characters")
        void shouldFailValidationWithSubjectExceeding200Characters() {
            EmailEvent event = new EmailEvent();
            event.setTo(Set.of("test@example.com"));
            event.setSubject("A".repeat(201));
            event.setContent("Content");

            Set<ConstraintViolation<EmailEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).hasSize(1);
            Assertions.assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("subject");
        }

        @Test
        @DisplayName("Should fail validation with priority less than 1")
        void shouldFailValidationWithPriorityLessThan1() {
            EmailEvent event = createValidEmailEvent();
            event.setPriority(0);

            Set<ConstraintViolation<EmailEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).hasSize(1);
            Assertions.assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("priority");
        }

        @Test
        @DisplayName("Should fail validation with priority greater than 5")
        void shouldFailValidationWithPriorityGreaterThan5() {
            EmailEvent event = createValidEmailEvent();
            event.setPriority(6);

            Set<ConstraintViolation<EmailEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).hasSize(1);
            Assertions.assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("priority");
        }

        @Test
        @DisplayName("Should fail validation with invalid email in CC field")
        void shouldFailValidationWithInvalidEmailInCcField() {
            EmailEvent event = createValidEmailEvent();
            event.setCc(Set.of("not-an-email"));

            Set<ConstraintViolation<EmailEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Should fail validation with invalid email in BCC field")
        void shouldFailValidationWithInvalidEmailInBccField() {
            EmailEvent event = createValidEmailEvent();
            event.setBcc(Set.of("bad@email@format"));

            Set<ConstraintViolation<EmailEvent>> violations = validator.validate(event);

            Assertions.assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have isHtml default to true")
        void shouldHaveIsHtmlDefaultToTrue() {
            EmailEvent event = new EmailEvent();

            Assertions.assertThat(event.isHtml()).isTrue();
        }

        @Test
        @DisplayName("Should have priority default to 3")
        void shouldHavePriorityDefaultTo3() {
            EmailEvent event = new EmailEvent();

            Assertions.assertThat(event.getPriority()).isEqualTo(3);
        }
    }

    private EmailEvent createValidEmailEvent() {
        EmailEvent event = new EmailEvent();
        event.setTo(Set.of("test@example.com"));
        event.setSubject("Test Subject");
        event.setContent("Test Content");
        return event;
    }
}
