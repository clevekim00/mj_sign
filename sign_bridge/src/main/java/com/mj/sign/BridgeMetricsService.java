package com.mj.sign;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BridgeMetricsService {

    private final AtomicInteger activeWebSocketSessions = new AtomicInteger();
    private final AtomicInteger bufferedSessions = new AtomicInteger();
    private final AtomicInteger bufferedFrames = new AtomicInteger();
    private final AtomicInteger inFlightInferences = new AtomicInteger();

    private final AtomicLong receivedMessages = new AtomicLong();
    private final AtomicLong payloadErrors = new AtomicLong();
    private final AtomicLong dispatchAccepted = new AtomicLong();
    private final AtomicLong dispatchRejected = new AtomicLong();
    private final AtomicLong inferenceCompleted = new AtomicLong();
    private final AtomicLong idleFlushTriggered = new AtomicLong();

    public void incrementActiveWebSocketSessions() {
        activeWebSocketSessions.incrementAndGet();
    }

    public void decrementActiveWebSocketSessions() {
        activeWebSocketSessions.updateAndGet(current -> Math.max(0, current - 1));
    }

    public void updateBufferState(int sessionCount, int frameCount) {
        bufferedSessions.set(Math.max(0, sessionCount));
        bufferedFrames.set(Math.max(0, frameCount));
    }

    public void incrementReceivedMessages() {
        receivedMessages.incrementAndGet();
    }

    public void incrementPayloadErrors() {
        payloadErrors.incrementAndGet();
    }

    public void incrementDispatchAccepted() {
        dispatchAccepted.incrementAndGet();
    }

    public void incrementDispatchRejected() {
        dispatchRejected.incrementAndGet();
    }

    public void incrementInferenceCompleted() {
        inferenceCompleted.incrementAndGet();
    }

    public void incrementIdleFlushTriggered() {
        idleFlushTriggered.incrementAndGet();
    }

    public void incrementInFlightInferences() {
        inFlightInferences.incrementAndGet();
    }

    public void decrementInFlightInferences() {
        inFlightInferences.updateAndGet(current -> Math.max(0, current - 1));
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> gauges = new LinkedHashMap<>();
        gauges.put("active_websocket_sessions", activeWebSocketSessions.get());
        gauges.put("buffered_sessions", bufferedSessions.get());
        gauges.put("buffered_frames", bufferedFrames.get());
        gauges.put("in_flight_inferences", inFlightInferences.get());

        Map<String, Object> counters = new LinkedHashMap<>();
        counters.put("received_messages", receivedMessages.get());
        counters.put("payload_errors", payloadErrors.get());
        counters.put("dispatch_accepted", dispatchAccepted.get());
        counters.put("dispatch_rejected", dispatchRejected.get());
        counters.put("inference_completed", inferenceCompleted.get());
        counters.put("idle_flush_triggered", idleFlushTriggered.get());

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("gauges", gauges);
        snapshot.put("counters", counters);
        return snapshot;
    }
}
