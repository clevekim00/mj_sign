package com.mj.sign;

public interface SignMotionGenerator {
    SignSynthesisMotion generate(SignSynthesisPlan plan, SignSynthesisContext context);
}
