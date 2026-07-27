package com.mj.sign;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class RoutingSpeechToTextAdapter implements SpeechToTextAdapter {
    private final SignSynthesisProperties properties;
    private final SpeechToTextAdapter mockAdapter;
    private final SpeechToTextAdapter httpAdapter;

    public RoutingSpeechToTextAdapter(
            SignSynthesisProperties properties,
            @Qualifier("mockSpeechToTextAdapter") SpeechToTextAdapter mockAdapter,
            @Qualifier("httpSpeechToTextAdapter") SpeechToTextAdapter httpAdapter
    ) {
        this.properties = properties;
        this.mockAdapter = mockAdapter;
        this.httpAdapter = httpAdapter;
    }

    @Override
    public SpeechRecognitionResult transcribe(SignSynthesisRequest request, SignSynthesisContext context) {
        return switch (provider()) {
            case "http" -> httpAdapter.transcribe(request, context);
            case "mock" -> mockAdapter.transcribe(request, context);
            default -> throw new IllegalArgumentException(
                    "Unsupported sign.synthesis.asr-provider: " + properties.getAsrProvider()
            );
        };
    }

    private String provider() {
        String value = properties.getAsrProvider();
        return value == null || value.isBlank() ? "mock" : value.trim().toLowerCase(Locale.ROOT);
    }
}
