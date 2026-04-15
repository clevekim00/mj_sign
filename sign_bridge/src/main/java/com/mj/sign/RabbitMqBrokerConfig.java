package com.mj.sign;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "sign.gpu", name = "queue-broker-mode", havingValue = "spring")
public class RabbitMqBrokerConfig {

    @Bean
    public DirectExchange queueExchange(GpuServingProperties properties) {
        return new DirectExchange(properties.getQueueExchange(), true, false);
    }

    @Bean
    public Queue queueRequestQueue(GpuServingProperties properties) {
        return new Queue(properties.getQueueRequestTopic(), true);
    }

    @Bean
    public Queue queueResultQueue(GpuServingProperties properties) {
        return new Queue(properties.getQueueResultTopic(), true);
    }

    @Bean
    public Binding queueRequestBinding(
            Queue queueRequestQueue,
            DirectExchange queueExchange,
            GpuServingProperties properties
    ) {
        return BindingBuilder.bind(queueRequestQueue)
                .to(queueExchange)
                .with(properties.getQueueRoutingKey());
    }

    @Bean
    public Binding queueResultBinding(
            Queue queueResultQueue,
            DirectExchange queueExchange,
            GpuServingProperties properties
    ) {
        return BindingBuilder.bind(queueResultQueue)
                .to(queueExchange)
                .with(properties.getQueueResultRoutingKey());
    }
}
