package com.mj.sign;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "sign.gpu", name = "queue-broker-mode", havingValue = "spring")
public class RabbitMqQueueWorkerConsumer {

    private final QueueWorkerBackend queueWorkerBackend;
    private final RabbitTemplate rabbitTemplate;
    private final GpuServingProperties properties;

    public RabbitMqQueueWorkerConsumer(
            QueueWorkerBackend queueWorkerBackend,
            RabbitTemplate rabbitTemplate,
            GpuServingProperties properties
    ) {
        this.queueWorkerBackend = queueWorkerBackend;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @RabbitListener(queues = "${sign.gpu.queue-request-topic}")
    public void onRequest(QueueBrokerMessage message) {
        QueueInferenceResult result = queueWorkerBackend.process(
                new QueueInferenceTask(
                        message.requestId(),
                        message.sessionId(),
                        message.requestTopic(),
                        message.payload(),
                        message.createdAt()
                )
        );

        rabbitTemplate.convertAndSend(
                properties.getQueueExchange(),
                properties.getQueueResultRoutingKey(),
                new QueueBrokerReplyMessage(message.requestId(), message.sessionId(), result.response())
        );
    }
}
