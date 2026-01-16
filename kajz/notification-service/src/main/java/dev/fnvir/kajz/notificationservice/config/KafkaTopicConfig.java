package dev.fnvir.kajz.notificationservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    
    public static final String EMAIL_TOPIC = "notification.email";
    
    @Bean
    NewTopic emailTopic() {
        return TopicBuilder
                .name(EMAIL_TOPIC)
                .partitions(3)
                .build();
    }

}
