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

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignWebSocketHandler extends BinaryWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(SignWebSocketHandler.class);
    private static final String STREAM_PROTOCOL_V1 = "signbridge-stream-v1";
    private static final String STREAM_PROTOCOL_V2 = "signbridge-stream-v2";
    private static final String STREAM_SCHEMA_V2 = "mj.sign.ClientStreamChunk/v2";

    private final ConcurrentHashMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> websocketToStreamSessionIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> streamSessionOwners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, InferenceContext> websocketToInferenceContexts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> websocketToStreamProtocols = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> websocketToLastChunkSequences = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> websocketToAcceptedChunkIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MessageRateWindow> websocketRateWindows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final SessionBufferService sessionBufferService;
    private final AsyncInferenceService asyncInferenceService;
    private final IdleFlushScheduler idleFlushScheduler;
    private final BridgeMetricsService metricsService;
    private final SignLanguageResolver signLanguageResolver;
    private final int maxBinaryMessageBytes;
    private final int maxSessionIdLength;
    private final int maxMessagesPerSecond;
    private final int maxPointsPerLandmarkGroup;
    private final float maxAbsoluteCoordinate;

    public SignWebSocketHandler(
            SessionBufferService sessionBufferService,
            AsyncInferenceService asyncInferenceService,
            IdleFlushScheduler idleFlushScheduler,
            BridgeMetricsService metricsService,
            ObjectMapper objectMapper
    ) {
        this(
                sessionBufferService,
                asyncInferenceService,
                idleFlushScheduler,
                metricsService,
                objectMapper,
                new SignLanguageResolver(new SignLanguageProperties()),
                1_048_576,
                128,
                60,
                512,
                10.0f
        );
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SignWebSocketHandler(
            SessionBufferService sessionBufferService,
            AsyncInferenceService asyncInferenceService,
            IdleFlushScheduler idleFlushScheduler,
            BridgeMetricsService metricsService,
            ObjectMapper objectMapper,
            SignLanguageResolver signLanguageResolver,
            @org.springframework.beans.factory.annotation.Value("${sign.websocket.max-binary-message-bytes:1048576}")
            int maxBinaryMessageBytes,
            @org.springframework.beans.factory.annotation.Value("${sign.websocket.max-session-id-length:128}")
            int maxSessionIdLength,
            @org.springframework.beans.factory.annotation.Value("${sign.websocket.max-messages-per-second:60}")
            int maxMessagesPerSecond,
            @org.springframework.beans.factory.annotation.Value("${sign.websocket.max-points-per-landmark-group:512}")
            int maxPointsPerLandmarkGroup,
            @org.springframework.beans.factory.annotation.Value("${sign.websocket.max-absolute-coordinate:10.0}")
            float maxAbsoluteCoordinate
    ) {
        this.sessionBufferService = sessionBufferService;
        this.asyncInferenceService = asyncInferenceService;
        this.idleFlushScheduler = idleFlushScheduler;
        this.metricsService = metricsService;
        this.objectMapper = objectMapper;
        this.signLanguageResolver = signLanguageResolver;
        this.maxBinaryMessageBytes = maxBinaryMessageBytes;
        this.maxSessionIdLength = maxSessionIdLength;
        this.maxMessagesPerSecond = Math.max(1, maxMessagesPerSecond);
        this.maxPointsPerLandmarkGroup = Math.max(1, maxPointsPerLandmarkGroup);
        this.maxAbsoluteCoordinate = Math.max(0.01f, maxAbsoluteCoordinate);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        InferenceContext context;
        try {
            context = resolveInferenceContext(session.getUri());
            websocketToStreamProtocols.put(session.getId(), resolveStreamProtocol(session.getUri()));
        } catch (IllegalArgumentException error) {
            metricsService.incrementPayloadErrors();
            sendError(session, "connection", "unsupported-profile", error.getMessage());
            session.close(CloseStatus.BAD_DATA.withReason(error.getMessage()));
            return;
        }
        log.info("Client connected: {} with inference context {}", session.getId(), context);
        activeSessions.put(session.getId(), session);
        websocketToInferenceContexts.put(session.getId(), context);
        metricsService.incrementActiveWebSocketSessions();
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        if (!allowMessage(session.getId())) {
            metricsService.incrementPayloadErrors();
            sendError(
                    session,
                    "connection",
                    "rate-limit",
                    "WebSocket message rate exceeds " + maxMessagesPerSecond + " messages per second."
            );
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        metricsService.incrementReceivedMessages();
        if (message.getPayloadLength() > maxBinaryMessageBytes) {
            metricsService.incrementPayloadErrors();
            sendError(
                    session,
                    "connection",
                    "payload-too-large",
                    "Binary message exceeds the maximum size of " + maxBinaryMessageBytes + " bytes."
            );
            session.close(CloseStatus.TOO_BIG_TO_PROCESS);
            return;
        }
        try {
            ClientStreamChunk chunk = ClientStreamChunk.parseFrom(toByteArray(message.getPayload()));
            if (chunk.getSessionId().isBlank()) {
                sendError(session, "missing-session", "missing-session", "session_id is required.");
                return;
            }
            if (chunk.getSessionId().length() > maxSessionIdLength) {
                metricsService.incrementPayloadErrors();
                sendError(
                        session,
                        chunk.getSessionId(),
                        "invalid-session",
                        "session_id exceeds the maximum length of " + maxSessionIdLength + "."
                );
                return;
            }
            if (!claimStreamSession(session, chunk.getSessionId())) {
                return;
            }
            boolean streamV2 = STREAM_PROTOCOL_V2.equals(websocketToStreamProtocols.get(session.getId()));
            if (streamV2 && !validateV2Chunk(session, chunk)) {
                return;
            }
            if (chunk.getFramesCount() == 0 && !(streamV2 && chunk.getEndOfSegment())) {
                metricsService.incrementPayloadErrors();
                sendError(
                        session,
                        chunk.getSessionId(),
                        "empty-frames",
                        "ClientStreamChunk must include at least one landmark frame."
                );
                return;
            }
            if (chunk.getFramesCount() > sessionBufferService.maxBufferedFrames()) {
                metricsService.incrementPayloadErrors();
                sendError(
                        session,
                        chunk.getSessionId(),
                        "too-many-frames",
                        "ClientStreamChunk maximum frame batch size is " + sessionBufferService.maxBufferedFrames() + "."
                );
                return;
            }
            Optional<LandmarkChunkValidator.ValidationError> validationError =
                    LandmarkChunkValidator.validate(
                            chunk,
                            maxPointsPerLandmarkGroup,
                            maxAbsoluteCoordinate
                    );
            if (validationError.isPresent()) {
                metricsService.incrementPayloadErrors();
                LandmarkChunkValidator.ValidationError error = validationError.get();
                sendError(session, chunk.getSessionId(), error.code(), error.message());
                return;
            }

            log.info("Received {} frames from session {}", chunk.getFramesCount(), chunk.getSessionId());
            websocketToStreamSessionIds.put(session.getId(), chunk.getSessionId());
            if (streamV2) {
                websocketToLastChunkSequences.put(session.getId(), chunk.getChunkSequence());
                websocketToAcceptedChunkIds
                        .computeIfAbsent(session.getId(), ignored -> ConcurrentHashMap.newKeySet())
                        .add(chunk.getChunkId());
            }

            if (chunk.getFramesCount() == 0) {
                sessionBufferService.flushNow(chunk.getSessionId())
                        .ifPresent(buffered -> dispatchBufferedChunk(session, chunk.getSessionId(), buffered));
                sendAck(session, chunk, 0, 0);
                sendStatus(session, chunk.getSessionId(), "segment_complete", "End of segment accepted.");
                return;
            }

            BufferedChunkResult buffered = sessionBufferService.append(chunk);
            if (!buffered.readyForInference()) {
                if (streamV2 && chunk.getEndOfSegment()) {
                    sessionBufferService.flushNow(chunk.getSessionId())
                            .ifPresent(flushed -> dispatchBufferedChunk(session, chunk.getSessionId(), flushed));
                    sendAck(session, chunk, chunk.getFramesCount(), 0);
                    sendStatus(session, chunk.getSessionId(), "segment_complete", "End of segment accepted.");
                    return;
                }
                sendStatus(
                        session,
                        chunk.getSessionId(),
                        "buffering",
                        "Buffering " + buffered.bufferedFrameCount() + " frames before inference."
                );
                if (streamV2) {
                    sendAck(session, chunk, chunk.getFramesCount(), 0);
                }
                scheduleIdleFlush(session, chunk.getSessionId(), buffered.scheduleToken());
                return;
            }

            dispatchBufferedChunk(session, chunk.getSessionId(), buffered);
            if (streamV2) {
                sendAck(session, chunk, chunk.getFramesCount(), 0);
                if (chunk.getEndOfSegment()) {
                    sendStatus(session, chunk.getSessionId(), "segment_complete", "End of segment accepted.");
                }
            }
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
        websocketToInferenceContexts.remove(session.getId());
        websocketToStreamProtocols.remove(session.getId());
        websocketToLastChunkSequences.remove(session.getId());
        websocketToAcceptedChunkIds.remove(session.getId());
        websocketRateWindows.remove(session.getId());
        if (streamSessionId != null) {
            streamSessionOwners.remove(streamSessionId, session.getId());
            sessionBufferService.clear(streamSessionId);
        }
    }

    private void sendRecognitionResult(WebSocketSession session, TranslationResult result) throws Exception {
        sendText(session, toJson(resultEvent(result)));
    }

    private void sendRecognitionResult(
            WebSocketSession session,
            TranslationResult result,
            InferenceContext context
    ) throws Exception {
        sendText(session, toJson(resultEvent(result, context)));
    }

    private void sendStatus(WebSocketSession session, String sessionId, String status, String text) throws Exception {
        sendText(session, toJson(statusEvent(sessionId, status, text)));
    }

    private void sendError(WebSocketSession session, String sessionId, String errorCode, String text) throws Exception {
        sendText(session, toJson(errorEvent(sessionId, errorCode, text)));
    }

    private void sendAck(
            WebSocketSession session,
            ClientStreamChunk chunk,
            int acceptedFrames,
            int droppedFrames
    ) throws Exception {
        Map<String, Object> event = baseEvent(chunk.getSessionId(), "ack");
        event.put("ack_sequence", chunk.getChunkSequence());
        event.put("chunk_id", chunk.getChunkId());
        event.put("accepted_frames", acceptedFrames);
        event.put("dropped_frames", droppedFrames);
        event.put("retry_after_ms", 0);
        sendText(session, toJson(event));
    }

    private void safeSendInferenceEvent(WebSocketSession session, TranslationResult result) {
        safeSendInferenceEvent(session, result, null);
    }

    private void safeSendInferenceEvent(WebSocketSession session, TranslationResult result, InferenceContext context) {
        if (!session.isOpen()) {
            return;
        }
        try {
            if (isInferenceError(result)) {
                sendError(session, result.getSessionId(), "inference-error", result.getText());
            } else if (context != null) {
                sendRecognitionResult(session, result, context);
            } else {
                sendRecognitionResult(session, result);
            }
        } catch (Exception e) {
            metricsService.incrementWebSocketSendFailures();
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
            metricsService.incrementWebSocketSendFailures();
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

    private Map<String, Object> resultEvent(TranslationResult result, InferenceContext context) {
        Map<String, Object> event = resultEvent(result);
        event.put("locale", context.locale());
        event.put("sign_language", context.sign_language());
        event.put("model_profile", context.model_profile());
        event.put("protocol_version", context.protocol_version());
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

    private void sendText(WebSocketSession session, String json) throws Exception {
        synchronized (session) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }
        }
    }

    private boolean claimStreamSession(WebSocketSession session, String streamSessionId) throws Exception {
        String boundSessionId = websocketToStreamSessionIds.get(session.getId());
        if (boundSessionId != null && !boundSessionId.equals(streamSessionId)) {
            metricsService.incrementPayloadErrors();
            sendError(
                    session,
                    streamSessionId,
                    "session-mismatch",
                    "A WebSocket connection cannot switch session_id."
            );
            return false;
        }

        String owner = streamSessionOwners.putIfAbsent(streamSessionId, session.getId());
        if (owner != null && !owner.equals(session.getId())) {
            metricsService.incrementPayloadErrors();
            sendError(
                    session,
                    streamSessionId,
                    "session-in-use",
                    "session_id is already owned by another WebSocket connection."
            );
            return false;
        }
        websocketToStreamSessionIds.putIfAbsent(session.getId(), streamSessionId);
        return true;
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

    private InferenceContext resolveInferenceContext(URI uri) {
        Map<String, String> params = parseQueryParams(uri);
        return signLanguageResolver.resolve(
                params.get("locale"),
                params.get("sign_language"),
                params.get("model_profile"),
                params.get("protocol_version")
        );
    }

    private String resolveStreamProtocol(URI uri) {
        String requested = parseQueryParams(uri).getOrDefault("stream_protocol_version", STREAM_PROTOCOL_V1);
        if (!STREAM_PROTOCOL_V1.equals(requested) && !STREAM_PROTOCOL_V2.equals(requested)) {
            throw new IllegalArgumentException("unsupported stream_protocol_version: " + requested);
        }
        return requested;
    }

    private boolean validateV2Chunk(WebSocketSession session, ClientStreamChunk chunk) throws Exception {
        if (chunk.getChunkSequence() == 0
                || chunk.getChunkId().isBlank()
                || chunk.getSegmentId().isBlank()
                || !STREAM_SCHEMA_V2.equals(chunk.getSchemaVersion())) {
            metricsService.incrementPayloadErrors();
            sendError(
                    session,
                    chunk.getSessionId(),
                    "invalid-v2-envelope",
                    "Stream v2 requires chunk_sequence, chunk_id, segment_id, and schema_version="
                            + STREAM_SCHEMA_V2 + "."
            );
            return false;
        }

        Set<String> acceptedIds = websocketToAcceptedChunkIds.computeIfAbsent(
                session.getId(),
                ignored -> ConcurrentHashMap.newKeySet()
        );
        if (acceptedIds.contains(chunk.getChunkId())) {
            sendAck(session, chunk, 0, 0);
            return false;
        }

        long expected = websocketToLastChunkSequences.getOrDefault(session.getId(), 0L) + 1;
        if (chunk.getChunkSequence() != expected) {
            metricsService.incrementPayloadErrors();
            sendError(
                    session,
                    chunk.getSessionId(),
                    "sequence-gap",
                    "Expected chunk_sequence " + expected + " but received " + chunk.getChunkSequence() + "."
            );
            return false;
        }
        return true;
    }

    private boolean allowMessage(String websocketSessionId) {
        long now = System.currentTimeMillis();
        MessageRateWindow window = websocketRateWindows.computeIfAbsent(
                websocketSessionId,
                ignored -> new MessageRateWindow(now)
        );
        synchronized (window) {
            if (now - window.startedAtMillis >= 1000) {
                window.startedAtMillis = now;
                window.count = 0;
            }
            window.count++;
            return window.count <= maxMessagesPerSecond;
        }
    }

    private Map<String, String> parseQueryParams(URI uri) {
        Map<String, String> params = new LinkedHashMap<>();
        if (uri == null || uri.getRawQuery() == null || uri.getRawQuery().isBlank()) {
            return params;
        }

        for (String pair : uri.getRawQuery().split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length > 1 ? decode(parts[1]) : "";
            if (!key.isBlank()) {
                params.put(key, value);
            }
        }
        return params;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
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

    private InferenceDispatchOutcome dispatchBufferedChunk(
            WebSocketSession session,
            String streamSessionId,
            BufferedChunkResult buffered
    ) {
        InferenceContext context = websocketToInferenceContexts.getOrDefault(
                session.getId(),
                signLanguageResolver.defaults()
        );
        InferenceDispatchOutcome outcome = asyncInferenceService.dispatchWithOutcome(
                streamSessionId,
                buffered.chunk(),
                context,
                result -> safeSendInferenceEvent(session, result, context)
        );
        if (outcome == InferenceDispatchOutcome.STARTED) {
            if (!buffered.idleTimeoutTriggered()) {
                safeSendStatus(
                        session,
                        streamSessionId,
                        "processing",
                        "Processing " + buffered.bufferedFrameCount() + " buffered frames."
                );
            }
        } else if (outcome == InferenceDispatchOutcome.QUEUED) {
            safeSendStatus(
                    session,
                    streamSessionId,
                    "queued",
                    "Inference queued for this session."
            );
        } else {
            int droppedFrames = sessionBufferService.restore(buffered.chunk());
            metricsService.addDroppedFrames(droppedFrames);
            safeSendStatus(
                    session,
                    streamSessionId,
                    "busy",
                    "Inference queue is full; frames were returned to the session buffer."
            );
        }
        return outcome;
    }

    private byte[] toByteArray(ByteBuffer buffer) {
        ByteBuffer copy = buffer.asReadOnlyBuffer();
        byte[] payload = new byte[copy.remaining()];
        copy.get(payload);
        return payload;
    }

    private static final class MessageRateWindow {
        private long startedAtMillis;
        private int count;

        private MessageRateWindow(long startedAtMillis) {
            this.startedAtMillis = startedAtMillis;
        }
    }
}
