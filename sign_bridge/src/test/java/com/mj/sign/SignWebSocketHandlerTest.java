package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.LandmarkFrame;
import com.mj.sign.protos.LandmarkProto.TranslationResult;
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
                new AsyncInferenceService(gateway, Runnable::run, metricsService),
                scheduler,
                metricsService
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession("ws-1");

        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(session, new BinaryMessage(chunk("stream-1", 1).toByteArray()));

        assertEquals(1, session.messages.size());
        assertTrue(session.messages.getFirst().contains("\"session_id\": \"stream-1\""));
        assertTrue(session.messages.getFirst().contains("Buffering 1 frames"));
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
                new AsyncInferenceService(gateway, Runnable::run, metricsService),
                new ManualIdleFlushScheduler(),
                metricsService
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession("ws-2");

        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(session, new BinaryMessage(chunk("stream-2", 1).toByteArray()));
        handler.handleBinaryMessage(session, new BinaryMessage(chunk("stream-2", 1).toByteArray()));

        assertTrue(gateway.called);
        assertEquals(2, gateway.lastChunk.getFramesCount());
        assertEquals(3, session.messages.size());
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("Processing 2 buffered frames")));
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("\"text\": \"translated\"")));
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
                new AsyncInferenceService(gateway, Runnable::run, metricsService),
                scheduler,
                metricsService
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession("ws-3");

        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(session, new BinaryMessage(chunk("stream-3", 2).toByteArray()));

        clock.advanceMillis(1001);
        scheduler.runAll();

        assertTrue(gateway.called);
        assertEquals(3, session.messages.size());
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("Idle timeout reached. Flushing 2 buffered frames.")));
        assertTrue(session.messages.stream().anyMatch(message -> message.contains("\"text\": \"idle-translated\"")));
    }

    @Test
    void sendsBusyMessageWhenIdleFlushHitsInFlightInference() throws Exception {
        MutableClock clock = new MutableClock();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        SessionBufferService bufferService = new SessionBufferService(10, 20, 1000, clock, metricsService);
        QueueingExecutor executor = new QueueingExecutor();
        RecordingInferenceGateway gateway = new RecordingInferenceGateway();
        ManualIdleFlushScheduler scheduler = new ManualIdleFlushScheduler();
        SignWebSocketHandler handler = new SignWebSocketHandler(
                bufferService,
                new AsyncInferenceService(gateway, executor, metricsService),
                scheduler,
                metricsService
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession("ws-4");

        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(session, new BinaryMessage(chunk("stream-4", 10).toByteArray()));
        handler.handleBinaryMessage(session, new BinaryMessage(chunk("stream-4", 2).toByteArray()));

        clock.advanceMillis(1001);
        scheduler.runAll();
        executor.runAll();

        assertTrue(session.messages.stream().anyMatch(message -> message.contains("Inference already in progress for this session.")));
    }

    @Test
    void sendsStructuredErrorForInvalidPayload() throws Exception {
        MutableClock clock = new MutableClock();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        SessionBufferService bufferService = new SessionBufferService(2, 6, 1000, clock, metricsService);
        SignWebSocketHandler handler = new SignWebSocketHandler(
                bufferService,
                new AsyncInferenceService(new RecordingInferenceGateway(), Runnable::run, metricsService),
                new ManualIdleFlushScheduler(),
                metricsService
        );
        RecordingWebSocketSession session = new RecordingWebSocketSession("ws-5");

        handler.afterConnectionEstablished(session);
        handler.handleBinaryMessage(session, new BinaryMessage(new byte[]{0x01, 0x02, 0x03}));

        assertEquals(1, session.messages.size());
        assertTrue(session.messages.getFirst().contains("Failed to parse protobuf payload."));
    }

    @Test
    void clearsStreamBufferWhenWebSocketCloses() throws Exception {
        MutableClock clock = new MutableClock();
        BridgeMetricsService metricsService = new BridgeMetricsService();
        SessionBufferService bufferService = new SessionBufferService(3, 6, 1000, clock, metricsService);
        SignWebSocketHandler handler = new SignWebSocketHandler(
                bufferService,
                new AsyncInferenceService(new RecordingInferenceGateway(), Runnable::run, metricsService),
                new ManualIdleFlushScheduler(),
                metricsService
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

    private ClientStreamChunk chunk(String sessionId, int frameCount) {
        ClientStreamChunk.Builder builder = ClientStreamChunk.newBuilder().setSessionId(sessionId);
        for (int index = 0; index < frameCount; index++) {
            builder.addFrames(LandmarkFrame.newBuilder().setTimestampMs(index).build());
        }
        return builder.build();
    }

    private static final class RecordingInferenceGateway implements InferenceGateway {
        private final TranslationResult result;
        private boolean called;
        private ClientStreamChunk lastChunk;

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
            this.called = true;
            this.lastChunk = chunk;
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
        private final List<String> messages = new ArrayList<>();
        private boolean open = true;

        private RecordingWebSocketSession(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public URI getUri() {
            return URI.create("ws://localhost/ws/sign");
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
