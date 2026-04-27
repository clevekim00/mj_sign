package com.mj.sign;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
import com.google.protobuf.InvalidProtocolBufferException;
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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignWebSocketHandler extends BinaryWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(SignWebSocketHandler.class);

    private final ConcurrentHashMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> websocketToStreamSessionIds = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final SessionBufferService sessionBufferService;
    private final AsyncInferenceService asyncInferenceService;
    private final IdleFlushScheduler idleFlushScheduler;
    private final BridgeMetricsService metricsService;

    public SignWebSocketHandler(
            SessionBufferService sessionBufferService,
            AsyncInferenceService asyncInferenceService,
            IdleFlushScheduler idleFlushScheduler,
            BridgeMetricsService metricsService,
            ObjectMapper objectMapper
    ) {
        this.sessionBufferService = sessionBufferService;
        this.asyncInferenceService = asyncInferenceService;
        this.idleFlushScheduler = idleFlushScheduler;
        this.metricsService = metricsService;
        this.objectMapper = objectMapper;
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
                sendError(session, "missing-session", "missing-session", "session_id is required.");
                return;
            }

            log.info("Received {} frames from session {}", chunk.getFramesCount(), chunk.getSessionId());
            websocketToStreamSessionIds.put(session.getId(), chunk.getSessionId());
            BufferedChunkResult buffered = sessionBufferService.append(chunk);
            if (!buffered.readyForInference()) {
                sendStatus(
                        session,
                        chunk.getSessionId(),
                        "buffering",
                        "Buffering " + buffered.bufferedFrameCount() + " frames before inference."
                );
                scheduleIdleFlush(session, chunk.getSessionId(), buffered.scheduleToken());
                return;
            }

            dispatchBufferedChunk(session, chunk.getSessionId(), buffered);
        } catch (InvalidProtocolBufferException e) {
            log.warn("Failed to parse protobuf message from {}", session.getId(), e);
            metricsService.incrementPayloadErrors();
            sendError(session, "invalid-payload", "invalid-payload", "Failed to parse protobuf payload.");
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

    private void sendRecognitionResult(WebSocketSession session, TranslationResult result) throws Exception {
        session.sendMessage(new TextMessage(toJson(resultEvent(result))));
    }

    private void sendStatus(WebSocketSession session, String sessionId, String status, String text) throws Exception {
        session.sendMessage(new TextMessage(toJson(statusEvent(sessionId, status, text))));
    }

    private void sendError(WebSocketSession session, String sessionId, String errorCode, String text) throws Exception {
        session.sendMessage(new TextMessage(toJson(errorEvent(sessionId, errorCode, text))));
    }

    private void safeSendInferenceEvent(WebSocketSession session, TranslationResult result) {
        if (!session.isOpen()) {
            return;
        }
        try {
            if (isInferenceError(result)) {
                sendError(session, result.getSessionId(), "inference-error", result.getText());
            } else {
                sendRecognitionResult(session, result);
            }
        } catch (Exception e) {
            log.warn("Failed to send async result to session {}", session.getId(), e);
        }
    }

    private void safeSendStatus(WebSocketSession session, String sessionId, String status, String text) {
        if (!session.isOpen()) {
            return;
        }
        try {
            sendStatus(session, sessionId, status, text);
        } catch (Exception e) {
            log.warn("Failed to send status to session {}", session.getId(), e);
        }
    }

    private Map<String, Object> resultEvent(TranslationResult result) {
        Map<String, Object> event = baseEvent(result.getSessionId(), "result");
        event.put("result_text", result.getText());
        event.put("text", result.getText());
        event.put("is_final", result.getIsFinal());
        event.put("confidence", result.getConfidence());
        return event;
    }

    private Map<String, Object> statusEvent(String sessionId, String status, String text) {
        Map<String, Object> event = baseEvent(sessionId, "status");
        event.put("status", status);
        event.put("status_text", text);
        event.put("is_final", false);
        event.put("confidence", 0.0f);
        return event;
    }

    private Map<String, Object> errorEvent(String sessionId, String errorCode, String text) {
        Map<String, Object> event = baseEvent(sessionId, "error");
        event.put("error_code", errorCode);
        event.put("status_text", text);
        event.put("is_final", true);
        event.put("confidence", 0.0f);
        return event;
    }

    private Map<String, Object> baseEvent(String sessionId, String eventType) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("session_id", sessionId);
        event.put("event_type", eventType);
        return event;
    }

    private String toJson(Map<String, Object> event) throws JsonProcessingException {
        return objectMapper.writeValueAsString(event);
    }

    private boolean isInferenceError(TranslationResult result) {
        if (!result.getIsFinal() || result.getConfidence() > 0.0f || result.getText().isBlank()) {
            return false;
        }
        String normalized = result.getText().toLowerCase(Locale.ROOT);
        return normalized.startsWith("failed ")
                || normalized.startsWith("async inference failed")
                || normalized.startsWith("queue transport ")
                || normalized.startsWith("no inference gateway registered");
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
                            safeSendStatus(
                                    activeSession,
                                    streamSessionId,
                                    "idle_flush",
                                    "Idle timeout reached. Flushing " + buffered.bufferedFrameCount() + " buffered frames."
                            );
                            dispatchBufferedChunk(activeSession, streamSessionId, buffered);
                        })
        );
    }

    private void dispatchBufferedChunk(WebSocketSession session, String streamSessionId, BufferedChunkResult buffered) {
        boolean accepted = asyncInferenceService.dispatch(
                streamSessionId,
                buffered.chunk(),
                result -> safeSendInferenceEvent(session, result)
        );
        if (accepted) {
            if (!buffered.idleTimeoutTriggered()) {
                safeSendStatus(
                        session,
                        streamSessionId,
                        "processing",
                        "Processing " + buffered.bufferedFrameCount() + " buffered frames."
                );
            }
        } else {
            safeSendStatus(
                    session,
                    streamSessionId,
                    "busy",
                    "Inference already in progress for this session."
            );
        }
    }

    private byte[] toByteArray(ByteBuffer buffer) {
        ByteBuffer copy = buffer.asReadOnlyBuffer();
        byte[] payload = new byte[copy.remaining()];
        copy.get(payload);
        return payload;
    }
}
