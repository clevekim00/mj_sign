package com.mj.sign;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@ConditionalOnProperty(prefix = "sign.gpu", name = "queue-broker-mode", havingValue = "spring")
public class SpringRabbitMqBrokerAdapter implements RabbitMqBrokerPort {

    private final RabbitTemplate rabbitTemplate;
    private final GpuServingProperties properties;
    private final QueueReplyStore replyStore;

    public SpringRabbitMqBrokerAdapter(
            RabbitTemplate rabbitTemplate,
            GpuServingProperties properties,
            QueueReplyStore replyStore
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.replyStore = replyStore;
    }

    @Override
    public void publish(QueueBrokerMessage message) {
        rabbitTemplate.convertAndSend(
                properties.getQueueExchange(),
                properties.getQueueRoutingKey(),
                message
        );
    }

    @RabbitListener(queues = "${sign.gpu.queue-result-topic}")
    public void onResult(QueueBrokerReplyMessage reply) {
        replyStore.complete(new QueueInferenceResult(
                reply.requestId(),
                reply.sessionId(),
                reply.response(),
                Instant.now()
        ));
    }
}
