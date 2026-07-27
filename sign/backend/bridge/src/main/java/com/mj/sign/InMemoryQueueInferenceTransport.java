package com.mj.sign;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
public class InMemoryQueueInferenceTransport implements QueueInferenceTransport {

    private final QueueWorkerBackend queueWorkerBackend;
    private final Executor inferenceExecutor;

    public InMemoryQueueInferenceTransport(
            QueueWorkerBackend queueWorkerBackend,
            @Qualifier("inferenceExecutor") Executor inferenceExecutor
    ) {
        this.queueWorkerBackend = queueWorkerBackend;
        this.inferenceExecutor = inferenceExecutor;
    }

    @Override
    public QueueTransportKind kind() {
        return QueueTransportKind.IN_MEMORY;
    }

    @Override
    public QueueInferenceResult submitAndAwait(QueueInferenceTask task, Duration timeout) {
        try {
            return CompletableFuture
                    .supplyAsync(() -> queueWorkerBackend.process(task), inferenceExecutor)
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception error) {
            return new QueueInferenceResult(
                    task.requestId(),
                    task.sessionId(),
                    new GpuInferenceResponse(
                            task.sessionId(),
                            null,
                            true,
                            0,
                            0,
                            null,
                            "Queue transport timed out or failed."
                    ),
                    Instant.now()
            );
        }
    }
}
