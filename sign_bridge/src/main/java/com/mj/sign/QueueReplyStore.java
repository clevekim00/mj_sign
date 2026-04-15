package com.mj.sign;

import java.time.Duration;

public interface QueueReplyStore {
    void registerRequest(String requestId);

    void complete(QueueInferenceResult result);

    QueueInferenceResult await(String requestId, Duration timeout);
}
