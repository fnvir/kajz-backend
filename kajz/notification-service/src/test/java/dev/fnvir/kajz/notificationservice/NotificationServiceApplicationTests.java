package dev.fnvir.kajz.notificationservice;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import dev.fnvir.kajz.notificationservice.config.EmailProperties;
import dev.fnvir.kajz.notificationservice.config.TwilioConfig;
import dev.fnvir.kajz.notificationservice.controller.NotificationPublishController;
import dev.fnvir.kajz.notificationservice.service.email.EmailEventListener;
import dev.fnvir.kajz.notificationservice.service.email.EmailSenderService;
import dev.fnvir.kajz.notificationservice.service.event.NotificationEventProducer;
import dev.fnvir.kajz.notificationservice.service.sms.SmsEventListener;
import dev.fnvir.kajz.notificationservice.service.sms.SmsSenderService;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "ENABLE_TC", matches = "true")
@DisplayName("Notification Service Application Tests")
class NotificationServiceApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
        Assertions.assertThat(applicationContext).isNotNull();
    }

    @Nested
    @DisplayName("Bean Injection Tests")
    class BeanInjectionTests {

        @Autowired
        private NotificationPublishController notificationPublishController;

        @Autowired
        private NotificationEventProducer notificationEventProducer;

        @Autowired
        private EmailSenderService emailSenderService;

        @Autowired
        private SmsSenderService smsSenderService;

        @Autowired
        private EmailEventListener emailEventListener;

        @Autowired
        private SmsEventListener smsEventListener;

        @Autowired
        private JavaMailSender javaMailSender;

        @Autowired
        private EmailProperties emailProperties;

        @Autowired
        private TwilioConfig twilioConfig;

        @Test
        @DisplayName("Controller beans are properly configured")
        void controllerBeansAreProperlyConfigured() {
            Assertions.assertThat(notificationPublishController).isNotNull();
        }

        @Test
        @DisplayName("Service beans are properly configured")
        void serviceBeansAreProperlyConfigured() {
            Assertions.assertThat(notificationEventProducer).isNotNull();
            Assertions.assertThat(emailSenderService).isNotNull();
            Assertions.assertThat(smsSenderService).isNotNull();
        }

        @Test
        @DisplayName("Kafka event listener beans are properly configured")
        void kafkaEventListenerBeansAreProperlyConfigured() {
            Assertions.assertThat(emailEventListener).isNotNull();
            Assertions.assertThat(smsEventListener).isNotNull();
        }

        @Test
        @DisplayName("Mail sender is properly configured")
        void mailSenderIsProperlyConfigured() {
            Assertions.assertThat(javaMailSender).isNotNull();
        }

        @Test
        @DisplayName("Configuration properties are properly loaded")
        void configurationPropertiesAreProperlyLoaded() {
            Assertions.assertThat(emailProperties).isNotNull();
            Assertions.assertThat(twilioConfig).isNotNull();
        }
    }

    @Nested
    @DisplayName("Configuration Validation Tests")
    class ConfigurationValidationTests {

        @Autowired
        private EmailProperties emailProperties;

        @Autowired
        private TwilioConfig twilioConfig;

        @Test
        @DisplayName("Email properties have required fields")
        void emailPropertiesHaveRequiredFields() {
            Assertions.assertThat(emailProperties.getUsername()).isNotNull();
            Assertions.assertThat(emailProperties.getProvider()).isNotNull();
        }

        @Test
        @DisplayName("Twilio config is loaded (disabled in test)")
        void twilioConfigIsLoaded() {
            Assertions.assertThat(twilioConfig).isNotNull();
            // Twilio is disabled in test profile
            Assertions.assertThat(twilioConfig.isEnabled()).isFalse();
        }
    }

}
