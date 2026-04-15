package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Primary
@Service
public class RoutingInferenceGateway implements InferenceGateway {

    private final GpuServingProperties properties;
    private final Map<InferenceProvider, InferenceGateway> gateways;

    public RoutingInferenceGateway(
            GpuServingProperties properties,
            List<InferenceGateway> gatewayImplementations
    ) {
        this.properties = properties;
        this.gateways = new EnumMap<>(InferenceProvider.class);
        for (InferenceGateway gateway : gatewayImplementations) {
            if (gateway instanceof RoutingInferenceGateway) {
                continue;
            }
            gateways.put(gateway.provider(), gateway);
        }
    }

    @Override
    public InferenceProvider provider() {
        return selectedProvider();
    }

    @Override
    public TranslationResult sendForInference(ClientStreamChunk chunk) {
        InferenceGateway delegate = gateways.get(selectedProvider());
        if (delegate == null) {
            return TranslationResult.newBuilder()
                    .setSessionId(chunk.getSessionId())
                    .setText("No inference gateway registered for provider: " + properties.getProvider())
                    .setIsFinal(true)
                    .setConfidence(0.0f)
                    .build();
        }
        return delegate.sendForInference(chunk);
    }

    InferenceProvider selectedProvider() {
        return InferenceProvider.fromConfig(properties.getProvider());
    }
}
