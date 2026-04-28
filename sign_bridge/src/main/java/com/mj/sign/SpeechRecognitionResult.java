package com.mj.sign;

public record SpeechRecognitionResult(
        String session_id,
        String transcript,
        Number confidence,
        String provider,
        String error
) {
}
