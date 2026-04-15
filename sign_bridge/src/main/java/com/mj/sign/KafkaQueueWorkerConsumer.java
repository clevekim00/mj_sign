package com.mj.sign;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "sign.gpu", name = "queue-broker-mode", havingValue = "spring")
public class KafkaQueueWorkerConsumer {

    private final QueueWorkerBackend queueWorkerBackend;
    private final KafkaTemplate<String, QueueBrokerReplyMessage> queueBrokerReplyKafkaTemplate;
    private final GpuServingProperties properties;

    public KafkaQueueWorkerConsumer(
            QueueWorkerBackend queueWorkerBackend,
            KafkaTemplate<String, QueueBrokerReplyMessage> queueBrokerReplyKafkaTemplate,
            GpuServingProperties properties
    ) {
        this.queueWorkerBackend = queueWorkerBackend;
        this.queueBrokerReplyKafkaTemplate = queueBrokerReplyKafkaTemplate;
        this.properties = properties;
    }

    @KafkaListener(
            topics = "${sign.gpu.queue-request-topic}",
            groupId = "${sign.gpu.queue-consumer-group}",
            containerFactory = "queueBrokerRequestKafkaListenerContainerFactory"
    )
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

        queueBrokerReplyKafkaTemplate.send(
                properties.getQueueResultTopic(),
                message.requestId(),
                new QueueBrokerReplyMessage(message.requestId(), message.sessionId(), result.response())
        );
    }
}
