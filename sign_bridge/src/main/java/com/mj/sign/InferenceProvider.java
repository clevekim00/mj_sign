package com.mj.sign;

public enum InferenceProvider {
    HTTP("http"),
    GRPC("grpc"),
    QUEUE("queue");

    private final String configValue;

    InferenceProvider(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public static InferenceProvider fromConfig(String rawValue) {
        for (InferenceProvider provider : values()) {
            if (provider.configValue.equalsIgnoreCase(rawValue)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unsupported inference provider: " + rawValue);
    }
}
