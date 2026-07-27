package com.mj.sign;

public interface SignSynthesisProvider {
    SignSynthesisResult synthesize(SignSynthesisRequest request, SignSynthesisContext context);
}
