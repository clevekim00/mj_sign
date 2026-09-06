/// Shared recognition lifecycle and local/backend runtime adapters.
library;

import 'dart:async';
import 'dart:collection';

import 'generated/schema/landmark.pb.dart';
import 'sign_gemma_client.dart';

enum SignRecognitionMode { embedded, backend }

enum SignRecognitionEngineState { stopped, starting, ready, processing, error }

class SignRecognitionEvent {
  const SignRecognitionEvent({required this.state, this.result, this.message});

  final SignRecognitionEngineState state;
  final TranslationResult? result;
  final String? message;
}

abstract interface class SignRecognitionEngine {
  SignRecognitionMode get mode;
  bool get isReady;
  Stream<SignRecognitionEvent> get events;

  Future<void> start();

  Future<void> recognize({
    required String sessionId,
    required List<LandmarkFrame> frames,
    bool endOfSegment = false,
  });

  Future<void> endSegment(String sessionId);
  Future<void> stop();
}

class EmbeddedRecognitionRequest {
  const EmbeddedRecognitionRequest({
    required this.sessionId,
    required this.frames,
    required this.endOfSegment,
    required this.languageContext,
  });

  final String sessionId;
  final List<LandmarkFrame> frames;
  final bool endOfSegment;
  final SignLanguageContext languageContext;
}

typedef EmbeddedRecognitionDelegate =
    Future<TranslationResult> Function(EmbeddedRecognitionRequest request);

/// Runs a caller-supplied on-device model delegate without any network access.
/// The delegate is the integration boundary for TFLite, MediaPipe Tasks, Core ML,
/// or another bundled runtime. Requests are serialized and bounded per engine.
class EmbeddedSignRecognitionEngine implements SignRecognitionEngine {
  EmbeddedSignRecognitionEngine({
    required EmbeddedRecognitionDelegate delegate,
    SignLanguageContext? languageContext,
    this.maxPendingRequests = 4,
  }) : _delegate = delegate,
       languageContext =
           languageContext ?? SignLanguageContext.fromPlatformDispatcher() {
    if (maxPendingRequests < 1) {
      throw ArgumentError.value(maxPendingRequests, 'maxPendingRequests');
    }
  }

  final EmbeddedRecognitionDelegate _delegate;
  final SignLanguageContext languageContext;
  final int maxPendingRequests;
  final Queue<EmbeddedRecognitionRequest> _pending = Queue();
  final StreamController<SignRecognitionEvent> _events =
      StreamController<SignRecognitionEvent>.broadcast();
  bool _ready = false;
  bool _processing = false;
  bool _closed = false;

  @override
  SignRecognitionMode get mode => SignRecognitionMode.embedded;

  @override
  bool get isReady => _ready && !_closed;

  @override
  Stream<SignRecognitionEvent> get events => _events.stream;

  @override
  Future<void> start() async {
    if (_closed) {
      throw StateError('Embedded recognition engine has been closed.');
    }
    if (_ready) return;
    _events.add(
      const SignRecognitionEvent(
        state: SignRecognitionEngineState.starting,
        message: '내장 모델을 준비하고 있습니다.',
      ),
    );
    _ready = true;
    _events.add(
      const SignRecognitionEvent(
        state: SignRecognitionEngineState.ready,
        message: '내장 모델이 준비되었습니다.',
      ),
    );
  }

  @override
  Future<void> recognize({
    required String sessionId,
    required List<LandmarkFrame> frames,
    bool endOfSegment = false,
  }) async {
    if (!isReady) {
      throw StateError('Embedded recognition engine is not ready.');
    }
    if (frames.isEmpty && !endOfSegment) return;
    if (_pending.length >= maxPendingRequests) {
      throw StateError('Embedded recognition queue is full.');
    }
    _pending.add(
      EmbeddedRecognitionRequest(
        sessionId: sessionId,
        frames: List<LandmarkFrame>.unmodifiable(frames),
        endOfSegment: endOfSegment,
        languageContext: languageContext,
      ),
    );
    await _drain();
  }

