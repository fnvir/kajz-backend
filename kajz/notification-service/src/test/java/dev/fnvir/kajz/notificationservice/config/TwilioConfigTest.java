package dev.fnvir.kajz.notificationservice.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TwilioConfig Unit Tests")
class TwilioConfigTest {

    private TwilioConfig twilioConfig;

    @BeforeEach
    void setUp() {
        twilioConfig = new TwilioConfig();
    }

    @Nested
    @DisplayName("Default Configuration Tests")
    class DefaultConfigurationTests {

        @Test
        @DisplayName("Should be disabled by default")
        void shouldBeDisabledByDefault() {
            assertFalse(twilioConfig.isEnabled());
        }

        @Test
        @DisplayName("Should have null account SID by default")
        void shouldHaveNullAccountSidByDefault() {
            assertNull(twilioConfig.getAccountSid());
        }

        @Test
        @DisplayName("Should have null auth token by default")
        void shouldHaveNullAuthTokenByDefault() {
            assertNull(twilioConfig.getAuthToken());
        }

        @Test
        @DisplayName("Should have null phone number by default")
        void shouldHaveNullPhoneNumberByDefault() {
            assertNull(twilioConfig.getPhoneNumber());
        }
    }

    @Nested
    @DisplayName("PostConstruct Validation Tests")
    class PostConstructValidationTests {

        @Test
        @DisplayName("Should disable when account SID is missing")
        void shouldDisableWhenAccountSidIsMissing() {
            // Given
            twilioConfig.setEnabled(true);
            twilioConfig.setAccountSid(null);
            twilioConfig.setAuthToken("token");
            twilioConfig.setPhoneNumber("+1234567890");

            twilioConfig.init();

            assertFalse(twilioConfig.isEnabled());
        }

        @Test
        @DisplayName("Should disable when auth token is missing")
        void shouldDisableWhenAuthTokenIsMissing() {
            twilioConfig.setEnabled(true);
            twilioConfig.setAccountSid("sid");
            twilioConfig.setAuthToken(null);
            twilioConfig.setPhoneNumber("+1234567890");

            twilioConfig.init();

            assertFalse(twilioConfig.isEnabled());
        }

        @Test
        @DisplayName("Should disable when phone number is missing")
        void shouldDisableWhenPhoneNumberIsMissing() {
            twilioConfig.setEnabled(true);
            twilioConfig.setAccountSid("sid");
            twilioConfig.setAuthToken("token");
            twilioConfig.setPhoneNumber(null);

            twilioConfig.init();

            assertFalse(twilioConfig.isEnabled());
        }

        @Test
        @DisplayName("Should disable when account SID is blank")
        void shouldDisableWhenAccountSidIsBlank() {
            twilioConfig.setEnabled(true);
            twilioConfig.setAccountSid("   ");
            twilioConfig.setAuthToken("token");
            twilioConfig.setPhoneNumber("+1234567890");

            twilioConfig.init();

            assertFalse(twilioConfig.isEnabled());
        }

        @Test
        @DisplayName("Should disable when auth token is blank")
        void shouldDisableWhenAuthTokenIsBlank() {
            twilioConfig.setEnabled(true);
            twilioConfig.setAccountSid("sid");
            twilioConfig.setAuthToken("");
            twilioConfig.setPhoneNumber("+1234567890");

            twilioConfig.init();

            assertFalse(twilioConfig.isEnabled());
        }

        @Test
        @DisplayName("Should remain enabled when all properties are set")
        void shouldRemainEnabledWhenAllPropertiesAreSet() {
            twilioConfig.setEnabled(true);
            twilioConfig.setAccountSid("valid-sid");
            twilioConfig.setAuthToken("valid-token");
            twilioConfig.setPhoneNumber("+1234567890");

            twilioConfig.init();

            assertTrue(twilioConfig.isEnabled());
        }

        @Test
        @DisplayName("Should stay disabled even when all properties are set but enabled is false")
        void shouldStayDisabledWhenEnabledIsFalse() {
            twilioConfig.setEnabled(false);
            twilioConfig.setAccountSid("valid-sid");
            twilioConfig.setAuthToken("valid-token");
            twilioConfig.setPhoneNumber("+1234567890");

            twilioConfig.init();

            assertFalse(twilioConfig.isEnabled());
        }
    }

}
