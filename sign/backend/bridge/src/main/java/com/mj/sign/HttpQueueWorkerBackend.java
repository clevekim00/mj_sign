package com.mj.sign;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class HttpQueueWorkerBackend implements QueueWorkerBackend {

    private final GpuServingClient gpuServingClient;

    public HttpQueueWorkerBackend(GpuServingClient gpuServingClient) {
        this.gpuServingClient = gpuServingClient;
    }

    @Override
    public QueueInferenceResult process(QueueInferenceTask task) {
        GpuInferenceResponse response = gpuServingClient.infer(task.request());
        return new QueueInferenceResult(
                task.requestId(),
                task.sessionId(),
                response,
                Instant.now()
        );
    }
}
