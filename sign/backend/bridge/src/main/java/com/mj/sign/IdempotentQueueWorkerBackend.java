package com.mj.sign;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Primary
@Service
public class IdempotentQueueWorkerBackend implements QueueWorkerBackend {

    private final QueueWorkerBackend delegate;
    private final long ttlMillis;
    private final int maxEntries;
    private final Clock clock;
    private final ConcurrentHashMap<String, CachedRequest> requests = new ConcurrentHashMap<>();

    @Autowired
    public IdempotentQueueWorkerBackend(
            @Qualifier("httpQueueWorkerBackend") QueueWorkerBackend delegate,
            @Value("${sign.gpu.queue-idempotency-ttl-ms:300000}") long ttlMillis,
            @Value("${sign.gpu.queue-idempotency-max-entries:10000}") int maxEntries
    ) {
        this(delegate, ttlMillis, maxEntries, Clock.systemUTC());
    }

    IdempotentQueueWorkerBackend(
            QueueWorkerBackend delegate,
            long ttlMillis,
            int maxEntries,
            Clock clock
    ) {
        this.delegate = delegate;
        this.ttlMillis = Math.max(1, ttlMillis);
        this.maxEntries = Math.max(1, maxEntries);
        this.clock = clock;
    }

    @Override
    public QueueInferenceResult process(QueueInferenceTask task) {
        evictExpiredAndOverflow();
        String fingerprint = fingerprint(task);
        CachedRequest created = new CachedRequest(
                fingerprint,
                new CompletableFuture<>(),
                clock.millis()
        );
        CachedRequest existing = requests.putIfAbsent(task.requestId(), created);
        CachedRequest selected = existing == null ? created : existing;

        if (!selected.fingerprint().equals(fingerprint)) {
            throw new IllegalArgumentException("requestId was reused with a different queue payload");
        }
        if (existing != null) {
            return selected.result().join();
        }

        try {
            QueueInferenceResult result = delegate.process(task);
            created.result().complete(result);
            return result;
        } catch (RuntimeException error) {
            requests.remove(task.requestId(), created);
            created.result().completeExceptionally(error);
            throw error;
        }
    }

    private void evictExpiredAndOverflow() {
        long cutoff = clock.millis() - ttlMillis;
        requests.entrySet().removeIf(entry ->
                entry.getValue().createdAtMillis() < cutoff && entry.getValue().result().isDone()
        );
        if (requests.size() <= maxEntries) {
            return;
        }
        requests.entrySet().stream()
                .filter(entry -> entry.getValue().result().isDone())
                .sorted(Map.Entry.comparingByValue(
                        (left, right) -> Long.compare(left.createdAtMillis(), right.createdAtMillis())
                ))
                .limit(requests.size() - maxEntries)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(requests::remove);
    }

    private String fingerprint(QueueInferenceTask task) {
        return task.sessionId() + "|" + task.topic() + "|" + String.valueOf(task.request());
    }

    private record CachedRequest(
            String fingerprint,
            CompletableFuture<QueueInferenceResult> result,
            long createdAtMillis
    ) {
    }
}
