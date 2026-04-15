package com.mj.sign;

import java.time.Instant;

public record QueueInferenceResult(
        String requestId,
        String sessionId,
        GpuInferenceResponse response,
        Instant completedAt
) {
}
