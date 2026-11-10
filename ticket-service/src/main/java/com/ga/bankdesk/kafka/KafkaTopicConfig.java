package com.ga.bankdesk.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic emailEventsTopic() {
        return TopicBuilder.name(NotificationProducer.EMAIL_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic liveEventsTopic() {
        return TopicBuilder.name(NotificationProducer.LIVE_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic ticketEventsTopic() {
        return TopicBuilder.name(TicketEventProducer.TOPIC).partitions(3).replicas(1).build();
    }
}