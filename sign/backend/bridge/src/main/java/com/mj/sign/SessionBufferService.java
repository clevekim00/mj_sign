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
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SessionBufferService {

    private final int minFramesForInference;
    private final int maxBufferedFrames;
    private final long idleTimeoutMillis;
    private final Clock clock;
    private final BridgeMetricsService metricsService;
    private final ConcurrentHashMap<String, BufferState> sessionBuffers = new ConcurrentHashMap<>();
    private final AtomicInteger totalBufferedFrames = new AtomicInteger(0);

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

    public BufferedChunkResult append(ClientStreamChunk incomingChunk) {
        long now = clock.millis();
        String sessionId = incomingChunk.getSessionId();
        BufferState state = sessionBuffers.computeIfAbsent(
                sessionId,
                ignored -> new BufferState()
        );
        
        synchronized (state) {
            int addedFrames = incomingChunk.getFramesCount();
            state.frames.addAll(incomingChunk.getFramesList());
            state.lastUpdatedAtMillis = now;
            state.scheduleToken++;
            totalBufferedFrames.addAndGet(addedFrames);

            if (state.frames.size() >= maxBufferedFrames) {
                ClientStreamChunk chunk = buildChunk(sessionId, state.frames);
                int bufferedFrameCount = state.frames.size();
                
                int keepFrom = Math.max(0, state.frames.size() - minFramesForInference);
                int removedFrames = keepFrom;
                state.frames = new ArrayList<>(state.frames.subList(keepFrom, state.frames.size()));
                totalBufferedFrames.addAndGet(-removedFrames);
                
                state.lastUpdatedAtMillis = now;
                updateMetrics();
                return new BufferedChunkResult(true, chunk, bufferedFrameCount, state.scheduleToken, false);
            }

            if (state.frames.size() >= minFramesForInference) {
                ClientStreamChunk chunk = buildChunk(sessionId, state.frames);
                int bufferedFrameCount = state.frames.size();
                sessionBuffers.remove(sessionId);
                totalBufferedFrames.addAndGet(-bufferedFrameCount);
                updateMetrics();
                return new BufferedChunkResult(true, chunk, bufferedFrameCount, state.scheduleToken, false);
            }

            updateMetrics();
            return new BufferedChunkResult(
                    false,
                    buildChunk(sessionId, state.frames),
                    state.frames.size(),
                    state.scheduleToken,
                    false
            );
        }
    }

    public Optional<BufferedChunkResult> flushIfIdle(String sessionId, long scheduleToken) {
        BufferState state = sessionBuffers.get(sessionId);
        if (state == null) {
            return Optional.empty();
        }

        synchronized (state) {
            if (state.frames.isEmpty() || state.scheduleToken != scheduleToken) {
                return Optional.empty();
            }

            long now = clock.millis();
            if ((now - state.lastUpdatedAtMillis) < idleTimeoutMillis) {
                return Optional.empty();
            }

            ClientStreamChunk chunk = buildChunk(sessionId, state.frames);
            int bufferedFrameCount = state.frames.size();
            sessionBuffers.remove(sessionId);
            totalBufferedFrames.addAndGet(-bufferedFrameCount);
            updateMetrics();
            return Optional.of(
                    new BufferedChunkResult(true, chunk, bufferedFrameCount, scheduleToken, true)
            );
        }
    }

    public void clear(String sessionId) {
        BufferState state = sessionBuffers.remove(sessionId);
        if (state != null) {
            synchronized (state) {
                totalBufferedFrames.addAndGet(-state.frames.size());
            }
            updateMetrics();
        }
    }

    public long idleTimeoutMillis() {
        return idleTimeoutMillis;
    }

    public int maxBufferedFrames() {
        return maxBufferedFrames;
    }

    private void updateMetrics() {
        metricsService.updateBufferState(sessionBuffers.size(), totalBufferedFrames.get());
    }

    private ClientStreamChunk buildChunk(String sessionId, List<LandmarkFrame> frames) {
        return ClientStreamChunk.newBuilder()
                .setSessionId(sessionId)
                .addAllFrames(frames)
                .build();
    }

    private static final class BufferState {
        private List<LandmarkFrame> frames = new ArrayList<>();
        private long lastUpdatedAtMillis;
        private long scheduleToken;
    }
}
