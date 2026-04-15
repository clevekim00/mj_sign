package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.time.Duration;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignWebSocketHandler extends BinaryWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(SignWebSocketHandler.class);
    private static final JsonFormat.Printer JSON_PRINTER = JsonFormat.printer()
            .preservingProtoFieldNames();

    private final ConcurrentHashMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> websocketToStreamSessionIds = new ConcurrentHashMap<>();
    private final SessionBufferService sessionBufferService;
    private final AsyncInferenceService asyncInferenceService;
    private final IdleFlushScheduler idleFlushScheduler;
    private final BridgeMetricsService metricsService;

    public SignWebSocketHandler(
            SessionBufferService sessionBufferService,
            AsyncInferenceService asyncInferenceService,
            IdleFlushScheduler idleFlushScheduler,
            BridgeMetricsService metricsService
    ) {
        this.sessionBufferService = sessionBufferService;
        this.asyncInferenceService = asyncInferenceService;
        this.idleFlushScheduler = idleFlushScheduler;
        this.metricsService = metricsService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Client connected: {}", session.getId());
        activeSessions.put(session.getId(), session);
        metricsService.incrementActiveWebSocketSessions();
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        metricsService.incrementReceivedMessages();
        try {
            ClientStreamChunk chunk = ClientStreamChunk.parseFrom(toByteArray(message.getPayload()));
            if (chunk.getSessionId().isBlank()) {
                sendResult(session, errorResult("missing-session", "session_id is required."));
                return;
            }

            log.info("Received {} frames from session {}", chunk.getFramesCount(), chunk.getSessionId());
            websocketToStreamSessionIds.put(session.getId(), chunk.getSessionId());
            BufferedChunkResult buffered = sessionBufferService.append(chunk);
            if (!buffered.readyForInference()) {
                sendResult(session, bufferingResult(chunk.getSessionId(), buffered.bufferedFrameCount()));
                scheduleIdleFlush(session, chunk.getSessionId(), buffered.scheduleToken());
                return;
            }

            dispatchBufferedChunk(session, chunk.getSessionId(), buffered);
        } catch (InvalidProtocolBufferException e) {
            log.warn("Failed to parse protobuf message from {}", session.getId(), e);
            metricsService.incrementPayloadErrors();
            sendResult(session, errorResult("invalid-payload", "Failed to parse protobuf payload."));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Client disconnected: {}", session.getId());
        activeSessions.remove(session.getId());
        metricsService.decrementActiveWebSocketSessions();
        String streamSessionId = websocketToStreamSessionIds.remove(session.getId());
        if (streamSessionId != null) {
            sessionBufferService.clear(streamSessionId);
        }
    }

    private void sendResult(WebSocketSession session, TranslationResult result) throws Exception {
        session.sendMessage(new TextMessage(JSON_PRINTER.print(result)));
    }

    private void safeSendResult(WebSocketSession session, TranslationResult result) {
        if (!session.isOpen()) {
            return;
        }
        try {
            sendResult(session, result);
        } catch (Exception e) {
            log.warn("Failed to send async result to session {}", session.getId(), e);
        }
    }

    private TranslationResult errorResult(String sessionId, String text) {
        return TranslationResult.newBuilder()
                .setSessionId(sessionId)
                .setText(text)
                .setIsFinal(true)
                .setConfidence(0.0f)
                .build();
    }

    private TranslationResult bufferingResult(String sessionId, int bufferedFrameCount) {
        return TranslationResult.newBuilder()
                .setSessionId(sessionId)
                .setText("Buffering " + bufferedFrameCount + " frames before inference.")
                .setIsFinal(false)
                .setConfidence(0.0f)
                .build();
    }

    private TranslationResult processingResult(String sessionId, int bufferedFrameCount) {
        return TranslationResult.newBuilder()
                .setSessionId(sessionId)
                .setText("Processing " + bufferedFrameCount + " buffered frames.")
                .setIsFinal(false)
                .setConfidence(0.0f)
                .build();
    }

    private TranslationResult busyResult(String sessionId) {
        return TranslationResult.newBuilder()
                .setSessionId(sessionId)
                .setText("Inference already in progress for this session.")
                .setIsFinal(false)
                .setConfidence(0.0f)
                .build();
    }

    private TranslationResult idleFlushResult(String sessionId, int bufferedFrameCount) {
        return TranslationResult.newBuilder()
                .setSessionId(sessionId)
                .setText("Idle timeout reached. Flushing " + bufferedFrameCount + " buffered frames.")
                .setIsFinal(false)
                .setConfidence(0.0f)
                .build();
    }

    private void scheduleIdleFlush(WebSocketSession session, String streamSessionId, long scheduleToken) {
        idleFlushScheduler.schedule(
                streamSessionId,
                scheduleToken,
                Duration.ofMillis(sessionBufferService.idleTimeoutMillis()),
                () -> sessionBufferService.flushIfIdle(streamSessionId, scheduleToken)
                        .ifPresent(buffered -> {
                            WebSocketSession activeSession = activeSessions.get(session.getId());
                            if (activeSession == null || !activeSession.isOpen()) {
                                return;
                            }
                            metricsService.incrementIdleFlushTriggered();
                            safeSendResult(activeSession, idleFlushResult(streamSessionId, buffered.bufferedFrameCount()));
                            dispatchBufferedChunk(activeSession, streamSessionId, buffered);
                        })
        );
    }

    private void dispatchBufferedChunk(WebSocketSession session, String streamSessionId, BufferedChunkResult buffered) {
        boolean accepted = asyncInferenceService.dispatch(
                streamSessionId,
                buffered.chunk(),
                result -> safeSendResult(session, result)
        );
        if (accepted) {
            if (!buffered.idleTimeoutTriggered()) {
                safeSendResult(session, processingResult(streamSessionId, buffered.bufferedFrameCount()));
            }
        } else {
            safeSendResult(session, busyResult(streamSessionId));
        }
    }

    private byte[] toByteArray(ByteBuffer buffer) {
        ByteBuffer copy = buffer.asReadOnlyBuffer();
        byte[] payload = new byte[copy.remaining()];
        copy.get(payload);
        return payload;
    }
}
