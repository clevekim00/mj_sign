package com.mj.sign;

public enum QueueTransportKind {
    IN_MEMORY("in-memory"),
    KAFKA("kafka"),
    RABBITMQ("rabbitmq");

    private final String configValue;

    QueueTransportKind(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public static QueueTransportKind fromConfig(String rawValue) {
        for (QueueTransportKind kind : values()) {
            if (kind.configValue.equalsIgnoreCase(rawValue)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unsupported queue transport: " + rawValue);
    }
}
