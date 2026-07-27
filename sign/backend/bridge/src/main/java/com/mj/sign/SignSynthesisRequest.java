package com.mj.sign;

public record SignSynthesisRequest(
        String session_id,
        String source_type,
        String text,
        String transcript,
        String audio_b64,
        String locale,
        String sign_language,
        String model_profile,
        String output_format,
        String protocol_version
) {
    public SignSynthesisRequest withSourceType(String nextSourceType) {
        return new SignSynthesisRequest(
                session_id,
                nextSourceType,
                text,
                transcript,
                audio_b64,
                locale,
                sign_language,
                model_profile,
                output_format,
                protocol_version
        );
    }
}
