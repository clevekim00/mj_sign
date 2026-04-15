package com.mj.sign;

import java.time.Duration;

public interface QueueInferenceTransport {
    QueueTransportKind kind();

    QueueInferenceResult submitAndAwait(QueueInferenceTask task, Duration timeout);
}
