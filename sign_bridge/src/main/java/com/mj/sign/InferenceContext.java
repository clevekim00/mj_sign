package com.mj.sign;

public record InferenceContext(
        String locale,
        String sign_language,
        String model_profile,
        String protocol_version
) {
    public static final String DEFAULT_PROTOCOL_VERSION = "mj-sign-model-v1";

    public static InferenceContext defaults() {
        return new InferenceContext("ko-KR", "ksl", "sign-gemma-ko", DEFAULT_PROTOCOL_VERSION);
    }
}
