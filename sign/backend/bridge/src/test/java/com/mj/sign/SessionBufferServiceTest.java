package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.LandmarkFrame;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionBufferServiceTest {

    @Test
    void accumulatesFramesUntilThreshold() {
        MutableClock clock = new MutableClock();
        SessionBufferService service = new SessionBufferService(3, 6, 1000, clock, new BridgeMetricsService());

        BufferedChunkResult first = service.append(chunk("s-1", 1));
        BufferedChunkResult second = service.append(chunk("s-1", 1));
        BufferedChunkResult third = service.append(chunk("s-1", 1));

        assertFalse(first.readyForInference());
        assertEquals(1, first.bufferedFrameCount());
        assertFalse(second.readyForInference());
        assertEquals(2, second.bufferedFrameCount());
        assertTrue(third.readyForInference());
        assertEquals(3, third.chunk().getFramesCount());
    }

    @Test
    void clearsBufferedFramesAfterInferenceThresholdIsReached() {
        MutableClock clock = new MutableClock();
        SessionBufferService service = new SessionBufferService(2, 5, 1000, clock, new BridgeMetricsService());

        BufferedChunkResult result = service.append(chunk("s-2", 2));
        BufferedChunkResult next = service.append(chunk("s-2", 1));

        assertTrue(result.readyForInference());
        assertEquals(2, result.chunk().getFramesCount());
        assertFalse(next.readyForInference());
        assertEquals(1, next.bufferedFrameCount());
    }

    @Test
    void flushesBufferedFramesAfterIdleTimeout() {
        MutableClock clock = new MutableClock();
        SessionBufferService service = new SessionBufferService(4, 8, 1000, clock, new BridgeMetricsService());

        BufferedChunkResult buffered = service.append(chunk("s-3", 2));
        assertFalse(buffered.readyForInference());

        clock.advanceMillis(1001);
        BufferedChunkResult flushed = service.flushIfIdle("s-3", buffered.scheduleToken()).orElseThrow();

        assertTrue(flushed.readyForInference());
        assertTrue(flushed.idleTimeoutTriggered());
        assertEquals(2, flushed.bufferedFrameCount());
    }

    @Test
    void ignoresOutdatedIdleFlushTokens() {
        MutableClock clock = new MutableClock();
        SessionBufferService service = new SessionBufferService(4, 8, 1000, clock, new BridgeMetricsService());

        BufferedChunkResult first = service.append(chunk("s-4", 1));
        service.append(chunk("s-4", 1));
        clock.advanceMillis(1001);

        assertTrue(service.flushIfIdle("s-4", first.scheduleToken()).isEmpty());
    }

    private ClientStreamChunk chunk(String sessionId, int frameCount) {
        ClientStreamChunk.Builder builder = ClientStreamChunk.newBuilder().setSessionId(sessionId);
        for (int index = 0; index < frameCount; index++) {
            builder.addFrames(LandmarkFrame.newBuilder().setTimestampMs(index).build());
        }
        return builder.build();
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-04-15T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advanceMillis(long millis) {
            instant = instant.plusMillis(millis);
        }
    }
}
