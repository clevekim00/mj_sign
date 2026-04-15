package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
import org.springframework.stereotype.Service;

@Service
public class GrpcInferenceGateway implements InferenceGateway {

    @Override
    public InferenceProvider provider() {
        return InferenceProvider.GRPC;
    }

    @Override
    public TranslationResult sendForInference(ClientStreamChunk chunk) {
        return TranslationResult.newBuilder()
                .setSessionId(chunk.getSessionId())
                .setText("gRPC inference provider is not implemented yet.")
                .setIsFinal(true)
                .setConfidence(0.0f)
                .build();
    }
}
