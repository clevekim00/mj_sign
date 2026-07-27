package com.mj.sign;

public interface SpeechToTextAdapter {
    SpeechRecognitionResult transcribe(SignSynthesisRequest request, SignSynthesisContext context);
}
