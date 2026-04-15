package com.mj.sign;

public record GpuInferenceResponse(
        String session_id,
        String text,
        Boolean is_final,
        Number confidence,
        Number processing_time_ms,
        String model_version,
        String error
) {
}
