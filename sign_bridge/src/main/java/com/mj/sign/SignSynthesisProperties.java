package com.mj.sign;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sign.synthesis")
public class SignSynthesisProperties {
    private String provider = "mock";
    private String asrProvider = "mock";
    private String baseUrl = "http://localhost:8010";
    private String asrBaseUrl = "";
    private String synthesizePath = "/api/v2/sign/synthesize";
    private String asrPath = "/api/v2/speech/transcribe";
    private String outputFormat = "landmarks";
    private String protocolVersion = SignSynthesisService.PROTOCOL_VERSION;
    private long timeoutMs = 3000;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAsrProvider() {
        return asrProvider;
    }

    public void setAsrProvider(String asrProvider) {
        this.asrProvider = asrProvider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAsrBaseUrl() {
        return asrBaseUrl == null || asrBaseUrl.isBlank() ? baseUrl : asrBaseUrl;
    }

    public void setAsrBaseUrl(String asrBaseUrl) {
        this.asrBaseUrl = asrBaseUrl;
    }

    public String getSynthesizePath() {
        return synthesizePath;
    }

    public void setSynthesizePath(String synthesizePath) {
        this.synthesizePath = synthesizePath;
    }

    public String getAsrPath() {
        return asrPath;
    }

    public void setAsrPath(String asrPath) {
        this.asrPath = asrPath;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
