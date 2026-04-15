package com.mj.sign;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class InMemoryQueueReplyStore implements QueueReplyStore {

    private final ConcurrentHashMap<String, CompletableFuture<QueueInferenceResult>> replies = new ConcurrentHashMap<>();

    @Override
    public void registerRequest(String requestId) {
        replies.put(requestId, new CompletableFuture<>());
    }

    @Override
    public void complete(QueueInferenceResult result) {
        CompletableFuture<QueueInferenceResult> future = replies.get(result.requestId());
        if (future != null) {
            future.complete(result);
        }
    }

    @Override
    public QueueInferenceResult await(String requestId, Duration timeout) {
        CompletableFuture<QueueInferenceResult> future = replies.get(requestId);
        if (future == null) {
            return timeoutResult(requestId, "unknown-session");
        }

        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception error) {
            return timeoutResult(requestId, "unknown-session");
        } finally {
            replies.remove(requestId);
        }
    }

    private QueueInferenceResult timeoutResult(String requestId, String sessionId) {
        return new QueueInferenceResult(
                requestId,
                sessionId,
                new GpuInferenceResponse(
                        sessionId,
                        null,
                        true,
                        0,
                        0,
                        null,
                        "Queue reply timed out."
                ),
                Instant.now()
        );
    }
}
