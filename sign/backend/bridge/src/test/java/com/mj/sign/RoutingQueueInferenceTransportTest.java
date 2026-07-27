package com.mj.sign;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoutingQueueInferenceTransportTest {

    @Test
    void routesToConfiguredInMemoryTransport() {
        RoutingQueueInferenceTransport transport = new RoutingQueueInferenceTransport(
                properties("in-memory"),
                List.of(
                        fixedTransport(QueueTransportKind.IN_MEMORY, "in-memory"),
                        fixedTransport(QueueTransportKind.KAFKA, "kafka"),
                        fixedTransport(QueueTransportKind.RABBITMQ, "rabbitmq")
                )
        );

        QueueInferenceResult result = transport.submitAndAwait(task(), Duration.ofSeconds(1));

        assertEquals("in-memory", result.response().text());
        assertEquals(QueueTransportKind.IN_MEMORY, transport.kind());
    }

    @Test
    void routesToConfiguredKafkaTransport() {
        RoutingQueueInferenceTransport transport = new RoutingQueueInferenceTransport(
                properties("kafka"),
                List.of(
                        fixedTransport(QueueTransportKind.IN_MEMORY, "in-memory"),
                        fixedTransport(QueueTransportKind.KAFKA, "kafka")
                )
        );

        QueueInferenceResult result = transport.submitAndAwait(task(), Duration.ofSeconds(1));

        assertEquals("kafka", result.response().text());
    }

    @Test
    void failsFastForUnsupportedQueueTransport() {
        RoutingQueueInferenceTransport transport = new RoutingQueueInferenceTransport(
                properties("unsupported"),
                List.of(fixedTransport(QueueTransportKind.IN_MEMORY, "in-memory"))
        );

        assertThrows(IllegalArgumentException.class, transport::kind);
    }

    private QueueInferenceTask task() {
        return new QueueInferenceTask(
                "req-1",
                "session-1",
                "sign.inference.requests",
                new GpuInferenceRequest("session-1", "abc", 1, "protobuf-b64", "v1"),
                Instant.parse("2026-04-15T00:00:00Z")
        );
    }

    private GpuServingProperties properties(String queueTransport) {
        GpuServingProperties properties = new GpuServingProperties();
        properties.setQueueTransport(queueTransport);
        return properties;
    }

    private QueueInferenceTransport fixedTransport(QueueTransportKind kind, String text) {
        return new QueueInferenceTransport() {
            @Override
            public QueueTransportKind kind() {
                return kind;
            }

            @Override
            public QueueInferenceResult submitAndAwait(QueueInferenceTask task, Duration timeout) {
                return new QueueInferenceResult(
                        task.requestId(),
                        task.sessionId(),
                        new GpuInferenceResponse(task.sessionId(), text, true, 1.0, 0, null, null),
                        task.submittedAt()
                );
            }
        };
    }
}
