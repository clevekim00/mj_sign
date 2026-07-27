package com.mj.sign;

import java.util.Locale;

public record InferenceContext(
        String locale,
        String sign_language,
        String model_profile,
        String protocol_version
) {
    public static final String DEFAULT_PROTOCOL_VERSION = "signbridge-model-v1";
    public static final String LEGACY_PROTOCOL_VERSION = "mj-sign-model-v1";

    public InferenceContext {
        protocol_version = normalizeProtocolVersion(protocol_version);
    }

    public static InferenceContext defaults() {
        return new InferenceContext("ko-KR", "ksl", "sign-gemma-ko", DEFAULT_PROTOCOL_VERSION);
    }

    public static String normalizeProtocolVersion(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_PROTOCOL_VERSION;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (LEGACY_PROTOCOL_VERSION.equals(normalized)) {
            return DEFAULT_PROTOCOL_VERSION;
        }
        return normalized;
    }
}
