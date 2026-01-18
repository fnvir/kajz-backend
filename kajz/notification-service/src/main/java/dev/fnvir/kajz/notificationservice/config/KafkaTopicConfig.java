package dev.fnvir.kajz.notificationservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    
    public static final String EMAIL_TOPIC = "notification.email";
    public static final String SMS_TOPIC = "notification.sms";
    public static final String PUSH_TOPIC = "notification.push";
    
    @Bean
    NewTopic emailTopic() {
        return TopicBuilder
                .name(EMAIL_TOPIC)
                .partitions(3)
                .build();
    }
    
    @Bean
    NewTopic smsTopic() {
        return TopicBuilder
                .name(SMS_TOPIC)
                .partitions(3)
                .build();
    }
    
    @Bean
    NewTopic pushTopic() {
        return TopicBuilder
                .name(PUSH_TOPIC)
                .partitions(3)
//                .replicas(3).config("min.insync.replicas", "2")
                .build();
    }

}
