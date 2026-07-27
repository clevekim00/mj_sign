package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final InferenceResponseMapper responseMapper;

    @Autowired
    public QueueInferenceGateway(
            QueueInferenceTransport queueInferenceTransport,
            GpuServingProperties properties,
            InferenceResponseMapper responseMapper
    ) {
        this.queueInferenceTransport = queueInferenceTransport;
        this.properties = properties;
        this.responseMapper = responseMapper;
    }

    QueueInferenceGateway(
            QueueInferenceTransport queueInferenceTransport,
            GpuServingProperties properties
    ) {
        this(
                queueInferenceTransport,
                properties,
                new InferenceResponseMapper(new BridgeMetricsService())
        );
    }

    @Override
    public InferenceProvider provider() {
        return InferenceProvider.QUEUE;
    }

    @Override
    public TranslationResult sendForInference(ClientStreamChunk chunk) {
        return sendForInference(chunk, InferenceContext.defaults());
    }

    @Override
    public TranslationResult sendForInference(ClientStreamChunk chunk, InferenceContext context) {
        QueueInferenceTask task = new QueueInferenceTask(
                UUID.randomUUID().toString(),
                chunk.getSessionId(),
                properties.getQueueRequestTopic(),
                new GpuInferenceRequest(
                        chunk.getSessionId(),
                        Base64.getEncoder().encodeToString(chunk.toByteArray()),
                        chunk.getFramesCount(),
                        "protobuf-b64",
                        CLIENT_SCHEMA_VERSION,
                        context.protocol_version(),
                        context.locale(),
                        context.sign_language(),
                        context.model_profile()
                ),
                Instant.now()
        );

        QueueInferenceResult result = queueInferenceTransport.submitAndAwait(
                task,
                Duration.ofMillis(properties.getQueueTimeoutMs())
        );

        return responseMapper.map(chunk.getSessionId(), result.response(), context);
    }
}
