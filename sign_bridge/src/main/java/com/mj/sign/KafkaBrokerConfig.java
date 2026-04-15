package com.mj.sign;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(prefix = "sign.gpu", name = "queue-broker-mode", havingValue = "spring")
public class KafkaBrokerConfig {

    @Bean
    public NewTopic queueRequestTopic(GpuServingProperties properties) {
        return TopicBuilder.name(properties.getQueueRequestTopic()).build();
    }

    @Bean
    public NewTopic queueResultTopic(GpuServingProperties properties) {
        return TopicBuilder.name(properties.getQueueResultTopic()).build();
    }
}
