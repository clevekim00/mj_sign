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
    private final AtomicInteger pendingInferences = new AtomicInteger();

    private final AtomicLong receivedMessages = new AtomicLong();
    private final AtomicLong payloadErrors = new AtomicLong();
    private final AtomicLong dispatchAccepted = new AtomicLong();
    private final AtomicLong dispatchRejected = new AtomicLong();
    private final AtomicLong dispatchQueued = new AtomicLong();
    private final AtomicLong inferenceCompleted = new AtomicLong();
    private final AtomicLong idleFlushTriggered = new AtomicLong();
    private final AtomicLong modelProtocolErrors = new AtomicLong();
    private final AtomicLong droppedFrames = new AtomicLong();
    private final AtomicLong inferenceLatencyCount = new AtomicLong();
    private final AtomicLong inferenceLatencyTotalMs = new AtomicLong();
    private final AtomicLong inferenceLatencyMaxMs = new AtomicLong();
    private final AtomicLong websocketSendFailures = new AtomicLong();

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

    public void incrementDispatchQueued() {
        dispatchQueued.incrementAndGet();
    }

    public void updatePendingInferences(int count) {
        pendingInferences.set(Math.max(0, count));
    }

    public void incrementInferenceCompleted() {
        inferenceCompleted.incrementAndGet();
    }

    public void incrementIdleFlushTriggered() {
        idleFlushTriggered.incrementAndGet();
    }

    public void incrementModelProtocolErrors() {
        modelProtocolErrors.incrementAndGet();
    }

    public void addDroppedFrames(int count) {
        if (count > 0) {
            droppedFrames.addAndGet(count);
        }
    }

    public void recordInferenceLatency(long latencyMillis) {
        long normalized = Math.max(0, latencyMillis);
        inferenceLatencyCount.incrementAndGet();
        inferenceLatencyTotalMs.addAndGet(normalized);
        inferenceLatencyMaxMs.accumulateAndGet(normalized, Math::max);
    }

    public void incrementWebSocketSendFailures() {
        websocketSendFailures.incrementAndGet();
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
        gauges.put("pending_inferences", pendingInferences.get());

        Map<String, Object> counters = new LinkedHashMap<>();
        counters.put("received_messages", receivedMessages.get());
        counters.put("payload_errors", payloadErrors.get());
        counters.put("dispatch_accepted", dispatchAccepted.get());
        counters.put("dispatch_rejected", dispatchRejected.get());
        counters.put("dispatch_queued", dispatchQueued.get());
        counters.put("inference_completed", inferenceCompleted.get());
        counters.put("idle_flush_triggered", idleFlushTriggered.get());
        counters.put("model_protocol_errors", modelProtocolErrors.get());
        counters.put("dropped_frames", droppedFrames.get());
        counters.put("websocket_send_failures", websocketSendFailures.get());

        Map<String, Object> timings = new LinkedHashMap<>();
        long latencyCount = inferenceLatencyCount.get();
        timings.put("inference_latency_count", latencyCount);
        timings.put("inference_latency_avg_ms", latencyCount == 0
                ? 0.0
                : (double) inferenceLatencyTotalMs.get() / latencyCount);
        timings.put("inference_latency_max_ms", inferenceLatencyMaxMs.get());

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("gauges", gauges);
        snapshot.put("counters", counters);
        snapshot.put("timings", timings);
        return snapshot;
    }

    public String prometheusSnapshot() {
        StringBuilder builder = new StringBuilder();
        appendGauge(builder, "active_websocket_sessions", activeWebSocketSessions.get());
        appendGauge(builder, "buffered_sessions", bufferedSessions.get());
        appendGauge(builder, "buffered_frames", bufferedFrames.get());
        appendGauge(builder, "in_flight_inferences", inFlightInferences.get());
        appendGauge(builder, "pending_inferences", pendingInferences.get());

        appendCounter(builder, "received_messages", receivedMessages.get());
        appendCounter(builder, "payload_errors", payloadErrors.get());
        appendCounter(builder, "dispatch_accepted", dispatchAccepted.get());
        appendCounter(builder, "dispatch_rejected", dispatchRejected.get());
        appendCounter(builder, "dispatch_queued", dispatchQueued.get());
        appendCounter(builder, "inference_completed", inferenceCompleted.get());
        appendCounter(builder, "idle_flush_triggered", idleFlushTriggered.get());
        appendCounter(builder, "model_protocol_errors", modelProtocolErrors.get());
        appendCounter(builder, "dropped_frames", droppedFrames.get());
        appendCounter(builder, "websocket_send_failures", websocketSendFailures.get());
        appendCounter(builder, "inference_latency_count", inferenceLatencyCount.get());
        appendGauge(
                builder,
                "inference_latency_avg_ms",
                inferenceLatencyCount.get() == 0
                        ? 0.0
                        : (double) inferenceLatencyTotalMs.get() / inferenceLatencyCount.get()
        );
        appendGauge(builder, "inference_latency_max_ms", inferenceLatencyMaxMs.get());
        return builder.toString();
    }

    private void appendGauge(StringBuilder builder, String name, Number value) {
        appendMetric(builder, name, "gauge", value);
    }

    private void appendCounter(StringBuilder builder, String name, Number value) {
        appendMetric(builder, name + "_total", "counter", value);
    }

    private void appendMetric(StringBuilder builder, String name, String type, Number value) {
        String metricName = "signbridge_" + name;
        builder.append("# TYPE ").append(metricName).append(' ').append(type).append('\n');
        builder.append(metricName).append(' ').append(value).append('\n');
    }
}
