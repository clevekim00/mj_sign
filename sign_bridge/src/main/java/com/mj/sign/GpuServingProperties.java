package com.mj.sign;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sign.gpu")
public class GpuServingProperties {

    private String provider = "http";
    private String baseUrl = "http://localhost:8000";
    private String inferPath = "/api/v2/recognize";
    private String healthPath = "/";
    private String grpcTarget = "dns:///localhost:50051";
    private String queueTransport = "in-memory";
    private String queueBrokerMode = "loopback";
    private String queueTopic = "sign.inference.requests";
    private String queueRequestTopic = "sign.inference.requests";
    private String queueResultTopic = "sign.inference.results";
    private String queueConsumerGroup = "sign-bridge";
    private String queueExchange = "sign.inference";
    private String queueRoutingKey = "sign.inference.request";
    private String queueResultRoutingKey = "sign.inference.result";
    private long queueTimeoutMs = 5000;
    private long timeoutMs = 2000;
    private long probeTimeoutMs = 1000;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getInferPath() {
        return inferPath;
    }

    public void setInferPath(String inferPath) {
        this.inferPath = inferPath;
    }

    public String getHealthPath() {
        return healthPath;
    }

    public void setHealthPath(String healthPath) {
        this.healthPath = healthPath;
    }

    public String getGrpcTarget() {
        return grpcTarget;
    }

    public void setGrpcTarget(String grpcTarget) {
        this.grpcTarget = grpcTarget;
    }

    public String getQueueTopic() {
        return queueTopic;
    }

    public void setQueueTopic(String queueTopic) {
        this.queueTopic = queueTopic;
    }

    public String getQueueTransport() {
        return queueTransport;
    }

    public void setQueueTransport(String queueTransport) {
        this.queueTransport = queueTransport;
    }

    public String getQueueBrokerMode() {
        return queueBrokerMode;
    }

    public void setQueueBrokerMode(String queueBrokerMode) {
        this.queueBrokerMode = queueBrokerMode;
    }

    public String getQueueRequestTopic() {
        return queueRequestTopic;
    }

    public void setQueueRequestTopic(String queueRequestTopic) {
        this.queueRequestTopic = queueRequestTopic;
    }

    public String getQueueResultTopic() {
        return queueResultTopic;
    }

    public void setQueueResultTopic(String queueResultTopic) {
        this.queueResultTopic = queueResultTopic;
    }

    public String getQueueConsumerGroup() {
        return queueConsumerGroup;
    }

    public void setQueueConsumerGroup(String queueConsumerGroup) {
        this.queueConsumerGroup = queueConsumerGroup;
    }

    public String getQueueExchange() {
        return queueExchange;
    }

    public void setQueueExchange(String queueExchange) {
        this.queueExchange = queueExchange;
    }

    public String getQueueRoutingKey() {
        return queueRoutingKey;
    }

    public void setQueueRoutingKey(String queueRoutingKey) {
        this.queueRoutingKey = queueRoutingKey;
    }

    public String getQueueResultRoutingKey() {
        return queueResultRoutingKey;
    }

    public void setQueueResultRoutingKey(String queueResultRoutingKey) {
        this.queueResultRoutingKey = queueResultRoutingKey;
    }

    public long getQueueTimeoutMs() {
        return queueTimeoutMs;
    }

    public void setQueueTimeoutMs(long queueTimeoutMs) {
        this.queueTimeoutMs = queueTimeoutMs;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public long getProbeTimeoutMs() {
        return probeTimeoutMs;
    }

    public void setProbeTimeoutMs(long probeTimeoutMs) {
        this.probeTimeoutMs = probeTimeoutMs;
    }
}
