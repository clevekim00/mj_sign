package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Base64;

@Service
public class HttpInferenceGateway implements InferenceGateway {

    private static final String CLIENT_SCHEMA_VERSION = "v1";

    private final GpuServingClient gpuServingClient;
    private final GpuServingProperties properties;
    private final BridgeMetricsService metricsService;
    private final InferenceResponseMapper responseMapper;

    @Autowired
    public HttpInferenceGateway(
            GpuServingClient gpuServingClient,
            GpuServingProperties properties,
            BridgeMetricsService metricsService,
            InferenceResponseMapper responseMapper
    ) {
        this.gpuServingClient = gpuServingClient;
        this.properties = properties;
        this.metricsService = metricsService;
        this.responseMapper = responseMapper;
    }

    HttpInferenceGateway(
            GpuServingClient gpuServingClient,
            GpuServingProperties properties
    ) {
        this(gpuServingClient, properties, new BridgeMetricsService());
    }

    HttpInferenceGateway(
            GpuServingClient gpuServingClient,
            GpuServingProperties properties,
            BridgeMetricsService metricsService
    ) {
        this(gpuServingClient, properties, metricsService, new InferenceResponseMapper(metricsService));
    }

    @Override
    public InferenceProvider provider() {
        return InferenceProvider.HTTP;
    }

    @Override
    public TranslationResult sendForInference(ClientStreamChunk chunk) {
        return sendForInference(chunk, InferenceContext.defaults());
    }

    @Override
    public TranslationResult sendForInference(ClientStreamChunk chunk, InferenceContext context) {
        try {
            return toTranslationResult(chunk.getSessionId(), gpuServingClient.infer(toRequest(chunk, context)), context);
        } catch (Exception error) {
            return responseMapper.errorResult(chunk.getSessionId(), "Failed to connect to GPU server.");
        }
    }

    GpuInferenceRequest toRequest(ClientStreamChunk chunk) {
        return toRequest(chunk, InferenceContext.defaults());
    }

    GpuInferenceRequest toRequest(ClientStreamChunk chunk, InferenceContext context) {
        return new GpuInferenceRequest(
                chunk.getSessionId(),
                Base64.getEncoder().encodeToString(chunk.toByteArray()),
                chunk.getFramesCount(),
                "protobuf-b64",
                CLIENT_SCHEMA_VERSION,
                context.protocol_version(),
                context.locale(),
                context.sign_language(),
                context.model_profile()
        );
    }

    TranslationResult toTranslationResult(String requestedSessionId, GpuInferenceResponse response) {
        return toTranslationResult(requestedSessionId, response, InferenceContext.defaults());
    }

    TranslationResult toTranslationResult(
            String requestedSessionId,
            GpuInferenceResponse response,
            InferenceContext context
    ) {
        return responseMapper.map(requestedSessionId, response, context);
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

}
