package com.mj.sign;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "sign.gpu", name = "queue-transport", havingValue = "rabbitmq")
public class RabbitMqQueueInferenceTransport extends AbstractBrokerQueueInferenceTransport {

    private final RabbitMqBrokerPort rabbitMqBrokerPort;

    public RabbitMqQueueInferenceTransport(
            GpuServingProperties properties,
            QueueReplyStore replyStore,
            RabbitMqBrokerPort rabbitMqBrokerPort
    ) {
        super(properties, replyStore);
        this.rabbitMqBrokerPort = rabbitMqBrokerPort;
    }

    @Override
    public QueueTransportKind kind() {
        return QueueTransportKind.RABBITMQ;
    }

    @Override
    protected void publish(QueueBrokerMessage message) {
        rabbitMqBrokerPort.publish(message);
    }
}
