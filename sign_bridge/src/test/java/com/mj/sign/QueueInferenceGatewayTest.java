package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueueInferenceGatewayTest {

    @Test
    void wrapsChunkIntoQueueTaskAndMapsWorkerResponse() {
        QueueInferenceGateway gateway = new QueueInferenceGateway(
                new QueueInferenceTransport() {
                    @Override
                    public QueueTransportKind kind() {
                        return QueueTransportKind.IN_MEMORY;
                    }

                    @Override
                    public QueueInferenceResult submitAndAwait(QueueInferenceTask task, Duration timeout) {
                        assertEquals("sign.inference.requests", task.topic());
                        assertEquals(Duration.ofMillis(4500), timeout);
                        return new QueueInferenceResult(
                                task.requestId(),
                                task.sessionId(),
                                new GpuInferenceResponse(task.sessionId(), "queued-result", true, 0.88, 120, "worker-v1", null),
                                task.submittedAt()
                        );
                    }
                },
                properties()
        );

        TranslationResult result = gateway.sendForInference(
                ClientStreamChunk.newBuilder().setSessionId("queue-session").build()
        );

        assertEquals("queue-session", result.getSessionId());
        assertEquals("queued-result", result.getText());
        assertTrue(result.getIsFinal());
        assertEquals(0.88f, result.getConfidence());
    }

    @Test
    void returnsQueueErrorAsTranslationError() {
        QueueInferenceGateway gateway = new QueueInferenceGateway(
                new QueueInferenceTransport() {
                    @Override
                    public QueueTransportKind kind() {
                        return QueueTransportKind.IN_MEMORY;
                    }

                    @Override
                    public QueueInferenceResult submitAndAwait(QueueInferenceTask task, Duration timeout) {
                        return new QueueInferenceResult(
                                task.requestId(),
                                task.sessionId(),
                                new GpuInferenceResponse(task.sessionId(), null, true, 0, 0, null, "Queue transport timed out or failed."),
                                task.submittedAt()
                        );
                    }
                },
                properties()
        );

        TranslationResult result = gateway.sendForInference(
                ClientStreamChunk.newBuilder().setSessionId("queue-error").build()
        );

        assertEquals("Queue transport timed out or failed.", result.getText());
    }

    private GpuServingProperties properties() {
        GpuServingProperties properties = new GpuServingProperties();
        properties.setQueueTopic("sign.inference.requests");
        properties.setQueueRequestTopic("sign.inference.requests");
        properties.setQueueResultTopic("sign.inference.results");
        properties.setQueueTimeoutMs(4500);
        return properties;
    }
}
