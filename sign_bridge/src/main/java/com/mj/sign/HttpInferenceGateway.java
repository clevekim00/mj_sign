package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
import org.springframework.stereotype.Service;
import java.util.Base64;

@Service
public class HttpInferenceGateway implements InferenceGateway {

    private static final String CLIENT_SCHEMA_VERSION = "v1";

    private final GpuServingClient gpuServingClient;
    private final GpuServingProperties properties;

    public HttpInferenceGateway(
            GpuServingClient gpuServingClient,
            GpuServingProperties properties
    ) {
        this.gpuServingClient = gpuServingClient;
        this.properties = properties;
    }

    @Override
    public InferenceProvider provider() {
        return InferenceProvider.HTTP;
    }

    @Override
    public TranslationResult sendForInference(ClientStreamChunk chunk) {
        try {
            return toTranslationResult(chunk.getSessionId(), gpuServingClient.infer(toRequest(chunk)));
        } catch (Exception error) {
            return errorResult(chunk.getSessionId(), "Failed to connect to GPU server.");
        }
    }

    GpuInferenceRequest toRequest(ClientStreamChunk chunk) {
        return new GpuInferenceRequest(
                chunk.getSessionId(),
                Base64.getEncoder().encodeToString(chunk.toByteArray()),
                chunk.getFramesCount(),
                "protobuf-b64",
                CLIENT_SCHEMA_VERSION
        );
    }

    TranslationResult toTranslationResult(String requestedSessionId, GpuInferenceResponse response) {
        String sessionId = isBlank(response.session_id()) ? requestedSessionId : response.session_id();
        if (!isBlank(response.error())) {
            return errorResult(sessionId, response.error());
        }

        return TranslationResult.newBuilder()
                .setSessionId(sessionId)
                .setText(nullToEmpty(response.text()))
                .setIsFinal(response.is_final() == null || response.is_final())
                .setConfidence(response.confidence() == null ? 0.0f : response.confidence().floatValue())
                .build();
    }

    private TranslationResult errorResult(String sessionId, String message) {
        return TranslationResult.newBuilder()
                .setSessionId(sessionId)
                .setText(message)
                .setIsFinal(true)
                .setConfidence(0.0f)
                .build();
    }

    static String joinUrl(String baseUrl, String path) {
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
