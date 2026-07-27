package com.mj.sign;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrokerQueueInferenceTransportTest {

    @Test
    void kafkaTransportPublishesBrokerMessageAndAwaitsReply() {
        InMemoryQueueReplyStore replyStore = new InMemoryQueueReplyStore();
        GpuServingProperties properties = properties();
        AtomicReference<QueueBrokerMessage> published = new AtomicReference<>();

        KafkaQueueInferenceTransport transport = new KafkaQueueInferenceTransport(
                properties,
                replyStore,
                message -> {
                    published.set(message);
                    replyStore.complete(new QueueInferenceResult(
                            message.requestId(),
                            message.sessionId(),
                            new GpuInferenceResponse(message.sessionId(), "kafka-ok", true, 0.91, 10, null, null),
                            Instant.now()
                    ));
                }
        );

        QueueInferenceResult result = transport.submitAndAwait(task(), Duration.ofSeconds(1));

        assertEquals("kafka-ok", result.response().text());
        assertEquals("sign.inference.requests", published.get().requestTopic());
        assertEquals("sign.inference.results", published.get().resultTopic());
        assertEquals("sign-bridge", published.get().consumerGroup());
    }

    @Test
    void rabbitTransportPublishesBrokerMessageAndAwaitsReply() {
        InMemoryQueueReplyStore replyStore = new InMemoryQueueReplyStore();
        GpuServingProperties properties = properties();
        AtomicReference<QueueBrokerMessage> published = new AtomicReference<>();

        RabbitMqQueueInferenceTransport transport = new RabbitMqQueueInferenceTransport(
                properties,
                replyStore,
                message -> {
                    published.set(message);
                    replyStore.complete(new QueueInferenceResult(
                            message.requestId(),
                            message.sessionId(),
                            new GpuInferenceResponse(message.sessionId(), "rabbit-ok", true, 0.87, 12, null, null),
                            Instant.now()
                    ));
                }
        );

        QueueInferenceResult result = transport.submitAndAwait(task(), Duration.ofSeconds(1));

        assertEquals("rabbit-ok", result.response().text());
        assertEquals("sign.inference.request", published.get().routingKey());
    }

    @Test
    void brokerTransportReturnsTimeoutErrorWhenReplyDoesNotArrive() {
        InMemoryQueueReplyStore replyStore = new InMemoryQueueReplyStore();
        KafkaQueueInferenceTransport transport = new KafkaQueueInferenceTransport(
                properties(),
                replyStore,
                message -> {
                }
        );

        QueueInferenceResult result = transport.submitAndAwait(task(), Duration.ofMillis(5));

        assertTrue(result.response().error().contains("timed out"));
    }

    private QueueInferenceTask task() {
        return new QueueInferenceTask(
                "req-1",
                "session-1",
                "sign.inference.requests",
                new GpuInferenceRequest("session-1", "payload", 1, "protobuf-b64", "v1"),
                Instant.parse("2026-04-15T00:00:00Z")
        );
    }

    private GpuServingProperties properties() {
        GpuServingProperties properties = new GpuServingProperties();
        properties.setQueueTransport("kafka");
        properties.setQueueRequestTopic("sign.inference.requests");
        properties.setQueueResultTopic("sign.inference.results");
        properties.setQueueConsumerGroup("sign-bridge");
        properties.setQueueExchange("sign.inference");
        properties.setQueueRoutingKey("sign.inference.request");
        return properties;
    }
}
