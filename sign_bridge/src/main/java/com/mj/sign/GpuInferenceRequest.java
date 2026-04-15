package com.mj.sign;

public record GpuInferenceRequest(
        String session_id,
        String protobuf_b64,
        int frame_count,
        String transport,
        String client_schema_version
) {
}