  Future<void> _drain() async {
    if (_processing) return;
    _processing = true;
    try {
      while (_pending.isNotEmpty && !_closed) {
        final request = _pending.removeFirst();
        _events.add(
          const SignRecognitionEvent(
            state: SignRecognitionEngineState.processing,
            message: '기기에서 수어를 인식하고 있습니다.',
          ),
        );
        try {
          final result = await _delegate(request);
          if (_closed) break;
          _events.add(
            SignRecognitionEvent(
              state: SignRecognitionEngineState.ready,
              result: result,
            ),
          );
        } catch (error) {
          if (_closed) break;
          _events.add(
            SignRecognitionEvent(
              state: SignRecognitionEngineState.error,
              message: error.toString(),
            ),
          );
        }
      }
    } finally {
      _processing = false;
    }
  }

  @override
  Future<void> endSegment(String sessionId) => recognize(
    sessionId: sessionId,
    frames: const <LandmarkFrame>[],
    endOfSegment: true,
  );

  @override
  Future<void> stop() async {
    if (_closed) return;
    _closed = true;
    _ready = false;
    _pending.clear();
    _events.add(
      const SignRecognitionEvent(
        state: SignRecognitionEngineState.stopped,
        message: '내장 모델이 종료되었습니다.',
      ),
    );
    await _events.close();
  }
}

/// Adapts the SignBridge WebSocket protocol to the common recognition engine.
class BackendSignRecognitionEngine implements SignRecognitionEngine {
  BackendSignRecognitionEngine({required SignGemmaClient client})
    : _client = client;

  final SignGemmaClient _client;
  final StreamController<SignRecognitionEvent> _events =
      StreamController<SignRecognitionEvent>.broadcast();
  bool _closed = false;

  @override
  SignRecognitionMode get mode => SignRecognitionMode.backend;

  @override
  bool get isReady => !_closed && _client.isConnected;

  @override
  Stream<SignRecognitionEvent> get events => _events.stream;

  @override
  Future<void> start() async {
    if (_closed) {
      throw StateError('Backend recognition engine has been closed.');
    }
    _client.onConnectionState = (state, detail) {
      if (_closed) return;
      final mapped = switch (state) {
        SignGemmaConnectionState.disconnected =>
          SignRecognitionEngineState.stopped,
        SignGemmaConnectionState.connecting =>
          SignRecognitionEngineState.starting,
        SignGemmaConnectionState.connected => SignRecognitionEngineState.ready,
        SignGemmaConnectionState.error => SignRecognitionEngineState.error,
      };
      _events.add(SignRecognitionEvent(state: mapped, message: detail));
    };
    _client.onEvent = (event) {
      if (_closed) return;
      if (event.isResult) {
        _events.add(
          SignRecognitionEvent(
            state: SignRecognitionEngineState.ready,
            result: event.toTranslationResult(),
          ),
        );
      } else {
        _events.add(
          SignRecognitionEvent(
            state: event.isError
                ? SignRecognitionEngineState.error
                : SignRecognitionEngineState.processing,
            message: event.statusText,
          ),
        );
      }
    };
    await _client.connect();
  }

  @override
  Future<void> recognize({
    required String sessionId,
    required List<LandmarkFrame> frames,
    bool endOfSegment = false,
  }) async {
    if (!isReady) throw StateError('Backend recognition engine is not ready.');
    if (frames.isNotEmpty) {
      _client.sendFrames(frames, sessionId);
    }
    if (endOfSegment) {
      _client.endSegment(sessionId);
    }
  }

  @override
  Future<void> endSegment(String sessionId) async {
    if (!isReady) throw StateError('Backend recognition engine is not ready.');
    _client.endSegment(sessionId);
  }

  @override
  Future<void> stop() async {
    if (_closed) return;
    _closed = true;
    await _client.disconnect();
    await _events.close();
  }
}
