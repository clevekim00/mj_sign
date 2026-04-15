package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpInferenceGatewayTest {

    @Test
    void mapsStructuredServingResponse() {
        HttpInferenceGateway gateway = new HttpInferenceGateway(
                request -> new GpuInferenceResponse(
                        request.session_id(),
                        "translated",
                        true,
                        0.95,
                        321,
                        "sign-gemma-prod-v1",
                        null
                ),
                properties()
        );
        ClientStreamChunk chunk = ClientStreamChunk.newBuilder().setSessionId("session-1").build();

        TranslationResult result = gateway.sendForInference(chunk);

        assertEquals("session-1", result.getSessionId());
        assertEquals("translated", result.getText());
        assertTrue(result.getIsFinal());
        assertEquals(0.95f, result.getConfidence());
    }

    @Test
    void buildsExpectedServingRequestEnvelope() {
        HttpInferenceGateway gateway = new HttpInferenceGateway(
                request -> new GpuInferenceResponse(request.session_id(), "", true, 0, 0, null, null),
                properties()
        );
        ClientStreamChunk chunk = ClientStreamChunk.newBuilder().setSessionId("session-2").build();

        GpuInferenceRequest request = gateway.toRequest(chunk);

        assertEquals("session-2", request.session_id());
        assertEquals("protobuf-b64", request.transport());
        assertEquals("v1", request.client_schema_version());
    }

    @Test
    void turnsServingErrorIntoTranslationError() {
        HttpInferenceGateway gateway = new HttpInferenceGateway(
                request -> new GpuInferenceResponse(request.session_id(), null, true, 0, 0, null, "backend failed"),
                properties()
        );

        TranslationResult result = gateway.sendForInference(
                ClientStreamChunk.newBuilder().setSessionId("session-3").build()
        );

        assertEquals("backend failed", result.getText());
        assertTrue(result.getIsFinal());
    }

    private GpuServingProperties properties() {
        GpuServingProperties properties = new GpuServingProperties();
        properties.setBaseUrl("http://gpu.internal");
        properties.setInferPath("/infer");
        properties.setHealthPath("/healthz");
        properties.setProvider("http");
        properties.setTimeoutMs(2000);
        properties.setProbeTimeoutMs(1000);
        return properties;
    }
}
