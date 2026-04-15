package com.mj.sign;

import org.springframework.stereotype.Service;

@Service
public class KafkaQueueInferenceTransport extends AbstractBrokerQueueInferenceTransport {

    private final KafkaBrokerPort kafkaBrokerPort;

    public KafkaQueueInferenceTransport(
            GpuServingProperties properties,
            QueueReplyStore replyStore,
            KafkaBrokerPort kafkaBrokerPort
    ) {
        super(properties, replyStore);
        this.kafkaBrokerPort = kafkaBrokerPort;
    }

    @Override
    public QueueTransportKind kind() {
        return QueueTransportKind.KAFKA;
    }

    @Override
    protected void publish(QueueBrokerMessage message) {
        kafkaBrokerPort.publish(message);
    }
}
