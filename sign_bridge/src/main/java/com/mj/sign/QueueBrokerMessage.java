package com.mj.sign;

import java.time.Instant;
import java.util.Map;

public record QueueBrokerMessage(
        String requestId,
        String sessionId,
        String requestTopic,
        String resultTopic,
        String consumerGroup,
        String routingKey,
        Map<String, Object> headers,
        GpuInferenceRequest payload,
        Instant createdAt
) {
}
