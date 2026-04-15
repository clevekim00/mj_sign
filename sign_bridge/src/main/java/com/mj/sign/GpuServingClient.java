package com.mj.sign;

public interface GpuServingClient {
    GpuInferenceResponse infer(GpuInferenceRequest request);
}
