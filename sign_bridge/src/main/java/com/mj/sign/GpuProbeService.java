package com.mj.sign;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class GpuProbeService {

    private final RestTemplate restTemplate;
    private final GpuServingProperties properties;
    private final ObjectProvider<KafkaBrokerPort> kafkaBrokerPortProvider;
    private final ObjectProvider<RabbitMqBrokerPort> rabbitMqBrokerPortProvider;

    public GpuProbeService(
            RestTemplateBuilder restTemplateBuilder,
            GpuServingProperties properties,
            ObjectProvider<KafkaBrokerPort> kafkaBrokerPortProvider,
            ObjectProvider<RabbitMqBrokerPort> rabbitMqBrokerPortProvider
    ) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofMillis(properties.getProbeTimeoutMs()))
                .readTimeout(Duration.ofMillis(properties.getProbeTimeoutMs()))
                .build();
        this.kafkaBrokerPortProvider = kafkaBrokerPortProvider;
        this.rabbitMqBrokerPortProvider = rabbitMqBrokerPortProvider;
    }

    public ProbeStatus probe() {
        InferenceProvider provider = InferenceProvider.fromConfig(properties.getProvider());
        return switch (provider) {
            case HTTP -> probeHttpBackend();
            case QUEUE -> probeQueuePath();
            case GRPC -> grpcNotReady();
        };
    }

    protected ProbeStatus probeHttpBackend() {
        String probeUrl = HttpInferenceGateway.joinUrl(properties.getBaseUrl(), properties.getHealthPath());
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(probeUrl, Map.class);
            boolean healthy = response.getStatusCode().is2xxSuccessful();

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("probe_url", probeUrl);
            details.put("status_code", response.getStatusCode().value());
            details.put("response_body", response.getBody());
            details.put("provider", properties.getProvider());

            return new ProbeStatus(healthy, details);
        } catch (Exception error) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("probe_url", probeUrl);
            details.put("provider", properties.getProvider());
            details.put("error", error.getClass().getSimpleName());
            details.put("message", error.getMessage());
            return new ProbeStatus(false, details);
        }
    }

    private ProbeStatus probeQueuePath() {
        QueueTransportKind transportKind = QueueTransportKind.fromConfig(properties.getQueueTransport());
        ProbeStatus downstreamGpu = probeHttpBackend();

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("provider", properties.getProvider());
        details.put("queue_transport", transportKind.configValue());
        details.put("queue_broker_mode", properties.getQueueBrokerMode());
        details.put("downstream_gpu", downstreamGpu.details());

        boolean adapterPresent;
        switch (transportKind) {
            case IN_MEMORY -> {
                details.put("broker_adapter_present", true);
                return new ProbeStatus(downstreamGpu.healthy(), details);
            }
            case KAFKA -> adapterPresent = kafkaBrokerPortProvider.getIfAvailable() != null;
            case RABBITMQ -> adapterPresent = rabbitMqBrokerPortProvider.getIfAvailable() != null;
            default -> adapterPresent = false;
        }

        details.put("broker_adapter_present", adapterPresent);
        return new ProbeStatus(adapterPresent && downstreamGpu.healthy(), details);
    }

    private ProbeStatus grpcNotReady() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("provider", properties.getProvider());
        details.put("grpc_target", properties.getGrpcTarget());
        details.put("status", "NOT_IMPLEMENTED");
        return new ProbeStatus(false, details);
    }

    public record ProbeStatus(boolean healthy, Map<String, Object> details) {
    }
}
