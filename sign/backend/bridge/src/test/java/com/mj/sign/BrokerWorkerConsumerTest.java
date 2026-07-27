package com.mj.sign;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrokerWorkerConsumerTest {

    @Test
    void kafkaWorkerConsumerPublishesReplyMessage() {
        CapturingKafkaTemplate kafkaTemplate = new CapturingKafkaTemplate();
        KafkaQueueWorkerConsumer consumer = new KafkaQueueWorkerConsumer(
                task -> new QueueInferenceResult(
                        task.requestId(),
                        task.sessionId(),
                        new GpuInferenceResponse(task.sessionId(), "worker-result", true, 0.9, 11, null, null),
                        task.submittedAt()
                ),
                kafkaTemplate,
                properties()
        );

        consumer.onRequest(message());

        assertEquals("sign.inference.results", kafkaTemplate.topic);
        assertEquals("req-1", kafkaTemplate.key);
        assertEquals("worker-result", kafkaTemplate.message.response().text());
    }

    @Test
    void rabbitWorkerConsumerPublishesReplyMessage() {
        CapturingRabbitTemplate rabbitTemplate = new CapturingRabbitTemplate();
        RabbitMqQueueWorkerConsumer consumer = new RabbitMqQueueWorkerConsumer(
                task -> new QueueInferenceResult(
                        task.requestId(),
                        task.sessionId(),
                        new GpuInferenceResponse(task.sessionId(), "rabbit-worker-result", true, 0.8, 10, null, null),
                        task.submittedAt()
                ),
                rabbitTemplate,
                properties()
        );

        consumer.onRequest(message());

        assertEquals("sign.inference", rabbitTemplate.exchange);
        assertEquals("sign.inference.result", rabbitTemplate.routingKey);
        assertEquals("rabbit-worker-result", ((QueueBrokerReplyMessage) rabbitTemplate.payload).response().text());
    }

    private QueueBrokerMessage message() {
        return new QueueBrokerMessage(
                "req-1",
                "session-1",
                "sign.inference.requests",
                "sign.inference.results",
                "sign-bridge",
                "sign.inference.request",
                java.util.Map.of(),
                new GpuInferenceRequest("session-1", "payload", 1, "protobuf-b64", "v1"),
                java.time.Instant.parse("2026-04-15T00:00:00Z")
        );
    }

    private GpuServingProperties properties() {
        GpuServingProperties properties = new GpuServingProperties();
        properties.setQueueExchange("sign.inference");
        properties.setQueueResultTopic("sign.inference.results");
        properties.setQueueResultRoutingKey("sign.inference.result");
        return properties;
    }

    private static final class CapturingKafkaTemplate extends KafkaTemplate<String, QueueBrokerReplyMessage> {
        private String topic;
        private String key;
        private QueueBrokerReplyMessage message;

        private CapturingKafkaTemplate() {
            super(new org.springframework.kafka.core.DefaultKafkaProducerFactory<>(java.util.Map.of()));
        }

        @Override
        public java.util.concurrent.CompletableFuture<org.springframework.kafka.support.SendResult<String, QueueBrokerReplyMessage>> send(
                String topic,
                String key,
                QueueBrokerReplyMessage data
        ) {
            this.topic = topic;
            this.key = key;
            this.message = data;
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
    }

    private static final class CapturingRabbitTemplate extends RabbitTemplate {
        private String exchange;
        private String routingKey;
        private Object payload;

        @Override
        public void convertAndSend(String exchange, String routingKey, Object object) {
            this.exchange = exchange;
            this.routingKey = routingKey;
            this.payload = object;
        }
    }
}
