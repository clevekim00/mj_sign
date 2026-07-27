package com.mj.sign;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "sign.gpu", name = "queue-transport", havingValue = "kafka")
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
