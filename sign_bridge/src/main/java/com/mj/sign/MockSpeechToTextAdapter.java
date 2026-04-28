package com.mj.sign;

import org.springframework.stereotype.Service;

@Service("mockSpeechToTextAdapter")
public class MockSpeechToTextAdapter implements SpeechToTextAdapter {
    @Override
    public SpeechRecognitionResult transcribe(SignSynthesisRequest request, SignSynthesisContext context) {
        String transcript = request != null && request.transcript() != null && !request.transcript().isBlank()
                ? request.transcript().trim()
                : "mock speech input";
        return new SpeechRecognitionResult(
                context.session_id(),
                transcript,
                0.72,
                "mock",
                null
        );
    }
}
