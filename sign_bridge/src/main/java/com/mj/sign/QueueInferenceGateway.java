package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class QueueInferenceGateway implements InferenceGateway {

    private static final String CLIENT_SCHEMA_VERSION = "v1";

    private final QueueInferenceTransport queueInferenceTransport;
    private final GpuServingProperties properties;

    public QueueInferenceGateway(
            QueueInferenceTransport queueInferenceTransport,
            GpuServingProperties properties
    ) {
        this.queueInferenceTransport = queueInferenceTransport;
        this.properties = properties;
    }

    @Override
    public InferenceProvider provider() {
        return InferenceProvider.QUEUE;
    }

    @Override
    public TranslationResult sendForInference(ClientStreamChunk chunk) {
        QueueInferenceTask task = new QueueInferenceTask(
                UUID.randomUUID().toString(),
                chunk.getSessionId(),
                properties.getQueueRequestTopic(),
                new GpuInferenceRequest(
                        chunk.getSessionId(),
                        Base64.getEncoder().encodeToString(chunk.toByteArray()),
                        chunk.getFramesCount(),
                        "protobuf-b64",
                        CLIENT_SCHEMA_VERSION
                ),
                Instant.now()
        );

        QueueInferenceResult result = queueInferenceTransport.submitAndAwait(
                task,
                Duration.ofMillis(properties.getQueueTimeoutMs())
        );

        return toTranslationResult(result.response(), chunk.getSessionId());
    }

    private TranslationResult toTranslationResult(GpuInferenceResponse response, String sessionIdFallback) {
        String sessionId = response.session_id() == null || response.session_id().isBlank()
                ? sessionIdFallback
                : response.session_id();

        if (response.error() != null && !response.error().isBlank()) {
            return TranslationResult.newBuilder()
                    .setSessionId(sessionId)
                    .setText(response.error())
                    .setIsFinal(true)
                    .setConfidence(0.0f)
                    .build();
        }

        return TranslationResult.newBuilder()
                .setSessionId(sessionId)
                .setText(response.text() == null ? "" : response.text())
                .setIsFinal(response.is_final() == null || response.is_final())
                .setConfidence(response.confidence() == null ? 0.0f : response.confidence().floatValue())
                .build();
    }
}
