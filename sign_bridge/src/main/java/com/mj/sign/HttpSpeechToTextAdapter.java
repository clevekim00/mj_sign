package com.mj.sign;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service("httpSpeechToTextAdapter")
public class HttpSpeechToTextAdapter implements SpeechToTextAdapter {
    private final RestTemplate restTemplate;
    private final SignSynthesisProperties properties;

    @Autowired
    public HttpSpeechToTextAdapter(
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

    HttpSpeechToTextAdapter(RestTemplate restTemplate, SignSynthesisProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public SpeechRecognitionResult transcribe(SignSynthesisRequest request, SignSynthesisContext context) {
        SpeechRecognitionRequest speechRequest = new SpeechRecognitionRequest(
                context.session_id(),
                request == null ? null : request.audio_b64(),
                context.locale(),
                context.sign_language(),
                context.model_profile(),
                context.protocol_version()
        );

        ResponseEntity<SpeechRecognitionResult> response = restTemplate.postForEntity(
                HttpInferenceGateway.joinUrl(properties.getAsrBaseUrl(), properties.getAsrPath()),
                speechRequest,
                SpeechRecognitionResult.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("ASR provider error: " + response.getStatusCode());
        }
        return response.getBody();
    }
}
