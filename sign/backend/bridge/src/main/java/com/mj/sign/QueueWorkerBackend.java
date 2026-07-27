package com.mj.sign;

public interface QueueWorkerBackend {
    QueueInferenceResult process(QueueInferenceTask task);
}
