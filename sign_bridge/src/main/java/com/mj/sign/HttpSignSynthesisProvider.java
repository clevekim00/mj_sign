package com.mj.sign;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service("httpSignSynthesisProvider")
public class HttpSignSynthesisProvider implements SignSynthesisProvider {
    private final RestTemplate restTemplate;
    private final SignSynthesisProperties properties;

    public HttpSignSynthesisProvider(
            RestTemplateBuilder restTemplateBuilder,
            SignSynthesisProperties properties
    ) {
        this(
                restTemplateBuilder
                        .connectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                        .readTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                        .build(),
                properties
        );
    }

    HttpSignSynthesisProvider(RestTemplate restTemplate, SignSynthesisProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public SignSynthesisResult synthesize(SignSynthesisRequest request, SignSynthesisContext context) {
        ResponseEntity<SignSynthesisResult> response = restTemplate.postForEntity(
                HttpInferenceGateway.joinUrl(properties.getBaseUrl(), properties.getSynthesizePath()),
                context.toRequest(request == null ? null : request.audio_b64()),
                SignSynthesisResult.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Sign synthesis provider error: " + response.getStatusCode());
        }
        return response.getBody();
    }
}
