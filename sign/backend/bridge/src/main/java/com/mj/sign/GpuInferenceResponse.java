package com.mj.sign;

public record GpuInferenceResponse(
        String session_id,
        String text,
        Boolean is_final,
        Number confidence,
        Number processing_time_ms,
        String model_version,
        String error,
        String protocol_version,
        String locale,
        String sign_language,
        String model_profile
) {
    public GpuInferenceResponse(
            String session_id,
            String text,
            Boolean is_final,
            Number confidence,
            Number processing_time_ms,
            String model_version,
            String error
    ) {
        this(session_id, text, is_final, confidence, processing_time_ms, model_version, error, null, null, null, null);
    }
}
