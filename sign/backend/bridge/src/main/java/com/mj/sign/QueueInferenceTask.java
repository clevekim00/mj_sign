package com.mj.sign;

import java.time.Instant;

public record QueueInferenceTask(
        String requestId,
        String sessionId,
        String topic,
        GpuInferenceRequest request,
        Instant submittedAt
) {
}
