package com.mj.sign;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BridgeMetricsServiceTest {

    @Test
    void exposesGaugeAndCounterSnapshot() {
        BridgeMetricsService metricsService = new BridgeMetricsService();

        metricsService.incrementActiveWebSocketSessions();
        metricsService.updateBufferState(2, 5);
        metricsService.incrementReceivedMessages();
        metricsService.incrementPayloadErrors();
        metricsService.incrementDispatchAccepted();
        metricsService.incrementDispatchRejected();
        metricsService.incrementInferenceCompleted();
        metricsService.incrementIdleFlushTriggered();
        metricsService.incrementInFlightInferences();

        Map<String, Object> snapshot = metricsService.snapshot();
        @SuppressWarnings("unchecked")
        Map<String, Object> gauges = (Map<String, Object>) snapshot.get("gauges");
        @SuppressWarnings("unchecked")
        Map<String, Object> counters = (Map<String, Object>) snapshot.get("counters");

        assertEquals(1, gauges.get("active_websocket_sessions"));
        assertEquals(2, gauges.get("buffered_sessions"));
        assertEquals(5, gauges.get("buffered_frames"));
        assertEquals(1, gauges.get("in_flight_inferences"));

        assertEquals(1L, counters.get("received_messages"));
        assertEquals(1L, counters.get("payload_errors"));
        assertEquals(1L, counters.get("dispatch_accepted"));
        assertEquals(1L, counters.get("dispatch_rejected"));
        assertEquals(1L, counters.get("inference_completed"));
        assertEquals(1L, counters.get("idle_flush_triggered"));
    }
}
