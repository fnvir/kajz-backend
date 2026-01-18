package dev.fnvir.kajz.notificationservice.service.sms;

import static org.mockito.Mockito.when;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.fnvir.kajz.notificationservice.config.TwilioConfig;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsSenderService Unit Tests")
class SmsSenderServiceTest {

    @Mock
    private TwilioConfig twilioConfig;

    @InjectMocks
    private SmsSenderService smsSenderService;

    @Nested
    @DisplayName("Synchronous SMS Sending Tests")
    class SyncSmsTests {

        @Test
        @DisplayName("Should throw IllegalStateException when Twilio is disabled")
        void shouldThrowIllegalStateExceptionWhenTwilioDisabled() {
            when(twilioConfig.isEnabled()).thenReturn(false);

            Assertions.assertThatThrownBy(() -> smsSenderService.sendSms("+1234567890", "Test message"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Twilio isn't enabled");
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"1234567890", "invalid", "", " "})
        @DisplayName("Should return null for invalid phone number formats")
        void shouldReturnNullForInvalidPhoneNumberFormats(String phoneNumber) {
            when(twilioConfig.isEnabled()).thenReturn(true);

            String result = smsSenderService.sendSms(phoneNumber, "Test message");

            Assertions.assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Asynchronous SMS Sending Tests")
    class AsyncSmsTests {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"1234567890", "invalid-number", ""})
        @DisplayName("Should return empty Mono for invalid phone numbers")
        void shouldReturnEmptyMonoForInvalidPhoneNumbers(String phoneNumber) {
            StepVerifier.create(smsSenderService.sendSmsAsync(phoneNumber, "Test message"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return empty Mono when phone number missing country code")
        void shouldReturnEmptyMonoWhenPhoneNumberMissingCountryCode() {
            StepVerifier.create(smsSenderService.sendSmsAsync("1234567890", "Test message"))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Bulk SMS Sending Tests")
    class BulkSmsTests {

        @Test
        @DisplayName("Should filter out invalid phone numbers in bulk send")
        void shouldFilterOutInvalidPhoneNumbersInBulkSend() {
            List<String> recipients = List.of("1234567890", "invalid", "9876543210");

            StepVerifier.create(smsSenderService.sendBulkSmsAsync(recipients, "Bulk message"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle empty recipient list in bulk send")
        void shouldHandleEmptyRecipientListInBulkSend() {
            List<String> recipients = List.of();

            StepVerifier.create(smsSenderService.sendBulkSmsAsync(recipients, "Bulk message"))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Phone Number Validation Tests")
    class PhoneNumberValidationTests {

        @Test
        @DisplayName("Should reject phone number without plus prefix")
        void shouldRejectPhoneNumberWithoutPlusPrefix() {
            when(twilioConfig.isEnabled()).thenReturn(true);

            String result = smsSenderService.sendSms("1234567890", "Test");

            Assertions.assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should reject null phone number")
        void shouldRejectNullPhoneNumber() {
            when(twilioConfig.isEnabled()).thenReturn(true);

            String result = smsSenderService.sendSms(null, "Test");

            Assertions.assertThat(result).isNull();
        }
    }
}
