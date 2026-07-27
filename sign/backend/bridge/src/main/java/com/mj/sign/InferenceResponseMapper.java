package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.TranslationResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InferenceResponseMapper {

    private final BridgeMetricsService metricsService;

    public InferenceResponseMapper(BridgeMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    public TranslationResult map(
            String requestedSessionId,
            GpuInferenceResponse response,
            InferenceContext context
    ) {
        if (response == null) {
            return errorResult(requestedSessionId, "Model protocol error: empty response.");
        }

        List<String> validationErrors = ModelProtocolValidator.validateResponse(response, context);
        if (!validationErrors.isEmpty()) {
            metricsService.incrementModelProtocolErrors();
            return errorResult(requestedSessionId, "Model protocol error: " + String.join("; ", validationErrors));
        }

        String sessionId = isBlank(response.session_id()) ? requestedSessionId : response.session_id();
        if (!isBlank(response.error())) {
            return errorResult(sessionId, response.error());
        }

        return TranslationResult.newBuilder()
                .setSessionId(sessionId)
                .setText(response.text() == null ? "" : response.text())
                .setIsFinal(response.is_final() == null || response.is_final())
                .setConfidence((float) ModelProtocolValidator.normalizedConfidence(response.confidence()))
                .build();
    }

    public TranslationResult errorResult(String sessionId, String message) {
        return TranslationResult.newBuilder()
                .setSessionId(sessionId)
                .setText(message)
                .setIsFinal(true)
                .setConfidence(0.0f)
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
