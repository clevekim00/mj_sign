package com.mj.sign;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@ConditionalOnProperty(prefix = "sign.gpu", name = "queue-broker-mode", havingValue = "spring")
public class SpringKafkaBrokerAdapter implements KafkaBrokerPort {

    private final KafkaTemplate<String, QueueBrokerMessage> kafkaTemplate;
    private final QueueReplyStore replyStore;

    public SpringKafkaBrokerAdapter(
            KafkaTemplate<String, QueueBrokerMessage> kafkaTemplate,
            QueueReplyStore replyStore
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.replyStore = replyStore;
    }

    @Override
    public void publish(QueueBrokerMessage message) {
        kafkaTemplate.send(message.requestTopic(), message.requestId(), message);
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
