package com.mj.sign;

public interface KafkaBrokerPort {
    void publish(QueueBrokerMessage message);
}
