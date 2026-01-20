package dev.fnvir.kajz.notificationservice.integration;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import dev.fnvir.kajz.notificationservice.TestcontainersConfiguration;
import dev.fnvir.kajz.notificationservice.config.KafkaTopicConfig;
import dev.fnvir.kajz.notificationservice.dto.event.EmailEvent;
import dev.fnvir.kajz.notificationservice.dto.event.SmsEvent;
import dev.fnvir.kajz.notificationservice.service.NotificationEventProducer;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DirtiesContext
@EnabledIfEnvironmentVariable(named = "ENABLE_TC", matches = "true")
@DisplayName("Kafka Integration Tests")
class KafkaIntegrationTest {

    @Autowired
    private NotificationEventProducer notificationEventProducer;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaTemplate.getProducerFactory()
                .getConfigurationProperties().get("bootstrap.servers"));
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group-" + System.currentTimeMillis());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        ConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = consumerFactory.createConsumer();
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Nested
    @DisplayName("Email Event Producer Tests")
    class EmailEventProducerTests {

        @Test
        @DisplayName("Should successfully publish email event to Kafka topic")
        void shouldSuccessfullyPublishEmailEventToKafkaTopic() throws Exception {
            EmailEvent emailEvent = createValidEmailEvent();
            consumer.subscribe(Set.of(KafkaTopicConfig.EMAIL_TOPIC));

            CompletableFuture<SendResult<String, Object>> future = notificationEventProducer
                    .publishEmailNotification(emailEvent);
            SendResult<String, Object> result = future.get(10, TimeUnit.SECONDS);

            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.getRecordMetadata().topic()).isEqualTo(KafkaTopicConfig.EMAIL_TOPIC);
            Assertions.assertThat(result.getRecordMetadata().offset()).isGreaterThanOrEqualTo(0);

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));
                Assertions.assertThat(records.count()).isGreaterThan(0);
            });
        }

        @Test
        @DisplayName("Should publish email event with all fields to Kafka")
        void shouldPublishEmailEventWithAllFieldsToKafka() throws Exception {
            EmailEvent emailEvent = createFullEmailEvent();
            consumer.subscribe(Set.of(KafkaTopicConfig.EMAIL_TOPIC));

            CompletableFuture<SendResult<String, Object>> future = notificationEventProducer
                    .publishEmailNotification(emailEvent);
            SendResult<String, Object> result = future.get(10, TimeUnit.SECONDS);

            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.getRecordMetadata().topic()).isEqualTo(KafkaTopicConfig.EMAIL_TOPIC);
        }

        @Test
        @DisplayName("Should handle multiple email events concurrently")
        void shouldHandleMultipleEmailEventsConcurrently() throws Exception {
            int eventCount = 5;
            consumer.subscribe(Set.of(KafkaTopicConfig.EMAIL_TOPIC));

            CompletableFuture<?>[] futures = new CompletableFuture[eventCount];
            for (int i = 0; i < eventCount; i++) {
                EmailEvent event = createValidEmailEvent();
                event.setSubject("Test Email " + i);
                futures[i] = notificationEventProducer.publishEmailNotification(event);
            }

            CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

            for (CompletableFuture<?> future : futures) {
                Assertions.assertThat(future).isCompleted();
            }
        }
    }

    @Nested
    @DisplayName("SMS Event Producer Tests")
    class SmsEventProducerTests {

        @Test
        @DisplayName("Should successfully publish SMS event to Kafka topic")
        void shouldSuccessfullyPublishSmsEventToKafkaTopic() throws Exception {
            SmsEvent smsEvent = new SmsEvent("+1234567890", "Test SMS message");
            consumer.subscribe(Set.of(KafkaTopicConfig.SMS_TOPIC));

            CompletableFuture<SendResult<String, Object>> future = notificationEventProducer
                    .sendSmsNotification(smsEvent);
            SendResult<String, Object> result = future.get(10, TimeUnit.SECONDS);

            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.getRecordMetadata().topic()).isEqualTo(KafkaTopicConfig.SMS_TOPIC);
            Assertions.assertThat(result.getRecordMetadata().offset()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should publish SMS event with international phone number")
        void shouldPublishSmsEventWithInternationalPhoneNumber() throws Exception {
            SmsEvent smsEvent = new SmsEvent("+447911123456", "International SMS");
            consumer.subscribe(Set.of(KafkaTopicConfig.SMS_TOPIC));

            CompletableFuture<SendResult<String, Object>> future = notificationEventProducer
                    .sendSmsNotification(smsEvent);
            SendResult<String, Object> result = future.get(10, TimeUnit.SECONDS);

            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.getRecordMetadata().topic()).isEqualTo(KafkaTopicConfig.SMS_TOPIC);
        }

        @Test
        @DisplayName("Should handle multiple SMS events concurrently")
        void shouldHandleMultipleSmsEventsConcurrently() throws Exception {
            int eventCount = 5;
            consumer.subscribe(Set.of(KafkaTopicConfig.SMS_TOPIC));

            CompletableFuture<?>[] futures = new CompletableFuture[eventCount];
            for (int i = 0; i < eventCount; i++) {
                SmsEvent event = new SmsEvent("+1234567890", "Test SMS " + i);
                futures[i] = notificationEventProducer.sendSmsNotification(event);
            }

            CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

            for (CompletableFuture<?> future : futures) {
                Assertions.assertThat(future).isCompleted();
            }
        }
    }

    @Nested
    @DisplayName("Topic Configuration Tests")
    class TopicConfigurationTests {

        @Test
        @DisplayName("Should have email topic created")
        void shouldHaveEmailTopicCreated() throws Exception {
            EmailEvent emailEvent = createValidEmailEvent();

            CompletableFuture<SendResult<String, Object>> future = notificationEventProducer
                    .publishEmailNotification(emailEvent);
            SendResult<String, Object> result = future.get(10, TimeUnit.SECONDS);

            Assertions.assertThat(result.getRecordMetadata().topic()).isEqualTo("notification.email");
        }

        @Test
        @DisplayName("Should have SMS topic created")
        void shouldHaveSmstopicCreated() throws Exception {
            SmsEvent smsEvent = new SmsEvent("+1234567890", "Test SMS");

            CompletableFuture<SendResult<String, Object>> future = notificationEventProducer
                    .sendSmsNotification(smsEvent);
            SendResult<String, Object> result = future.get(10, TimeUnit.SECONDS);

            Assertions.assertThat(result.getRecordMetadata().topic()).isEqualTo("notification.sms");
        }
    }

    private EmailEvent createValidEmailEvent() {
        EmailEvent event = new EmailEvent();
        event.setTo(Set.of("test@example.com"));
        event.setSubject("Test Subject");
        event.setContent("Test Content");
        event.setHtml(true);
        event.setPriority(3);
        return event;
    }

    private EmailEvent createFullEmailEvent() {
        EmailEvent event = new EmailEvent();
        event.setTo(Set.of("test@example.com"));
        event.setCc(Set.of("cc@example.com"));
        event.setBcc(Set.of("bcc@example.com"));
        event.setSubject("Full Test Subject");
        event.setContent("<html><body>Test HTML Content</body></html>");
        event.setHtml(true);
        event.setPriority(1);
        return event;
    }
}
