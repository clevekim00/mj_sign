package com.mj.sign;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class RoutingSignSynthesisProvider implements SignSynthesisProvider {
    private final SignSynthesisProperties properties;
    private final SignSynthesisProvider mockProvider;
    private final SignSynthesisProvider httpProvider;

    public RoutingSignSynthesisProvider(
            SignSynthesisProperties properties,
            @Qualifier("mockSignSynthesisProvider") SignSynthesisProvider mockProvider,
            @Qualifier("httpSignSynthesisProvider") SignSynthesisProvider httpProvider
    ) {
        this.properties = properties;
        this.mockProvider = mockProvider;
        this.httpProvider = httpProvider;
    }

    @Override
    public SignSynthesisResult synthesize(SignSynthesisRequest request, SignSynthesisContext context) {
        return switch (provider()) {
            case "http" -> httpProvider.synthesize(request, context);
            case "mock" -> mockProvider.synthesize(request, context);
            default -> throw new IllegalArgumentException(
                    "Unsupported sign.synthesis.provider: " + properties.getProvider()
            );
        };
    }

    private String provider() {
        String value = properties.getProvider();
        return value == null || value.isBlank() ? "mock" : value.trim().toLowerCase(Locale.ROOT);
    }
}
