package com.mj.sign;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/internal")
public class OperationsController {

    private final BridgeMetricsService metricsService;
    private final GpuProbeService gpuProbeService;
    private final GpuServingProperties gpuServingProperties;

    public OperationsController(
            BridgeMetricsService metricsService,
            GpuProbeService gpuProbeService,
            GpuServingProperties gpuServingProperties
    ) {
        this.metricsService = metricsService;
        this.gpuProbeService = gpuProbeService;
        this.gpuServingProperties = gpuServingProperties;
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("bridge", metricsService.snapshot());
        response.put("gpu", providerSummary());
        return response;
    }

    @GetMapping("/healthz")
    public Map<String, Object> healthz() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());
        response.put("service", "sign-bridge");
        response.put("gpu", providerSummary());
        return response;
    }

    @GetMapping("/readyz")
    public ResponseEntity<Map<String, Object>> readyz() {
        GpuProbeService.ProbeStatus gpuStatus = gpuProbeService.probe();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", gpuStatus.healthy() ? "READY" : "NOT_READY");
        response.put("timestamp", Instant.now().toString());
        response.put("gpu", gpuStatus.details());
        response.put("bridge", metricsService.snapshot());

        return ResponseEntity
                .status(gpuStatus.healthy() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }

    private Map<String, Object> providerSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("provider", gpuServingProperties.getProvider());
        summary.put("base_url", gpuServingProperties.getBaseUrl());
        summary.put("infer_path", gpuServingProperties.getInferPath());
        summary.put("health_path", gpuServingProperties.getHealthPath());
        summary.put("grpc_target", gpuServingProperties.getGrpcTarget());
        summary.put("queue_transport", gpuServingProperties.getQueueTransport());
        summary.put("queue_topic", gpuServingProperties.getQueueTopic());
        summary.put("queue_request_topic", gpuServingProperties.getQueueRequestTopic());
        summary.put("queue_result_topic", gpuServingProperties.getQueueResultTopic());
        summary.put("queue_consumer_group", gpuServingProperties.getQueueConsumerGroup());
        summary.put("queue_exchange", gpuServingProperties.getQueueExchange());
        summary.put("queue_routing_key", gpuServingProperties.getQueueRoutingKey());
        summary.put("queue_timeout_ms", gpuServingProperties.getQueueTimeoutMs());
        return summary;
    }
}
