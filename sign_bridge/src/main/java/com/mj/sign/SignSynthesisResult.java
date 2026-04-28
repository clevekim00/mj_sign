package com.mj.sign;

public record SignSynthesisResult(
        String session_id,
        String event_type,
        String source_type,
        String text,
        String locale,
        String sign_language,
        String model_profile,
        String protocol_version,
        SignSynthesisPlan sign_plan,
        SignSynthesisMotion motion,
        Boolean is_final,
        Number confidence,
        String error
) {
}
