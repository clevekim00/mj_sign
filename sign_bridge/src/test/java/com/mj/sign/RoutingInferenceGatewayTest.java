package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoutingInferenceGatewayTest {

    @Test
    void routesToConfiguredHttpProvider() {
        GpuServingProperties properties = properties("http");
        RoutingInferenceGateway gateway = new RoutingInferenceGateway(
                properties,
                List.of(
                        fixedGateway(InferenceProvider.HTTP, "http-result"),
                        fixedGateway(InferenceProvider.GRPC, "grpc-result"),
                        fixedGateway(InferenceProvider.QUEUE, "queue-result")
                )
        );

        TranslationResult result = gateway.sendForInference(chunk("s-http"));

        assertEquals("http-result", result.getText());
        assertEquals(InferenceProvider.HTTP, gateway.provider());
    }

    @Test
    void routesToConfiguredGrpcProvider() {
        GpuServingProperties properties = properties("grpc");
        RoutingInferenceGateway gateway = new RoutingInferenceGateway(
                properties,
                List.of(
                        fixedGateway(InferenceProvider.HTTP, "http-result"),
                        fixedGateway(InferenceProvider.GRPC, "grpc-result")
                )
        );

        TranslationResult result = gateway.sendForInference(chunk("s-grpc"));

        assertEquals("grpc-result", result.getText());
    }

    @Test
    void failsFastForUnsupportedProviderValue() {
        RoutingInferenceGateway gateway = new RoutingInferenceGateway(
                properties("unknown"),
                List.of(fixedGateway(InferenceProvider.HTTP, "http-result"))
        );

        assertThrows(IllegalArgumentException.class, gateway::provider);
    }

    private ClientStreamChunk chunk(String sessionId) {
        return ClientStreamChunk.newBuilder().setSessionId(sessionId).build();
    }

    private GpuServingProperties properties(String provider) {
        GpuServingProperties properties = new GpuServingProperties();
        properties.setProvider(provider);
        return properties;
    }

    private InferenceGateway fixedGateway(InferenceProvider provider, String text) {
        return new InferenceGateway() {
            @Override
            public InferenceProvider provider() {
                return provider;
            }

            @Override
            public TranslationResult sendForInference(ClientStreamChunk chunk) {
                return TranslationResult.newBuilder()
                        .setSessionId(chunk.getSessionId())
                        .setText(text)
                        .setIsFinal(true)
                        .setConfidence(1.0f)
                        .build();
            }
        };
    }
}
