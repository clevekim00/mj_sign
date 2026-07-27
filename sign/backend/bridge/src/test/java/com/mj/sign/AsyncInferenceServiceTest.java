package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
import com.mj.sign.service.AiTranslationProvider;
import com.mj.sign.service.SignTranslationService;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncInferenceServiceTest {

    @Test
    void rejectsConcurrentDispatchForSameSessionUntilCompletion() {
        QueueingExecutor executor = new QueueingExecutor();
        CountingInferenceGateway gateway = new CountingInferenceGateway();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        AsyncInferenceService service = new AsyncInferenceService(
                gateway,
                executor,
                metricsService,
                translationService()
        );
        AtomicReference<TranslationResult> firstResult = new AtomicReference<>();

        boolean firstAccepted = service.dispatch("s-1", chunk("s-1"), firstResult::set);
        boolean secondAccepted = service.dispatch("s-1", chunk("s-1"), ignored -> {
        });

        assertTrue(firstAccepted);
        assertFalse(secondAccepted);
        assertEquals(0, gateway.callCount);
        @SuppressWarnings("unchecked")
        Map<String, Object> counters = (Map<String, Object>) metricsService.snapshot().get("counters");
        assertEquals(1L, ((Number) counters.get("dispatch_accepted")).longValue());

        executor.runNext();

        assertEquals(1, gateway.callCount);
        assertEquals("ok", firstResult.get().getText());
    }

    @Test
    void allowsNewDispatchAfterPreviousOneCompletes() {
        QueueingExecutor executor = new QueueingExecutor();
        CountingInferenceGateway gateway = new CountingInferenceGateway();
        AsyncInferenceService service = new AsyncInferenceService(
                gateway,
                executor,
                new BridgeMetricsService(),
                translationService()
        );

        assertTrue(service.dispatch("s-2", chunk("s-2"), ignored -> {
        }));
        executor.runNext();
        assertTrue(service.dispatch("s-2", chunk("s-2"), ignored -> {
        }));
    }

    @Test
    void refinesOnlyFinalHighConfidenceResults() {
        QueueingExecutor executor = new QueueingExecutor();
        AsyncInferenceService service = new AsyncInferenceService(
                gatewayReturning("나 밥 먹다", true, 0.95f),
                executor,
                new BridgeMetricsService(),
                new SignTranslationService(new AiTranslationProvider() {
                    @Override
                    public String generateResponse(String systemPrompt, String userPrompt) {
                        return "저는 밥을 먹어요.";
                    }
                })
        );
        AtomicReference<TranslationResult> result = new AtomicReference<>();

        assertTrue(service.dispatch("s-3", chunk("s-3"), result::set));
        executor.runNext();

        assertEquals("저는 밥을 먹어요.", result.get().getText());
    }

    @Test
    void skipsRefinementForNonFinalOrLowConfidenceResults() {
        QueueingExecutor executor = new QueueingExecutor();
        AsyncInferenceService service = new AsyncInferenceService(
                gatewayReturning("Buffering 4 frames before inference.", false, 0.0f),
                executor,
                new BridgeMetricsService(),
                new SignTranslationService(new AiTranslationProvider() {
                    @Override
                    public String generateResponse(String systemPrompt, String userPrompt) {
                        return "이 값은 사용되면 안 됩니다.";
                    }
                })
        );
        AtomicReference<TranslationResult> result = new AtomicReference<>();

        assertTrue(service.dispatch("s-4", chunk("s-4"), result::set));
        executor.runNext();

        assertEquals("Buffering 4 frames before inference.", result.get().getText());
    }

    private ClientStreamChunk chunk(String sessionId) {
        return ClientStreamChunk.newBuilder()
                .setSessionId(sessionId)
                .build();
    }

    private SignTranslationService translationService() {
        return new SignTranslationService(new AiTranslationProvider() {
            @Override
            public String generateResponse(String systemPrompt, String userPrompt) {
                return "ok";
            }
        });
    }

    private InferenceGateway gatewayReturning(String text, boolean isFinal, float confidence) {
        return new InferenceGateway() {
            @Override
            public InferenceProvider provider() {
                return InferenceProvider.HTTP;
            }

            @Override
            public TranslationResult sendForInference(ClientStreamChunk chunk) {
                return TranslationResult.newBuilder()
                        .setSessionId(chunk.getSessionId())
                        .setText(text)
                        .setIsFinal(isFinal)
                        .setConfidence(confidence)
                        .build();
            }
        };
    }

    private static final class CountingInferenceGateway implements InferenceGateway {
        private int callCount;

        @Override
        public InferenceProvider provider() {
            return InferenceProvider.HTTP;
        }

        @Override
        public TranslationResult sendForInference(ClientStreamChunk chunk) {
            callCount++;
            return TranslationResult.newBuilder()
                    .setSessionId(chunk.getSessionId())
                    .setText("ok")
                    .setIsFinal(true)
                    .setConfidence(1.0f)
                    .build();
        }
    }

    private static final class QueueingExecutor implements Executor {
        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        private void runNext() {
            Runnable task = tasks.poll();
            if (task != null) {
                task.run();
            }
        }
    }
}
