package com.mj.sign;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationsControllerTest {

    @Test
    void returnsServiceUnavailableWhenGpuProbeFails() {
        BridgeMetricsService metricsService = new BridgeMetricsService();
        OperationsController controller = new OperationsController(
                metricsService,
                new StubGpuProbeService(false),
                properties()
        );

        ResponseEntity<Map<String, Object>> response = controller.readyz();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("NOT_READY", response.getBody().get("status"));
    }

    @Test
    void returnsOkWhenGpuProbeSucceeds() {
        OperationsController controller = new OperationsController(
                new BridgeMetricsService(),
                new StubGpuProbeService(true),
                properties()
        );

        ResponseEntity<Map<String, Object>> response = controller.readyz();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("READY", response.getBody().get("status"));
    }

    private static final class StubGpuProbeService extends GpuProbeService {
        private final boolean healthy;

        private StubGpuProbeService(boolean healthy) {
            super(new org.springframework.boot.web.client.RestTemplateBuilder(), properties());
            this.healthy = healthy;
        }

        @Override
        public ProbeStatus probe() {
            return new ProbeStatus(healthy, Map.of("probe_url", "stub"));
        }
    }

    private static GpuServingProperties properties() {
        GpuServingProperties properties = new GpuServingProperties();
        properties.setProvider("http");
        properties.setBaseUrl("http://localhost:8000");
        properties.setInferPath("/api/v2/recognize");
        properties.setHealthPath("/");
        return properties;
    }
}
