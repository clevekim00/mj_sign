package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
import com.mj.sign.service.SignTranslationService; // Import Kotlin service
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
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
    private final Set<String> sessionsInFlight = ConcurrentHashMap.newKeySet();

    public AsyncInferenceService(
            InferenceGateway inferenceGateway,
            @Qualifier("inferenceExecutor") Executor inferenceExecutor,
            BridgeMetricsService metricsService,
            SignTranslationService translationService // Inject Kotlin service
    ) {
        this.inferenceGateway = inferenceGateway;
        this.inferenceExecutor = inferenceExecutor;
        this.metricsService = metricsService;
        this.translationService = translationService;
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
        if (rawResult.getText() == null || rawResult.getText().isEmpty()) {
            return rawResult;
        }

        // 공백으로 구분된 키워드들을 리스트로 변환
        List<String> keywords = Arrays.asList(rawResult.getText().split("\\s+"));
        
        // LLM을 통해 문장 보정 호출 (Kotlin Service 호출)
        String refinedText = translationService.translateKeywords(keywords);

        return TranslationResult.newBuilder(rawResult)
                .setText(refinedText)
                .build();
    }
}
