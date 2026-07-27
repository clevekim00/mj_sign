package com.mj.sign;

public record QueueBrokerReplyMessage(
        String requestId,
        String sessionId,
        GpuInferenceResponse response
) {
}
