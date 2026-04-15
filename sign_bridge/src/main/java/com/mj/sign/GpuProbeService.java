package com.mj.sign;

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

    public GpuProbeService(
            RestTemplateBuilder restTemplateBuilder,
            GpuServingProperties properties
    ) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofMillis(properties.getProbeTimeoutMs()))
                .readTimeout(Duration.ofMillis(properties.getProbeTimeoutMs()))
                .build();
    }

    public ProbeStatus probe() {
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

    public record ProbeStatus(boolean healthy, Map<String, Object> details) {
    }
}
