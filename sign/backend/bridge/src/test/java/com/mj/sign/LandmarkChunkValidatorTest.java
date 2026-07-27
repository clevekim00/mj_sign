package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.LandmarkFrame;
import com.mj.sign.protos.LandmarkProto.Point3D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LandmarkChunkValidatorTest {
    @Test
    void rejectsTimestampRegression() {
        ClientStreamChunk chunk = ClientStreamChunk.newBuilder()
                .addFrames(LandmarkFrame.newBuilder().setTimestampMs(20))
                .addFrames(LandmarkFrame.newBuilder().setTimestampMs(10))
                .build();

        LandmarkChunkValidator.ValidationError error =
                LandmarkChunkValidator.validate(chunk, 512, 10.0f).orElseThrow();

        assertEquals("timestamp-regression", error.code());
    }

    @Test
    void rejectsNonFiniteAndOutOfRangeCoordinates() {
        ClientStreamChunk nonFinite = chunkWithPoint(Float.NaN);
        ClientStreamChunk outOfRange = chunkWithPoint(11.0f);

        assertEquals(
                "invalid-coordinate",
                LandmarkChunkValidator.validate(nonFinite, 512, 10.0f).orElseThrow().code()
        );
        assertEquals(
                "coordinate-out-of-range",
                LandmarkChunkValidator.validate(outOfRange, 512, 10.0f).orElseThrow().code()
        );
    }

    @Test
    void acceptsFiniteNormalizedCoordinates() {
        assertTrue(LandmarkChunkValidator.validate(chunkWithPoint(0.5f), 512, 10.0f).isEmpty());
    }

    private ClientStreamChunk chunkWithPoint(float x) {
        return ClientStreamChunk.newBuilder()
                .addFrames(LandmarkFrame.newBuilder()
                        .setTimestampMs(1)
                        .addLeftHand(Point3D.newBuilder().setX(x).setY(0.2f).setZ(-0.1f)))
                .build();
    }
}
