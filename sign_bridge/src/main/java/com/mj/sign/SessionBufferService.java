package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.LandmarkFrame;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionBufferService {

    private final int minFramesForInference;
    private final int maxBufferedFrames;
    private final long idleTimeoutMillis;
    private final Clock clock;
    private final BridgeMetricsService metricsService;
    private final ConcurrentHashMap<String, BufferState> sessionBuffers = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public SessionBufferService(
            @Value("${sign.window.min-frames:8}") int minFramesForInference,
            @Value("${sign.window.max-buffered-frames:24}") int maxBufferedFrames,
            @Value("${sign.window.idle-timeout-ms:1200}") long idleTimeoutMillis,
            BridgeMetricsService metricsService
    ) {
        this(minFramesForInference, maxBufferedFrames, idleTimeoutMillis, Clock.systemUTC(), metricsService);
    }

    SessionBufferService(
            int minFramesForInference,
            int maxBufferedFrames,
            long idleTimeoutMillis,
            Clock clock,
            BridgeMetricsService metricsService
    ) {
        this.minFramesForInference = minFramesForInference;
        this.maxBufferedFrames = maxBufferedFrames;
        this.idleTimeoutMillis = idleTimeoutMillis;
        this.clock = clock;
        this.metricsService = metricsService;
    }

    public synchronized BufferedChunkResult append(ClientStreamChunk incomingChunk) {
        long now = clock.millis();
        BufferState state = sessionBuffers.computeIfAbsent(
                incomingChunk.getSessionId(),
                ignored -> new BufferState()
        );
        state.frames.addAll(incomingChunk.getFramesList());
        state.lastUpdatedAtMillis = now;
        state.scheduleToken++;

        if (state.frames.size() >= maxBufferedFrames) {
            ClientStreamChunk chunk = buildChunk(incomingChunk.getSessionId(), state.frames);
            int bufferedFrameCount = state.frames.size();
            state.frames = trimTail(state.frames);
            state.lastUpdatedAtMillis = now;
            updateMetrics();
            return new BufferedChunkResult(true, chunk, bufferedFrameCount, state.scheduleToken, false);
        }

        if (state.frames.size() >= minFramesForInference) {
            ClientStreamChunk chunk = buildChunk(incomingChunk.getSessionId(), state.frames);
            int bufferedFrameCount = state.frames.size();
            sessionBuffers.remove(incomingChunk.getSessionId());
            updateMetrics();
            return new BufferedChunkResult(true, chunk, bufferedFrameCount, state.scheduleToken, false);
        }

        updateMetrics();
        return new BufferedChunkResult(
                false,
                buildChunk(incomingChunk.getSessionId(), state.frames),
                state.frames.size(),
                state.scheduleToken,
                false
        );
    }

    public synchronized Optional<BufferedChunkResult> flushIfIdle(String sessionId, long scheduleToken) {
        BufferState state = sessionBuffers.get(sessionId);
        if (state == null || state.frames.isEmpty() || state.scheduleToken != scheduleToken) {
            return Optional.empty();
        }

        long now = clock.millis();
        if ((now - state.lastUpdatedAtMillis) < idleTimeoutMillis) {
            return Optional.empty();
        }

        ClientStreamChunk chunk = buildChunk(sessionId, state.frames);
        int bufferedFrameCount = state.frames.size();
        sessionBuffers.remove(sessionId);
        updateMetrics();
        return Optional.of(
                new BufferedChunkResult(true, chunk, bufferedFrameCount, scheduleToken, true)
        );
    }

    public synchronized void clear(String sessionId) {
        sessionBuffers.remove(sessionId);
        updateMetrics();
    }

    public long idleTimeoutMillis() {
        return idleTimeoutMillis;
    }

    private void updateMetrics() {
        metricsService.updateBufferState(sessionBuffers.size(), totalBufferedFrames());
    }

    private int totalBufferedFrames() {
        return sessionBuffers.values().stream()
                .mapToInt(state -> state.frames.size())
                .sum();
    }

    private ClientStreamChunk buildChunk(String sessionId, List<LandmarkFrame> frames) {
        return ClientStreamChunk.newBuilder()
                .setSessionId(sessionId)
                .addAllFrames(frames)
                .build();
    }

    private List<LandmarkFrame> trimTail(List<LandmarkFrame> frames) {
        int keepFrom = Math.max(0, frames.size() - minFramesForInference);
        return new ArrayList<>(frames.subList(keepFrom, frames.size()));
    }

    private static final class BufferState {
        private List<LandmarkFrame> frames = new ArrayList<>();
        private long lastUpdatedAtMillis;
        private long scheduleToken;
    }
}
