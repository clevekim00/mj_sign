package com.mj.sign;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @Operation(
            summary = "Read bridge metrics",
            description = "Returns lightweight bridge counters and active GPU provider configuration.",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Metrics snapshot.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "metrics",
                                    value = """
                                            {
                                              "timestamp": "2026-04-29T00:00:00Z",
                                              "bridge": {
                                                "sessions_started": 12,
                                                "frames_received": 240,
                                                "final_results_sent": 10,
                                                "errors": 0
                                              },
                                              "gpu": {
                                                "provider": "http",
                                                "base_url": "http://localhost:8000",
                                                "queue_transport": "none"
                                              }
                                            }
                                            """
                            )
                    )
            )
    )
    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("bridge", metricsService.snapshot());
        response.put("gpu", providerSummary());
        return response;
    }

    @Operation(
            summary = "Read Prometheus metrics",
            description = "Returns bridge gauges and counters in Prometheus text exposition format.",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Prometheus-compatible metrics.",
                    content = @Content(
                            mediaType = "text/plain",
                            examples = @ExampleObject(
                                    name = "prometheus",
                                    value = """
                                            # TYPE signbridge_active_websocket_sessions gauge
                                            signbridge_active_websocket_sessions 0
                                            # TYPE signbridge_received_messages_total counter
                                            signbridge_received_messages_total 0
                                            """
                            )
                    )
            )
    )
    @GetMapping(value = "/metrics.prometheus", produces = MediaType.TEXT_PLAIN_VALUE)
    public String prometheusMetrics() {
        return metricsService.prometheusSnapshot();
    }

    @Operation(
            summary = "Check liveness",
            description = "Returns UP when the bridge process is alive.",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Liveness response.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "healthz",
                                    value = """
                                            {
                                              "status": "UP",
                                              "timestamp": "2026-04-29T00:00:00Z",
                                              "service": "sign-bridge",
                                              "gpu": {
                                                "provider": "http",
                                                "base_url": "http://localhost:8000"
                                              }
                                            }
                                            """
                            )
                    )
            )
    )
    @GetMapping("/healthz")
    public Map<String, Object> healthz() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());
        response.put("service", "sign-bridge");
        response.put("gpu", providerSummary());
        return response;
    }

    @Operation(
            summary = "Check readiness",
            description = "Probes the configured model provider and returns 200 only when the bridge can serve inference traffic.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Bridge and model provider are ready.",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "ready",
                                            value = """
                                                    {
                                                      "status": "READY",
                                                      "timestamp": "2026-04-29T00:00:00Z",
                                                      "gpu": {
                                                        "provider": "http",
                                                        "base_url": "http://localhost:8000",
                                                        "healthy": true
                                                      },
                                                      "bridge": {
                                                        "sessions_started": 12,
                                                        "frames_received": 240,
                                                        "final_results_sent": 10,
                                                        "errors": 0
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "503",
                            description = "Configured model provider is unavailable.",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "not-ready",
                                            value = """
                                                    {
                                                      "status": "NOT_READY",
                                                      "timestamp": "2026-04-29T00:00:00Z",
                                                      "gpu": {
                                                        "provider": "http",
                                                        "base_url": "http://localhost:8000",
                                                        "healthy": false,
                                                        "error": "connection refused"
                                                      },
                                                      "bridge": {
                                                        "sessions_started": 0,
                                                        "frames_received": 0,
                                                        "final_results_sent": 0,
                                                        "errors": 1
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
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
