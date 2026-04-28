package com.mj.sign;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class SignSynthesisService {
    public static final String PROTOCOL_VERSION = "signbridge-synthesis-v1";

    private final SignLanguageResolver signLanguageResolver;
    private final RoutingSpeechToTextAdapter speechToTextAdapter;
    private final RoutingSignSynthesisProvider signSynthesisProvider;
    private final SignSynthesisProperties properties;

    public SignSynthesisService(
            SignLanguageResolver signLanguageResolver,
            RoutingSpeechToTextAdapter speechToTextAdapter,
            RoutingSignSynthesisProvider signSynthesisProvider,
            SignSynthesisProperties properties
    ) {
        this.signLanguageResolver = signLanguageResolver;
        this.speechToTextAdapter = speechToTextAdapter;
        this.signSynthesisProvider = signSynthesisProvider;
        this.properties = properties;
    }

    public SignSynthesisResult synthesize(SignSynthesisRequest request) {
        SignSynthesisRequest safeRequest = request == null
                ? new SignSynthesisRequest(null, null, null, null, null, null, null, null, null, null)
                : request;

        SignSynthesisContext context = resolveContext(safeRequest);
        if (isBlank(context.text()) && "speech".equals(context.source_type())) {
            SpeechRecognitionResult speech = speechToTextAdapter.transcribe(safeRequest, context);
            if (speech.error() != null && !speech.error().isBlank()) {
                throw new IllegalStateException("ASR provider error: " + speech.error());
            }
            context = context.withText(speech.transcript());
        }

        if (isBlank(context.text())) {
            throw new IllegalArgumentException("text or transcript is required for sign synthesis.");
        }

        return signSynthesisProvider.synthesize(safeRequest, context);
    }

    private SignSynthesisContext resolveContext(SignSynthesisRequest request) {
        String sourceType = defaultIfBlank(request.source_type(), "text").toLowerCase(Locale.ROOT);
        InferenceContext languageContext = signLanguageResolver.resolve(
                request.locale(),
                request.sign_language(),
                request.model_profile()
        );
        return new SignSynthesisContext(
                defaultIfBlank(request.session_id(), "synthesis-" + UUID.randomUUID()),
                sourceType,
                firstNonBlank(request.text(), request.transcript()),
                languageContext.locale(),
                languageContext.sign_language(),
                languageContext.model_profile(),
                defaultIfBlank(request.output_format(), properties.getOutputFormat()),
                normalizeProtocolVersion(request.protocol_version())
        );
    }

    private String normalizeProtocolVersion(String value) {
        String fallback = defaultIfBlank(properties.getProtocolVersion(), PROTOCOL_VERSION);
        return defaultIfBlank(value, fallback).trim().toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String first, String second) {
        if (!isBlank(first)) {
            return first.trim();
        }
        if (!isBlank(second)) {
            return second.trim();
        }
        return null;
    }

    private String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
