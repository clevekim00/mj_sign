package com.mj.sign;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(prefix = "sign.gpu", name = "queue-transport", havingValue = "kafka")
public class SpringKafkaBrokerAdapter implements KafkaBrokerPort {

    private final KafkaTemplate<String, QueueBrokerMessage> kafkaTemplate;
    private final QueueReplyStore replyStore;
    private final GpuServingProperties properties;

    public SpringKafkaBrokerAdapter(
            KafkaTemplate<String, QueueBrokerMessage> kafkaTemplate,
            QueueReplyStore replyStore,
            GpuServingProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.replyStore = replyStore;
        this.properties = properties;
    }

    @Override
    public void publish(QueueBrokerMessage message) {
        try {
            kafkaTemplate.send(message.requestTopic(), message.requestId(), message)
                    .get(properties.getQueuePublishTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (Exception error) {
            throw new IllegalStateException("Kafka request publish failed", error);
        }
    }

    @KafkaListener(
            topics = "${sign.gpu.queue-result-topic}",
            groupId = "${sign.gpu.queue-consumer-group}",
            containerFactory = "queueBrokerReplyKafkaListenerContainerFactory"
    )
    public void onResult(QueueBrokerReplyMessage reply) {
        replyStore.complete(new QueueInferenceResult(
                reply.requestId(),
                reply.sessionId(),
                reply.response(),
                Instant.now()
        ));
    }
}
