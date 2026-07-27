package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.TranslationResult;

public interface InferenceGateway {
    InferenceProvider provider();

    TranslationResult sendForInference(ClientStreamChunk chunk);

    default TranslationResult sendForInference(ClientStreamChunk chunk, InferenceContext context) {
        return sendForInference(chunk);
    }
}
