package com.mj.sign;

public interface RabbitMqBrokerPort {
    void publish(QueueBrokerMessage message);
}
