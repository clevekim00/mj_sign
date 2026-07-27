package com.mj.sign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.LandmarkFrame;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
import com.mj.sign.service.AiTranslationProvider;
import com.mj.sign.service.SignTranslationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketSession;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignWebSocketHandlerTest {

    @Test
    void returnsBufferingMessageBeforeThreshold() throws Exception {
        MutableClock clock = new MutableClock();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        SessionBufferService bufferService = new SessionBufferService(3, 6, 1000, clock, metricsService);
        RecordingInferenceGateway gateway = new RecordingInferenceGateway();
        ManualIdleFlushScheduler scheduler = new ManualIdleFlushScheduler();
        SignWebSocketHandler handler = new SignWebSocketHandler(
                bufferService,
                asyncInferenceService(gateway, Runnable::run, metricsService),
                scheduler,
                metricsService,
                new ObjectMapper()
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession("ws-1");

        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(session, new BinaryMessage(chunk("stream-1", 1).toByteArray()));

        assertEquals(1, session.messages.size());
        assertTrue(session.messages.getFirst().contains("\"session_id\":\"stream-1\""));
        assertTrue(session.messages.getFirst().contains("\"event_type\":\"status\""));
        assertTrue(session.messages.getFirst().contains("\"status\":\"buffering\""));
        assertTrue(session.messages.getFirst().contains("Buffering 1 frames"));
        assertFalse(session.messages.getFirst().contains("\"text\""));
        assertFalse(gateway.called);
        assertEquals(1, scheduler.tasks.size());
    }

    @Test
    void forwardsBufferedChunkForInferenceWhenThresholdReached() throws Exception {
        MutableClock clock = new MutableClock();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        SessionBufferService bufferService = new SessionBufferService(2, 6, 1000, clock, metricsService);
        RecordingInferenceGateway gateway = new RecordingInferenceGateway(
                TranslationResult.newBuilder()
                        .setSessionId("stream-2")
                        .setText("translated")
                        .setIsFinal(true)
                        .setConfidence(0.9f)
                        .build()
        );
        SignWebSocketHandler handler = new SignWebSocketHandler(
                bufferService,
                asyncInferenceService(gateway, Runnable::run, metricsService),
                new ManualIdleFlushScheduler(),
                metricsService,
                new ObjectMapper()
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession("ws-2");

        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(session, new BinaryMessage(chunk("stream-2", 1).toByteArray()));
        handler.handleBinaryMessage(session, new BinaryMessage(chunk("stream-2", 1).toByteArray()));

        assertTrue(gateway.called);
        assertEquals(2, gateway.lastChunk.getFramesCount());
        assertEquals(3, session.messages.size());
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("\"event_type\":\"status\"")));
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("\"event_type\":\"result\"")));
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("Processing 2 buffered frames")));
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("\"result_text\":\"translated\"")));
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("\"text\":\"translated\"")));
    }

    @Test
    void forwardsLanguageContextFromWebSocketQueryToInferenceGateway() throws Exception {
        MutableClock clock = new MutableClock();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        SessionBufferService bufferService = new SessionBufferService(1, 6, 1000, clock, metricsService);
        RecordingInferenceGateway gateway = new RecordingInferenceGateway();
        SignWebSocketHandler handler = new SignWebSocketHandler(
                bufferService,
                asyncInferenceService(gateway, Runnable::run, metricsService),
                new ManualIdleFlushScheduler(),
                metricsService,
                new ObjectMapper()
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession(
                "ws-locale",
                URI.create("ws://localhost/ws/sign?locale=en-US&sign_language=asl&model_profile=sign-gemma&protocol_version=mj-sign-model-v2")
        );

        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(session, new BinaryMessage(chunk("stream-locale", 1).toByteArray()));

        assertTrue(gateway.called);
        assertEquals("en-US", gateway.lastContext.locale());
        assertEquals("asl", gateway.lastContext.sign_language());
        assertEquals("sign-gemma", gateway.lastContext.model_profile());
        assertEquals("mj-sign-model-v2", gateway.lastContext.protocol_version());
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("\"locale\":\"en-US\"")));
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("\"sign_language\":\"asl\"")));
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("\"model_profile\":\"sign-gemma\"")));
    }

    @Test
    void rejectsUnsupportedModelProfileOnConnection() throws Exception {
        MutableClock clock = new MutableClock();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        SessionBufferService bufferService = new SessionBufferService(1, 6, 1000, clock, metricsService);
        RecordingInferenceGateway gateway = new RecordingInferenceGateway();
        SignWebSocketHandler handler = new SignWebSocketHandler(
                bufferService,
                asyncInferenceService(gateway, Runnable::run, metricsService),
                new ManualIdleFlushScheduler(),
                metricsService,
                new ObjectMapper()
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession(
                "ws-unsupported-profile",
                URI.create("ws://localhost/ws/sign?locale=en-US&sign_language=asl&model_profile=custom-model")
        );

        handler.afterConnectionEstablished(session);

        assertFalse(session.isOpen());
        assertFalse(gateway.called);
        assertEquals(1, session.messages.size());
        assertTrue(session.messages.getFirst().contains("\"error_code\":\"unsupported-profile\""));
        assertTrue(session.messages.getFirst().contains("unsupported model_profile"));
    }

    @Test
    void flushesBufferedFramesAfterIdleTimeout() throws Exception {
        MutableClock clock = new MutableClock();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        SessionBufferService bufferService = new SessionBufferService(4, 8, 1000, clock, metricsService);
        RecordingInferenceGateway gateway = new RecordingInferenceGateway(
                TranslationResult.newBuilder()
                        .setSessionId("stream-3")
                        .setText("idle-translated")
                        .setIsFinal(true)
                        .setConfidence(0.7f)
                        .build()
        );
        ManualIdleFlushScheduler scheduler = new ManualIdleFlushScheduler();
        SignWebSocketHandler handler = new SignWebSocketHandler(
                bufferService,
                asyncInferenceService(gateway, Runnable::run, metricsService),
                scheduler,
                metricsService,
                new ObjectMapper()
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession("ws-3");

        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(session, new BinaryMessage(chunk("stream-3", 2).toByteArray()));

        clock.advanceMillis(1001);
        scheduler.runAll();

        assertTrue(gateway.called);
        assertEquals(3, session.messages.size());
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("\"status\":\"idle_flush\"")));
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("Idle timeout reached. Flushing 2 buffered frames.")));
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("\"result_text\":\"idle-translated\"")));
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("\"text\":\"idle-translated\"")));
    }

    @Test
    void queuesIdleFlushWhenInferenceIsAlreadyInFlight() throws Exception {
        MutableClock clock = new MutableClock();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        SessionBufferService bufferService = new SessionBufferService(10, 20, 1000, clock, metricsService);
        QueueingExecutor executor = new QueueingExecutor();
        RecordingInferenceGateway gateway = new RecordingInferenceGateway();
        ManualIdleFlushScheduler scheduler = new ManualIdleFlushScheduler();
        SignWebSocketHandler handler = new SignWebSocketHandler(
                bufferService,
                asyncInferenceService(gateway, executor, metricsService),
                scheduler,
                metricsService,
                new ObjectMapper()
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession("ws-4");

        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(session, new BinaryMessage(chunk("stream-4", 10).toByteArray()));
        handler.handleBinaryMessage(session, new BinaryMessage(chunk("stream-4", 2).toByteArray()));

        clock.advanceMillis(1001);
        scheduler.runAll();
        executor.runAll();

        assertTrue(session.messages.stream().anyMatch(message -> message.contains("\"status\":\"queued\"")));
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("Inference queued for this session.")));
    }

    @Test
    void sendsInferenceFailuresAsErrorEvents() throws Exception {
        MutableClock clock = new MutableClock();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        SessionBufferService bufferService = new SessionBufferService(1, 6, 1000, clock, metricsService);
        RecordingInferenceGateway gateway = new RecordingInferenceGateway(
                TranslationResult.newBuilder()
                        .setSessionId("stream-error")
                        .setText("Failed to connect to GPU server.")
                        .setIsFinal(true)
                        .setConfidence(0.0f)
                        .build()
        );
        SignWebSocketHandler handler = new SignWebSocketHandler(
                bufferService,
                asyncInferenceService(gateway, Runnable::run, metricsService),
                new ManualIdleFlushScheduler(),
                metricsService,
                new ObjectMapper()
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession("ws-error");

        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(session, new BinaryMessage(chunk("stream-error", 1).toByteArray()));

        assertTrue(session.messages.stream().anyMatch(message -> message.contains("\"event_type\":\"error\"")));
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("\"error_code\":\"inference-error\"")));
        assertFalse(session.messages.stream().anyMatch(message -> message.contains("\"event_type\":\"result\"")));
    }

    @Test
    void sendsStructuredErrorForInvalidPayload() throws Exception {
        MutableClock clock = new MutableClock();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        SessionBufferService bufferService = new SessionBufferService(2, 6, 1000, clock, metricsService);
        SignWebSocketHandler handler = new SignWebSocketHandler(
                bufferService,
                asyncInferenceService(new RecordingInferenceGateway(), Runnable::run, metricsService),
                new ManualIdleFlushScheduler(),
                metricsService,
                new ObjectMapper()
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession("ws-5");

        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(session, new BinaryMessage(new byte[]{0x01, 0x02, 0x03}));

        assertEquals(1, session.messages.size());
        assertTrue(session.messages.getFirst().contains("\"event_type\":\"error\""));
        assertTrue(session.messages.getFirst().contains("\"error_code\":\"invalid-payload\""));
        assertTrue(session.messages.getFirst().contains("Failed to parse protobuf payload."));
    }

    @Test
    void rejectsEmptyFrameChunks() throws Exception {
        MutableClock clock = new MutableClock();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        SessionBufferService bufferService = new SessionBufferService(2, 6, 1000, clock, metricsService);
        RecordingInferenceGateway gateway = new RecordingInferenceGateway();
        SignWebSocketHandler handler = new SignWebSocketHandler(
                bufferService,
                asyncInferenceService(gateway, Runnable::run, metricsService),
                new ManualIdleFlushScheduler(),
                metricsService,
                new ObjectMapper()
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession("ws-empty");

        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(
                session,
                new BinaryMessage(ClientStreamChunk.newBuilder().setSessionId("stream-empty").build().toByteArray())
        );

        assertEquals(1, session.messages.size());
        assertTrue(session.messages.getFirst().contains("\"event_type\":\"error\""));
        assertTrue(session.messages.getFirst().contains("\"error_code\":\"empty-frames\""));
        assertTrue(session.messages.getFirst().contains("at least one landmark frame"));
        assertFalse(gateway.called);
    }

    @Test
    void rejectsOversizedFrameChunks() throws Exception {
        MutableClock clock = new MutableClock();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        SessionBufferService bufferService = new SessionBufferService(2, 6, 1000, clock, metricsService);
        RecordingInferenceGateway gateway = new RecordingInferenceGateway();
        SignWebSocketHandler handler = new SignWebSocketHandler(
                bufferService,
                asyncInferenceService(gateway, Runnable::run, metricsService),
                new ManualIdleFlushScheduler(),
                metricsService,
                new ObjectMapper()
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession("ws-oversized");

        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(session, new BinaryMessage(chunk("stream-oversized", 7).toByteArray()));

        assertEquals(1, session.messages.size());
        assertTrue(session.messages.getFirst().contains("\"event_type\":\"error\""));
        assertTrue(session.messages.getFirst().contains("\"error_code\":\"too-many-frames\""));
        assertTrue(session.messages.getFirst().contains("maximum frame batch size is 6"));
        assertFalse(gateway.called);
    }

    @Test
    void clearsStreamBufferWhenWebSocketCloses() throws Exception {
        MutableClock clock = new MutableClock();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        SessionBufferService bufferService = new SessionBufferService(3, 6, 1000, clock, metricsService);
        SignWebSocketHandler handler = new SignWebSocketHandler(
                bufferService,
                asyncInferenceService(new RecordingInferenceGateway(), Runnable::run, metricsService),
                new ManualIdleFlushScheduler(),
                metricsService,
                new ObjectMapper()
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession("ws-6");

        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(session, new BinaryMessage(chunk("stream-6", 1).toByteArray()));
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(session, new BinaryMessage(chunk("stream-6", 1).toByteArray()));

        assertEquals(2, session.messages.size());
        assertTrue(session.messages.get(1).contains("Buffering 1 frames"));
    }

    @Test
    void preventsSessionSwitchAndCrossConnectionSessionReuse() throws Exception {
        MutableClock clock = new MutableClock();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        SessionBufferService bufferService = new SessionBufferService(4, 8, 1000, clock, metricsService);
        SignWebSocketHandler handler = new SignWebSocketHandler(
                bufferService,
                asyncInferenceService(new RecordingInferenceGateway(), Runnable::run, metricsService),
                new ManualIdleFlushScheduler(),
                metricsService,
                new ObjectMapper()
        );
        RecordingWebSocketSession first = new RecordingWebSocketSession("owner-1");
        RecordingWebSocketSession second = new RecordingWebSocketSession("owner-2");

        handler.afterConnectionEstablished(first);
        handler.afterConnectionEstablished(second);
        handler.handleBinaryMessage(first, new BinaryMessage(chunk("owned-stream", 1).toByteArray()));
        handler.handleBinaryMessage(first, new BinaryMessage(chunk("other-stream", 1).toByteArray()));
        handler.handleBinaryMessage(second, new BinaryMessage(chunk("owned-stream", 1).toByteArray()));

        assertTrue(first.messages.stream().anyMatch(message ->
                message.contains("\"error_code\":\"session-mismatch\"")
        ));
        assertTrue(second.messages.stream().anyMatch(message ->
                message.contains("\"error_code\":\"session-in-use\"")
        ));
    }

    @Test
    void acceptsV2ChunkWithAckAndRejectsSequenceGap() throws Exception {
        MutableClock clock = new MutableClock();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        SignWebSocketHandler handler = new SignWebSocketHandler(
                new SessionBufferService(4, 8, 1000, clock, metricsService),
                asyncInferenceService(new RecordingInferenceGateway(), Runnable::run, metricsService),
                new ManualIdleFlushScheduler(),
                metricsService,
                new ObjectMapper()
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession(
                "v2-session",
                URI.create("ws://localhost/ws/sign?stream_protocol_version=signbridge-stream-v2")
        );

        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(session, new BinaryMessage(v2Chunk("stream-v2", 1, false).toByteArray()));
        handler.handleBinaryMessage(session, new BinaryMessage(v2Chunk("stream-v2", 3, false).toByteArray()));

        assertTrue(session.messages.stream().anyMatch(message ->
                message.contains("\"event_type\":\"ack\"")
                        && message.contains("\"ack_sequence\":1")
        ));
        assertTrue(session.messages.stream().anyMatch(message ->
                message.contains("\"error_code\":\"sequence-gap\"")
                        && message.contains("Expected chunk_sequence 2")
        ));
    }

    @Test
    void v2EndOfSegmentFlushesFramesBelowThreshold() throws Exception {
        MutableClock clock = new MutableClock();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        RecordingInferenceGateway gateway = new RecordingInferenceGateway();
        SignWebSocketHandler handler = new SignWebSocketHandler(
                new SessionBufferService(8, 16, 1000, clock, metricsService),
                asyncInferenceService(gateway, Runnable::run, metricsService),
                new ManualIdleFlushScheduler(),
                metricsService,
                new ObjectMapper()
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession(
                "v2-eos",
                URI.create("ws://localhost/ws/sign?stream_protocol_version=signbridge-stream-v2")
        );

        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(session, new BinaryMessage(v2Chunk("stream-eos", 1, true).toByteArray()));

        assertTrue(gateway.called);
        assertEquals(1, gateway.lastChunk.getFramesCount());
        assertTrue(session.messages.stream().anyMatch(message ->
                message.contains("\"status\":\"segment_complete\"")
        ));
    }

    private ClientStreamChunk chunk(String sessionId, int frameCount) {
        ClientStreamChunk.Builder builder = ClientStreamChunk.newBuilder().setSessionId(sessionId);
        for (int index = 0; index < frameCount; index++) {
            builder.addFrames(LandmarkFrame.newBuilder().setTimestampMs(index).build());
        }
        return builder.build();
    }

    private ClientStreamChunk v2Chunk(String sessionId, long sequence, boolean endOfSegment) {
        return ClientStreamChunk.newBuilder()
                .setSessionId(sessionId)
                .addFrames(LandmarkFrame.newBuilder().setTimestampMs(sequence).build())
                .setChunkSequence(sequence)
                .setChunkId("chunk-" + sequence)
                .setSegmentId("segment-1")
                .setEndOfSegment(endOfSegment)
                .setSentAtMs(sequence)
                .setSchemaVersion("mj.sign.ClientStreamChunk/v2")
                .build();
    }

    private SignTranslationService translationService() {
        return new SignTranslationService(new AiTranslationProvider() {
            @Override
            public String generateResponse(String systemPrompt, String userPrompt) {
                return userPrompt;
            }
        });
    }

    private AsyncInferenceService asyncInferenceService(
            InferenceGateway gateway,
            java.util.concurrent.Executor executor,
            BridgeMetricsService metricsService
    ) {
        return new AsyncInferenceService(
                gateway,
                executor,
                metricsService,
                translationService(),
                false,
                0.6f
        );
    }

    private static final class RecordingInferenceGateway implements InferenceGateway {
        private final TranslationResult result;
        private boolean called;
        private ClientStreamChunk lastChunk;
        private InferenceContext lastContext;

        private RecordingInferenceGateway() {
            this(
                    TranslationResult.newBuilder()
                            .setSessionId("default")
                            .setText("ok")
                            .setIsFinal(true)
                            .setConfidence(1.0f)
                            .build()
            );
        }

        private RecordingInferenceGateway(TranslationResult result) {
            this.result = result;
        }

        @Override
        public InferenceProvider provider() {
            return InferenceProvider.HTTP;
        }

        @Override
        public TranslationResult sendForInference(ClientStreamChunk chunk) {
            return sendForInference(chunk, InferenceContext.defaults());
        }

        @Override
        public TranslationResult sendForInference(ClientStreamChunk chunk, InferenceContext context) {
            this.called = true;
            this.lastChunk = chunk;
            this.lastContext = context;
            return result;
        }
    }

    private static final class ManualIdleFlushScheduler implements IdleFlushScheduler {
        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void schedule(String sessionId, long scheduleToken, Duration delay, Runnable task) {
            tasks.add(task);
        }

        private void runAll() {
            List<Runnable> pending = new ArrayList<>(tasks);
            tasks.clear();
            pending.forEach(Runnable::run);
        }
    }

    private static final class QueueingExecutor implements java.util.concurrent.Executor {
        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        private void runAll() {
            List<Runnable> pending = new ArrayList<>(tasks);
            tasks.clear();
            pending.forEach(Runnable::run);
        }
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

    private static final class RecordingWebSocketSession implements WebSocketSession {
        private final String id;
        private final URI uri;
        private final List<String> messages = new ArrayList<>();
        private boolean open = true;

        private RecordingWebSocketSession(String id) {
            this(id, URI.create("ws://localhost/ws/sign"));
        }

        private RecordingWebSocketSession(String id, URI uri) {
            this.id = id;
            this.uri = uri;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public URI getUri() {
            return uri;
        }

        @Override
        public HttpHeaders getHandshakeHeaders() {
            return HttpHeaders.EMPTY;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return new HashMap<>();
        }

        @Override
        public Principal getPrincipal() {
            return null;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public String getAcceptedProtocol() {
            return null;
        }

        @Override
        public void setTextMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getTextMessageSizeLimit() {
            return 0;
        }

        @Override
        public void setBinaryMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getBinaryMessageSizeLimit() {
            return 0;
        }

        @Override
        public List<WebSocketExtension> getExtensions() {
            return Collections.emptyList();
        }

        @Override
        public void sendMessage(WebSocketMessage<?> message) {
            messages.add(message.getPayload().toString());
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() {
            open = false;
        }

        @Override
        public void close(CloseStatus status) {
            open = false;
        }
    }
}
