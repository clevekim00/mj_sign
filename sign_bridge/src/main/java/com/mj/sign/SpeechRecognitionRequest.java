package com.mj.sign;

public record SpeechRecognitionRequest(
        String session_id,
        String audio_b64,
        String locale,
        String sign_language,
        String model_profile,
        String protocol_version
) {
}
