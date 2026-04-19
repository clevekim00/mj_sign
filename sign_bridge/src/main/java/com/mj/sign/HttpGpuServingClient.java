package com.mj.sign;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service
public class HttpGpuServingClient implements GpuServingClient {

    private final RestTemplate restTemplate;
    private final GpuServingProperties properties;

    @org.springframework.beans.factory.annotation.Autowired
    public HttpGpuServingClient(
            RestTemplateBuilder restTemplateBuilder,
            GpuServingProperties properties
    ) {
        this(
                restTemplateBuilder
                        .connectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                        .readTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                        .build(),
                properties
        );
    }

    HttpGpuServingClient(RestTemplate restTemplate, GpuServingProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public GpuInferenceResponse infer(GpuInferenceRequest request) {
        ResponseEntity<GpuInferenceResponse> response = restTemplate.postForEntity(
                HttpInferenceGateway.joinUrl(properties.getBaseUrl(), properties.getInferPath()),
                request,
                GpuInferenceResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return new GpuInferenceResponse(
                    request.session_id(),
                    null,
                    true,
                    0,
                    0,
                    null,
                    "GPU server error: " + response.getStatusCode()
            );
        }
        return response.getBody();
    }
}
