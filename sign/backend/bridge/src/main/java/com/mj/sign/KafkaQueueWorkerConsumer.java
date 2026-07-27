package com.mj.sign;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(prefix = "sign.gpu", name = "queue-transport", havingValue = "kafka")
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

        String resultTopic = message.resultTopic() == null || message.resultTopic().isBlank()
                ? properties.getQueueResultTopic()
                : message.resultTopic();
        try {
            queueBrokerReplyKafkaTemplate.send(
                            resultTopic,
                            message.requestId(),
                            new QueueBrokerReplyMessage(message.requestId(), message.sessionId(), result.response())
                    )
                    .get(properties.getQueuePublishTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (Exception error) {
            throw new IllegalStateException("Kafka result publish failed", error);
        }
    }
}
