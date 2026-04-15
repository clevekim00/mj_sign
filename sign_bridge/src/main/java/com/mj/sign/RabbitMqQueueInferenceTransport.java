package com.mj.sign;

import org.springframework.stereotype.Service;

@Service
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
