package com.mj.sign;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignSynthesisServiceTest {
    @Test
    void synthesizesKoreanTextToKslMotionThroughMockSpi() {
        SignSynthesisService service = newService(new SignSynthesisProperties());

        SignSynthesisResult result = service.synthesize(new SignSynthesisRequest(
                "t2s-ko",
                "text",
                "내일 병원에 가야 합니다.",
                null,
                null,
                "ko-KR",
                "ksl",
                "sign-gemma-ko",
                "landmarks",
                null
        ));

        assertEquals("t2s-ko", result.session_id());
        assertEquals("text", result.source_type());
        assertEquals("ksl", result.sign_language());
        assertEquals(SignSynthesisService.PROTOCOL_VERSION, result.protocol_version());
        assertTrue(result.sign_plan().glosses().contains("병원"));
        assertTrue(result.motion().frame_count() > 0);
        assertEquals(result.motion().frame_count(), result.motion().frames().size());
        assertEquals(21, result.motion().frames().getFirst().left_hand().size());
    }

    @Test
    void synthesizesEnglishTranscriptToAslDefaults() {
        SignSynthesisService service = newService(new SignSynthesisProperties());

        SignSynthesisResult result = service.synthesize(new SignSynthesisRequest(
                "sts-en",
                "speech",
                null,
                "I need help tomorrow!",
                null,
                "en-US",
                null,
                null,
                null,
                null
        ));

        assertEquals("speech", result.source_type());
        assertEquals("asl", result.sign_language());
        assertEquals("sign-gemma", result.model_profile());
        assertTrue(result.sign_plan().glosses().contains("NEED"));
        assertTrue(result.sign_plan().non_manual_markers().contains("emphasis"));
        assertFalse(result.motion().frames().isEmpty());
    }

    @Test
    void usesAsrAdapterForSpeechAudioWhenTranscriptIsMissing() {
        SignSynthesisService service = newService(new SignSynthesisProperties());

        SignSynthesisResult result = service.synthesize(new SignSynthesisRequest(
                "sts-audio",
                "speech",
                null,
                null,
                "base64-audio",
                "ko-KR",
                "ksl",
                null,
                null,
                null
        ));

        assertEquals("mock speech input", result.text());
        assertTrue(result.sign_plan().glosses().contains("mock"));
    }

    @Test
    void rejectsMissingTextAndTranscriptForTextSource() {
        SignSynthesisService service = newService(new SignSynthesisProperties());
        SignSynthesisRequest request = new SignSynthesisRequest(
                "missing-input",
                "text",
                null,
                null,
                null,
                "ko-KR",
                "ksl",
                null,
                null,
                null
        );

        assertThrows(IllegalArgumentException.class, () -> service.synthesize(request));
    }

    @Test
    void routesToHttpSynthesisProviderWhenConfigured() {
        SignSynthesisProperties properties = new SignSynthesisProperties();
        properties.setProvider("http");
        AtomicBoolean httpProviderCalled = new AtomicBoolean(false);

        SignSynthesisProvider httpProvider = (request, context) -> {
            httpProviderCalled.set(true);
            return new SignSynthesisResult(
                    context.session_id(),
                    "synthesis_result",
                    context.source_type(),
                    context.text(),
                    context.locale(),
                    context.sign_language(),
                    context.model_profile(),
                    context.protocol_version(),
                    new SignSynthesisPlan(java.util.List.of("HTTP"), java.util.List.of("neutral"), "external"),
                    new SignSynthesisMotion("landmark-frames", 12, 0, java.util.List.of()),
                    true,
                    0.9,
                    null
            );
        };

        SignSynthesisService service = newService(properties, httpProvider);
        SignSynthesisResult result = service.synthesize(new SignSynthesisRequest(
                "http-provider",
                "text",
                "hello",
                null,
                null,
                "en-US",
                null,
                null,
                null,
                null
        ));

        assertTrue(httpProviderCalled.get());
        assertEquals(java.util.List.of("HTTP"), result.sign_plan().glosses());
    }

    private SignSynthesisService newService(SignSynthesisProperties properties) {
        SignSynthesisProvider failingHttpProvider = (request, context) -> {
            throw new AssertionError("HTTP synthesis provider should not be called in mock tests.");
        };
        return newService(properties, failingHttpProvider);
    }

    private SignSynthesisService newService(
            SignSynthesisProperties properties,
            SignSynthesisProvider httpProvider
    ) {
        SignLanguageResolver resolver = new SignLanguageResolver(new SignLanguageProperties());
        SpeechToTextAdapter failingHttpAsr = (request, context) -> {
            throw new AssertionError("HTTP ASR adapter should not be called in mock tests.");
        };
        RoutingSpeechToTextAdapter speechToTextAdapter = new RoutingSpeechToTextAdapter(
                properties,
                new MockSpeechToTextAdapter(),
                failingHttpAsr
        );
        RoutingSignSynthesisProvider synthesisProvider = new RoutingSignSynthesisProvider(
                properties,
                new MockSignSynthesisProvider(new DefaultSignPlanner(), new MockSignMotionGenerator()),
                httpProvider
        );
        return new SignSynthesisService(resolver, speechToTextAdapter, synthesisProvider, properties);
    }
}
