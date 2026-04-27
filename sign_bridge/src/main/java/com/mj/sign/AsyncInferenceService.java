package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
import com.mj.sign.service.SignTranslationService; // Import Kotlin service
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@Service
public class AsyncInferenceService {

    private final InferenceGateway inferenceGateway;
    private final Executor inferenceExecutor;
    private final BridgeMetricsService metricsService;
    private final SignTranslationService translationService; // New dependency
    private final boolean refinementEnabled;
    private final float minConfidenceForRefinement;
    private final Set<String> sessionsInFlight = ConcurrentHashMap.newKeySet();
    private static final Set<String> SYSTEM_MESSAGE_PREFIXES = new HashSet<>(Arrays.asList(
            "buffering ",
            "processing ",
            "inference already in progress",
            "idle timeout reached",
            "failed to connect to gpu server",
            "async inference failed",
            "queue transport ",
            "no inference gateway registered",
            "session_id is required",
            "failed to parse protobuf payload"
    ));

    public AsyncInferenceService(
            InferenceGateway inferenceGateway,
            @Qualifier("inferenceExecutor") Executor inferenceExecutor,
            BridgeMetricsService metricsService,
            SignTranslationService translationService, // Inject Kotlin service
            @Value("${sign.translation.refinement-enabled:true}") boolean refinementEnabled,
            @Value("${sign.translation.min-confidence:0.6}") float minConfidenceForRefinement
    ) {
        this.inferenceGateway = inferenceGateway;
        this.inferenceExecutor = inferenceExecutor;
        this.metricsService = metricsService;
        this.translationService = translationService;
        this.refinementEnabled = refinementEnabled;
        this.minConfidenceForRefinement = minConfidenceForRefinement;
    }

    AsyncInferenceService(
            InferenceGateway inferenceGateway,
            Executor inferenceExecutor,
            BridgeMetricsService metricsService,
            SignTranslationService translationService
    ) {
        this(inferenceGateway, inferenceExecutor, metricsService, translationService, true, 0.6f);
    }

    public boolean dispatch(
            String sessionId,
            ClientStreamChunk chunk,
            Consumer<TranslationResult> onComplete
    ) {
        if (!sessionsInFlight.add(sessionId)) {
            metricsService.incrementDispatchRejected();
            return false;
        }

        metricsService.incrementDispatchAccepted();
        metricsService.incrementInFlightInferences();

        CompletableFuture
                .supplyAsync(() -> inferenceGateway.sendForInference(chunk), inferenceExecutor)
                .thenApply(this::refineWithLlm) // Add LLM refinement step
                .exceptionally(error -> TranslationResult.newBuilder()
                        .setSessionId(sessionId)
                        .setText("Async inference failed: " + error.getMessage())
                        .setIsFinal(true)
                        .setConfidence(0.0f)
                        .build())
                .whenComplete((result, error) -> {
                    sessionsInFlight.remove(sessionId);
                    metricsService.decrementInFlightInferences();
                    if (result != null) {
                        metricsService.incrementInferenceCompleted();
                        onComplete.accept(result);
                    }
                });
        return true;
    }

    /**
     * GPU 인식 결과(키워드)를 LLM을 통해 자연스러운 문장으로 변환합니다.
     */
    private TranslationResult refineWithLlm(TranslationResult rawResult) {
        if (!shouldRefine(rawResult)) {
            return rawResult;
        }

        List<String> keywords = Arrays.asList(rawResult.getText().split("\\s+"));
        try {
            String refinedText = translationService.translateKeywords(keywords);
            if (refinedText == null || refinedText.isBlank()) {
                return rawResult;
            }

            return TranslationResult.newBuilder(rawResult)
                    .setText(refinedText)
                    .build();
        } catch (Exception ignored) {
            return rawResult;
        }
    }

    private boolean shouldRefine(TranslationResult rawResult) {
        if (!refinementEnabled || !rawResult.getIsFinal()) {
            return false;
        }
        if (rawResult.getText() == null || rawResult.getText().isBlank()) {
            return false;
        }
        if (rawResult.getConfidence() < minConfidenceForRefinement) {
            return false;
        }

        String normalized = rawResult.getText().trim().toLowerCase(Locale.ROOT);
        for (String prefix : SYSTEM_MESSAGE_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }
}
