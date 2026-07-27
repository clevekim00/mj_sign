package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
import com.mj.sign.service.SignTranslationService; // Import Kotlin service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

@Service
public class AsyncInferenceService {

    private final InferenceGateway inferenceGateway;
    private final Executor inferenceExecutor;
    private final BridgeMetricsService metricsService;
    private final SignTranslationService translationService; // New dependency
    private final boolean refinementEnabled;
    private final float minConfidenceForRefinement;
    private final int pendingPerSessionCapacity;
    private final ConcurrentHashMap<String, SessionDispatchState> sessionStates = new ConcurrentHashMap<>();
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

    @Autowired
    public AsyncInferenceService(
            InferenceGateway inferenceGateway,
            @Qualifier("inferenceExecutor") Executor inferenceExecutor,
            BridgeMetricsService metricsService,
            SignTranslationService translationService, // Inject Kotlin service
            @Value("${sign.translation.refinement-enabled:true}") boolean refinementEnabled,
            @Value("${sign.translation.min-confidence:0.6}") float minConfidenceForRefinement,
            @Value("${sign.async.pending-per-session-capacity:4}") int pendingPerSessionCapacity
    ) {
        this.inferenceGateway = inferenceGateway;
        this.inferenceExecutor = inferenceExecutor;
        this.metricsService = metricsService;
        this.translationService = translationService;
        this.refinementEnabled = refinementEnabled;
        this.minConfidenceForRefinement = minConfidenceForRefinement;
        this.pendingPerSessionCapacity = Math.max(1, pendingPerSessionCapacity);
    }

    AsyncInferenceService(
            InferenceGateway inferenceGateway,
            Executor inferenceExecutor,
            BridgeMetricsService metricsService,
            SignTranslationService translationService
    ) {
        this(inferenceGateway, inferenceExecutor, metricsService, translationService, true, 0.6f, 4);
    }

    AsyncInferenceService(
            InferenceGateway inferenceGateway,
            Executor inferenceExecutor,
            BridgeMetricsService metricsService,
            SignTranslationService translationService,
            boolean refinementEnabled,
            float minConfidenceForRefinement
    ) {
        this(
                inferenceGateway,
                inferenceExecutor,
                metricsService,
                translationService,
                refinementEnabled,
                minConfidenceForRefinement,
                4
        );
    }

    public boolean dispatch(
            String sessionId,
            ClientStreamChunk chunk,
            Consumer<TranslationResult> onComplete
    ) {
        return dispatchWithOutcome(sessionId, chunk, InferenceContext.defaults(), onComplete)
                != InferenceDispatchOutcome.REJECTED;
    }

    public boolean dispatch(
            String sessionId,
            ClientStreamChunk chunk,
            InferenceContext context,
            Consumer<TranslationResult> onComplete
    ) {
        return dispatchWithOutcome(sessionId, chunk, context, onComplete)
                != InferenceDispatchOutcome.REJECTED;
    }

    public InferenceDispatchOutcome dispatchWithOutcome(
            String sessionId,
            ClientStreamChunk chunk,
            InferenceContext context,
            Consumer<TranslationResult> onComplete
    ) {
        PendingInference pending = new PendingInference(chunk, context, onComplete);
        SessionDispatchState state = sessionStates.computeIfAbsent(sessionId, ignored -> new SessionDispatchState());
        synchronized (state) {
            if (state.running) {
                if (state.pending.size() >= pendingPerSessionCapacity) {
                    metricsService.incrementDispatchRejected();
                    return InferenceDispatchOutcome.REJECTED;
                }
                state.pending.add(pending);
                metricsService.incrementDispatchQueued();
                metricsService.updatePendingInferences(totalPendingInferences());
                return InferenceDispatchOutcome.QUEUED;
            }
            state.running = true;
        }

        metricsService.incrementDispatchAccepted();
        startInference(sessionId, state, pending);
        return InferenceDispatchOutcome.STARTED;
    }

    private void startInference(String sessionId, SessionDispatchState state, PendingInference pending) {
        long startedAtNanos = System.nanoTime();
        metricsService.incrementInFlightInferences();
        CompletableFuture<TranslationResult> inferenceFuture;
        try {
            inferenceFuture = CompletableFuture.supplyAsync(
                    () -> inferenceGateway.sendForInference(pending.chunk(), pending.context()),
                    inferenceExecutor
            );
        } catch (RejectedExecutionException error) {
            metricsService.decrementInFlightInferences();
            metricsService.incrementDispatchRejected();
            metricsService.recordInferenceLatency(
                    java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos)
            );
            pending.onComplete().accept(TranslationResult.newBuilder()
                    .setSessionId(sessionId)
                    .setText("Async inference failed: executor rejected the task")
                    .setIsFinal(true)
                    .setConfidence(0.0f)
                    .build());
            startNextOrRelease(sessionId, state);
            return;
        }
        inferenceFuture
                .thenApply(result -> refineWithLlm(result, pending.context()))
                .exceptionally(error -> TranslationResult.newBuilder()
                        .setSessionId(sessionId)
                        .setText("Async inference failed: " + error.getMessage())
                        .setIsFinal(true)
                        .setConfidence(0.0f)
                        .build())
                .whenComplete((result, error) -> {
                    metricsService.decrementInFlightInferences();
                    metricsService.recordInferenceLatency(
                            java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                                    System.nanoTime() - startedAtNanos
                            )
                    );
                    if (result != null) {
                        metricsService.incrementInferenceCompleted();
                        pending.onComplete().accept(result);
                    }
                    startNextOrRelease(sessionId, state);
                });
    }

    private void startNextOrRelease(String sessionId, SessionDispatchState state) {
        PendingInference next;
        synchronized (state) {
            next = state.pending.poll();
            if (next == null) {
                state.running = false;
                sessionStates.remove(sessionId, state);
            }
        }
        metricsService.updatePendingInferences(totalPendingInferences());
        if (next != null) {
            metricsService.incrementDispatchAccepted();
            startInference(sessionId, state, next);
        }
    }

    private int totalPendingInferences() {
        int total = 0;
        for (SessionDispatchState state : sessionStates.values()) {
            synchronized (state) {
                total += state.pending.size();
            }
        }
        return total;
    }

    /**
     * GPU 인식 결과(키워드)를 LLM을 통해 자연스러운 문장으로 변환합니다.
     */
    private TranslationResult refineWithLlm(TranslationResult rawResult, InferenceContext context) {
        if (!shouldRefine(rawResult)) {
            return rawResult;
        }

        List<String> keywords = Arrays.asList(rawResult.getText().split("\\s+"));
        try {
            String refinedText = translationService.translateKeywords(
                    keywords,
                    context.locale(),
                    context.sign_language()
            );
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

    private record PendingInference(
            ClientStreamChunk chunk,
            InferenceContext context,
            Consumer<TranslationResult> onComplete
    ) {
    }

    private static final class SessionDispatchState {
        private final Queue<PendingInference> pending = new ArrayDeque<>();
        private boolean running;
    }
}
