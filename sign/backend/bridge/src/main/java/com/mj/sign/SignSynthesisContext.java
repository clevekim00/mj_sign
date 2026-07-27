package com.mj.sign;

public record SignSynthesisContext(
        String session_id,
        String source_type,
        String text,
        String locale,
        String sign_language,
        String model_profile,
        String output_format,
        String protocol_version
) {
    public SignSynthesisContext withText(String nextText) {
        return new SignSynthesisContext(
                session_id,
                source_type,
                nextText,
                locale,
                sign_language,
                model_profile,
                output_format,
                protocol_version
        );
    }

    public SignSynthesisRequest toRequest(String audioB64) {
        return new SignSynthesisRequest(
                session_id,
                source_type,
                "speech".equals(source_type) ? null : text,
                "speech".equals(source_type) ? text : null,
                audioB64,
                locale,
                sign_language,
                model_profile,
                output_format,
                protocol_version
        );
    }
}
