package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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
    private final Set<String> sessionsInFlight = ConcurrentHashMap.newKeySet();

    public AsyncInferenceService(
            InferenceGateway inferenceGateway,
            @Qualifier("inferenceExecutor") Executor inferenceExecutor,
            BridgeMetricsService metricsService
    ) {
        this.inferenceGateway = inferenceGateway;
        this.inferenceExecutor = inferenceExecutor;
        this.metricsService = metricsService;
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
                .exceptionally(error -> TranslationResult.newBuilder()
                        .setSessionId(sessionId)
                        .setText("Async inference failed.")
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
}
