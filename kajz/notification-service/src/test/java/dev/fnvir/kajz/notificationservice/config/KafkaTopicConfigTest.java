package dev.fnvir.kajz.notificationservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("KafkaTopicConfig Unit Tests")
class KafkaTopicConfigTest {

    private KafkaTopicConfig kafkaTopicConfig;

    @BeforeEach
    void setUp() {
        kafkaTopicConfig = new KafkaTopicConfig();
    }

    @Nested
    @DisplayName("Topic Constants Tests")
    class TopicConstantsTests {

        @Test
        @DisplayName("Should have correct email topic name")
        void shouldHaveCorrectEmailTopicName() {
            assertEquals(KafkaTopicConfig.EMAIL_TOPIC, "notification.email");
        }

        @Test
        @DisplayName("Should have correct SMS topic name")
        void shouldHaveCorrectSmsTopicName() {
            assertEquals(KafkaTopicConfig.SMS_TOPIC, "notification.sms");
        }
        
        @Test
        @DisplayName("Should have correct Push notification topic name")
        void shouldHaveCorrectPushTopicName() {
            assertEquals(KafkaTopicConfig.PUSH_TOPIC, "notification.push");
        }
    }

    @Nested
    @DisplayName("Email Topic Bean Tests")
    class EmailTopicBeanTests {

        @Test
        @DisplayName("Should create email topic with correct name")
        void shouldCreateEmailTopicWithCorrectName() {
            NewTopic emailTopic = kafkaTopicConfig.emailTopic();

            assertEquals(emailTopic.name(), "notification.email");
        }

        @Test
        @DisplayName("Should create email topic with 3 partitions")
        void shouldCreateEmailTopicWith3Partitions() {
            NewTopic emailTopic = kafkaTopicConfig.emailTopic();

            assertEquals(emailTopic.numPartitions(), 3);
        }
    }

    @Nested
    @DisplayName("SMS Topic Bean Tests")
    class SmsTopicBeanTests {

        @Test
        @DisplayName("Should create SMS topic with correct name")
        void shouldCreateSmsTopicWithCorrectName() {
            NewTopic smsTopic = kafkaTopicConfig.smsTopic();

            assertEquals(smsTopic.name(), "notification.sms");
        }

        @Test
        @DisplayName("Should create SMS topic with 3 partitions")
        void shouldCreateSmsTopicWith3Partitions() {
            NewTopic smsTopic = kafkaTopicConfig.smsTopic();

            assertEquals(smsTopic.numPartitions(), 3);
        }
    }
    
    @Nested
    @DisplayName("Push Topic Bean Tests")
    class PushNotificationTopicBeanTests {
        
        @Test
        @DisplayName("Should create Push topic with correct name")
        void shouldCreatePushTopicWithCorrectName() {
            NewTopic pushTopic = kafkaTopicConfig.pushTopic();
            
            assertEquals(pushTopic.name(), "notification.push");
        }
        
        @Test
        @DisplayName("Should create Push topic with 3 partitions")
        void shouldCreatePushTopicWith3Partitions() {
            NewTopic topic = kafkaTopicConfig.pushTopic();
            
            assertEquals(topic.numPartitions(), 3);
        }
    }

    @Nested
    @DisplayName("Topic Independence Tests")
    class TopicIndependenceTests {

        @Test
        @DisplayName("All topics should have different names")
        void emailAndSmsTopicsShouldHaveDifferentNames() {
            NewTopic emailTopic = kafkaTopicConfig.emailTopic();
            NewTopic smsTopic = kafkaTopicConfig.smsTopic();
            NewTopic pushTopic = kafkaTopicConfig.pushTopic();

            List<String> allTopics = List.of(emailTopic.name(), smsTopic.name(), pushTopic.name());
            
            assertTrue(allTopics.stream().distinct().count() == allTopics.size());
        }
    }
}
