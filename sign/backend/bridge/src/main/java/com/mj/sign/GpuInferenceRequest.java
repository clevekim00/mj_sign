package com.mj.sign;

public record GpuInferenceRequest(
        String session_id,
        String protobuf_b64,
        int frame_count,
        String transport,
        String client_schema_version,
        String protocol_version,
        String locale,
        String sign_language,
        String model_profile
) {
    public GpuInferenceRequest(
            String session_id,
            String protobuf_b64,
            int frame_count,
            String transport,
            String client_schema_version
    ) {
        this(
                session_id,
                protobuf_b64,
                frame_count,
                transport,
                client_schema_version,
                InferenceContext.defaults().protocol_version(),
                InferenceContext.defaults().locale(),
                InferenceContext.defaults().sign_language(),
                InferenceContext.defaults().model_profile()
        );
    }
}
