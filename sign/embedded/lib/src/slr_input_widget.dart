import 'dart:async';
import 'dart:math';

import 'package:fixnum/fixnum.dart';
import 'package:flutter/material.dart';

import 'generated/schema/landmark.pb.dart';
import 'landmark_frame_source.dart';
import 'sign_gemma_client.dart';
import 'sign_recognition_engine.dart';

class SlrInputWidget extends StatefulWidget {
  const SlrInputWidget({
    super.key,
    required this.onSignRecognized,
    this.bridgeUrl = 'ws://127.0.0.1:8080/ws/sign',
    this.sessionId,
    this.languageContext,
    this.recognitionEngine,
    this.disposeRecognitionEngine = false,
    this.landmarkFrameStream,
    this.landmarkFrameSource,
    this.disposeLandmarkFrameSource = false,
    this.autoStreamMockFrames = false,
    this.autoReconnect = false,
    this.maxReconnectAttempts = 5,
    this.reconnectInitialDelay = const Duration(seconds: 1),
    this.reconnectMaxDelay = const Duration(seconds: 12),
    this.frameInterval = const Duration(milliseconds: 900),
    this.placeholder = '수어로 입력하려면 아이콘을 누르세요...',
  });

  final ValueChanged<String> onSignRecognized;
  final String bridgeUrl;
  final String? sessionId;
  final SignLanguageContext? languageContext;
  final SignRecognitionEngine? recognitionEngine;
  final bool disposeRecognitionEngine;
  final Stream<List<LandmarkFrame>>? landmarkFrameStream;
  final LandmarkFrameSource? landmarkFrameSource;
  final bool disposeLandmarkFrameSource;
  final bool autoStreamMockFrames;
  final bool autoReconnect;
  final int maxReconnectAttempts;
  final Duration reconnectInitialDelay;
  final Duration reconnectMaxDelay;
  final Duration frameInterval;
  final String placeholder;

  @override
  State<SlrInputWidget> createState() => _SlrInputWidgetState();
}

class _SlrInputWidgetState extends State<SlrInputWidget> {
  SignGemmaClient? _client;
  StreamSubscription<SignRecognitionEvent>? _recognitionSubscription;
  late final String _sessionId;
  Timer? _streamTimer;
  Timer? _reconnectTimer;
  StreamSubscription<List<LandmarkFrame>>? _landmarkSubscription;
  String _statusText = '';
  String _connectionDetail = '브리지에 연결되지 않았습니다.';
  bool _connected = false;
  bool _connecting = false;
  bool _disposed = false;
  bool _manualRetrying = false;
  bool _streamingMockFrames = false;
  bool _streamingSourceFrames = false;
  int _reconnectAttempts = 0;

  @override
  void initState() {
    super.initState();
    _sessionId = widget.sessionId ?? _createSessionId();
    _statusText = widget.placeholder;
    if (widget.recognitionEngine == null) {
      _client =
          SignGemmaClient(
              url: widget.bridgeUrl,
              languageContext: widget.languageContext,
            )
            ..onConnectionState = _handleConnectionState
            ..onEvent = _handleBridgeEvent;
    } else {
      _recognitionSubscription = widget.recognitionEngine!.events.listen(
        _handleRecognitionEvent,
      );
    }
    unawaited(_connect());
  }

  Future<void> _connect() async {
    setState(() {
      _connecting = true;
      _connectionDetail = '브리지에 연결 중입니다...';
    });

    try {
      final engine = widget.recognitionEngine;
      if (engine != null) {
        await engine.start();
        _handleConnectionState(SignGemmaConnectionState.connected, null);
      } else {
        await _client!.connect();
      }
      if (!mounted) {
        return;
      }
      setState(() {
        _reconnectAttempts = 0;
        _statusText = _hasLandmarkInput
            ? '연결되었습니다. 실제 landmark stream을 전송합니다.'
            : widget.autoStreamMockFrames
            ? '연결되었습니다. mock landmark stream을 전송할 수 있습니다.'
            : '연결되었습니다. 실제 landmark source를 연결해 주세요.';
      });
      if (_hasLandmarkInput) {
        await _startLandmarkSourceStream();
      } else if (widget.autoStreamMockFrames) {
        _startMockStream();
      }
    } catch (_) {
      if (!mounted) {
        return;
      }
      setState(() {
        _statusText = widget.placeholder;
      });
    }
  }

  @override
  void dispose() {
    _disposed = true;
    _streamTimer?.cancel();
    _reconnectTimer?.cancel();
    unawaited(_landmarkSubscription?.cancel() ?? Future<void>.value());
    unawaited(_recognitionSubscription?.cancel() ?? Future<void>.value());
    if (widget.disposeLandmarkFrameSource) {
      unawaited(widget.landmarkFrameSource?.dispose() ?? Future<void>.value());
    } else {
      unawaited(widget.landmarkFrameSource?.stop() ?? Future<void>.value());
    }
    unawaited(_client?.disconnect() ?? Future<void>.value());
    if (widget.disposeRecognitionEngine) {
      unawaited(widget.recognitionEngine?.stop() ?? Future<void>.value());
    }
    super.dispose();
  }

  @override
  void didUpdateWidget(covariant SlrInputWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.landmarkFrameStream != widget.landmarkFrameStream ||
        oldWidget.landmarkFrameSource != widget.landmarkFrameSource) {
      unawaited(_landmarkSubscription?.cancel() ?? Future<void>.value());
      unawaited(oldWidget.landmarkFrameSource?.stop() ?? Future<void>.value());
      _landmarkSubscription = null;
      _streamingSourceFrames = false;
      if (_connected && _hasLandmarkInput) {
        unawaited(_startLandmarkSourceStream());
      }
    }
  }

  void _handleBridgeEvent(SignGemmaBridgeEvent event) {
    if (!mounted) {
      return;
    }
    if (event.isResult) {
      final resultText = event.resultText ?? '';
      setState(() {
        _statusText = resultText.isEmpty ? widget.placeholder : resultText;
      });
      if (event.isFinal && resultText.isNotEmpty) {
        widget.onSignRecognized(resultText);
      }
      return;
    }

    setState(() {
      _statusText = event.statusText ?? widget.placeholder;
    });
  }

  void _handleRecognitionEvent(SignRecognitionEvent event) {
    if (!mounted) return;
    final result = event.result;
    if (result != null) {
      setState(() {
        _statusText = result.text.isEmpty ? widget.placeholder : result.text;
      });
      if (result.isFinal && result.text.isNotEmpty) {
        widget.onSignRecognized(result.text);
      }
      return;
    }
    setState(() {
      _statusText = event.message ?? widget.placeholder;
      _connected =
          event.state == SignRecognitionEngineState.ready ||
          event.state == SignRecognitionEngineState.processing;
      _connecting = event.state == SignRecognitionEngineState.starting;
      _connectionDetail = switch (event.state) {
        SignRecognitionEngineState.ready => '인식 엔진이 준비되었습니다.',
        SignRecognitionEngineState.processing => '수어를 인식하고 있습니다.',
        SignRecognitionEngineState.starting => '인식 엔진을 준비하고 있습니다.',
        SignRecognitionEngineState.error => '인식 엔진 오류: ${event.message ?? ''}',
        SignRecognitionEngineState.stopped => '인식 엔진이 종료되었습니다.',
      };
    });
  }

  void _handleConnectionState(SignGemmaConnectionState state, String? detail) {
    if (!mounted) {
      return;
    }
    var shouldScheduleReconnect = false;
    setState(() {
      _connecting = state == SignGemmaConnectionState.connecting;
      _connected = state == SignGemmaConnectionState.connected;
      switch (state) {
        case SignGemmaConnectionState.disconnected:
          if (!widget.autoReconnect || _reconnectTimer == null) {
            _connectionDetail = '브리지 연결이 종료되었습니다.';
          }
          _stopOutgoingStreams();
          shouldScheduleReconnect = true;
          break;
        case SignGemmaConnectionState.connecting:
          _connectionDetail = '브리지에 연결 중입니다...';
          break;
        case SignGemmaConnectionState.connected:
          _connectionDetail = '브리지와 연결되었습니다.';
          _reconnectAttempts = 0;
          break;
        case SignGemmaConnectionState.error:
          if (!widget.autoReconnect || _reconnectTimer == null) {
            _connectionDetail = detail == null
                ? '브리지 연결에 실패했습니다.'
                : '브리지 연결에 실패했습니다: $detail';
          }
          _stopOutgoingStreams();
          shouldScheduleReconnect = true;
          break;
      }
    });
    if (shouldScheduleReconnect) {
      _scheduleReconnectIfNeeded();
    }
  }

  Future<void> _startLandmarkSourceStream() async {
    final frames = _effectiveLandmarkFrameStream;
    if (frames == null) {
      return;
    }

    _streamTimer?.cancel();
    _streamingMockFrames = false;
    unawaited(_landmarkSubscription?.cancel() ?? Future<void>.value());
    _landmarkSubscription = frames.listen(
      (frames) {
        if (!_isRecognitionReady || frames.isEmpty) {
          return;
        }
        unawaited(_recognize(frames));
      },
      onError: (Object error) {
        if (!mounted) {
          return;
        }
        setState(() {
          _streamingSourceFrames = false;
          _statusText = 'landmark source 오류: $error';
        });
      },
      onDone: () {
        if (!mounted) {
          return;
        }
        setState(() {
          _streamingSourceFrames = false;
          _statusText = 'landmark source 전송이 종료되었습니다.';
        });
      },
    );
    setState(() {
      _streamingSourceFrames = true;
      _statusText = '실제 landmark stream을 전송 중입니다.';
    });

    try {
      await widget.landmarkFrameSource?.start();
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _streamingSourceFrames = false;
        _statusText = 'landmark source 시작 실패: $error';
      });
    }
  }

  void _startMockStream() {
    _streamTimer?.cancel();
    _streamTimer = Timer.periodic(widget.frameInterval, (_) {
      if (!_isRecognitionReady) {
        return;
      }
      unawaited(_recognize(_buildMockFrames()));
    });
    setState(() {
      _streamingMockFrames = true;
      _streamingSourceFrames = false;
      _statusText = 'mock landmark stream을 전송 중입니다.';
    });
  }

  Future<void> _retryConnection() async {
    _reconnectTimer?.cancel();
    _stopOutgoingStreams();
    setState(() {
      _connected = false;
      _reconnectAttempts = 0;
    });
    _manualRetrying = true;
    await _client?.disconnect();
    _manualRetrying = false;
    await _connect();
  }

  bool get _isRecognitionReady =>
      widget.recognitionEngine?.isReady ?? (_client?.isConnected ?? false);

  Future<void> _endSegment() async {
    try {
      final engine = widget.recognitionEngine;
      if (engine != null) {
        await engine.endSegment(_sessionId);
      } else {
        _client!.endSegment(_sessionId);
      }
    } catch (error) {
      if (mounted) setState(() => _statusText = '문장 확정 실패: $error');
    }
  }

  Future<void> _recognize(List<LandmarkFrame> frames) async {
    final engine = widget.recognitionEngine;
    try {
      if (engine != null) {
        await engine.recognize(sessionId: _sessionId, frames: frames);
      } else {
        _client!.sendFrames(frames, _sessionId);
      }
    } catch (error) {
      if (mounted) {
        setState(() => _statusText = '인식 요청 실패: $error');
      }
    }
  }

  void _scheduleReconnectIfNeeded() {
    if (!widget.autoReconnect ||
        _disposed ||
        _manualRetrying ||
        _connected ||
        _reconnectTimer != null) {
      return;
    }
    if (_reconnectAttempts >= widget.maxReconnectAttempts) {
      setState(() {
        _statusText = '자동 재연결 한도에 도달했습니다.';
      });
      return;
    }

    final delay = _nextReconnectDelay();
    _reconnectAttempts++;
    setState(() {
      _connectionDetail =
          '브리지 연결이 끊겼습니다. ${delay.inSeconds}초 후 자동 재연결을 시도합니다. '
          '($_reconnectAttempts/${widget.maxReconnectAttempts})';
    });
    _reconnectTimer = Timer(delay, () {
      _reconnectTimer = null;
      if (!_disposed && !_connected && !_connecting) {
        unawaited(_connect());
      }
    });
  }

  Duration _nextReconnectDelay() {
    final multiplier = pow(2, _reconnectAttempts).toInt();
    final millis = widget.reconnectInitialDelay.inMilliseconds * multiplier;
    return Duration(
      milliseconds: min(millis, widget.reconnectMaxDelay.inMilliseconds),
    );
  }

  void _stopOutgoingStreams() {
    _streamTimer?.cancel();
    _streamTimer = null;
    unawaited(_landmarkSubscription?.cancel() ?? Future<void>.value());
    _landmarkSubscription = null;
    unawaited(widget.landmarkFrameSource?.stop() ?? Future<void>.value());
    _streamingMockFrames = false;
    _streamingSourceFrames = false;
  }

  String get _streamDescription {
    if (widget.recognitionEngine?.mode == SignRecognitionMode.embedded) {
      return '입력 landmark를 기기 내 모델 delegate로 처리합니다.';
    }
    if (_streamingSourceFrames) {
      return '실제 landmark source에서 받은 frame batch를 브리지로 전송 중입니다.';
    }
    if (_streamingMockFrames) {
      return '브리지 검증용 mock landmark stream을 전송 중입니다.';
    }
    if (_hasLandmarkInput) {
      return '실제 landmark source에서 받은 frame batch를 브리지로 전송합니다.';
    }
    if (widget.autoStreamMockFrames) {
      return '현재 위젯은 브리지 검증용 mock landmark stream을 선택적으로 전송합니다.';
    }
    final reconnect = widget.autoReconnect
        ? ' 자동 재연결은 최대 ${widget.maxReconnectAttempts}회까지 시도합니다.'
        : '';
    return '현재 위젯은 브리지 연결 상태만 검증합니다. 실제 landmark source 연결은 다음 단계 작업입니다.$reconnect';
  }

  bool get _hasLandmarkInput =>
      widget.landmarkFrameSource != null || widget.landmarkFrameStream != null;

  Stream<List<LandmarkFrame>>? get _effectiveLandmarkFrameStream =>
      widget.landmarkFrameSource?.frames ?? widget.landmarkFrameStream;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return DecoratedBox(
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155)],
        ),
        borderRadius: BorderRadius.circular(24),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: FilledButton.tonalIcon(
                    onPressed: _isRecognitionReady ? _endSegment : null,
                    icon: const Icon(Icons.check),
                    label: const Text('문장 확정'),
                  ),
                ),
                const SizedBox(width: 12),
                Icon(
                  _connecting
                      ? Icons.cloud_sync_outlined
                      : _connected
                      ? Icons.cloud_done_outlined
                      : Icons.cloud_off_outlined,
                  color: Colors.white,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    widget.recognitionEngine?.mode ==
                            SignRecognitionMode.embedded
                        ? 'Embedded Sign Recognition'
                        : 'SignBridge Stream',
                    style: theme.textTheme.titleLarge?.copyWith(
                      color: Colors.white,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Expanded(
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.08),
                  borderRadius: BorderRadius.circular(18),
                  border: Border.all(color: Colors.white24),
                ),
                child: Center(
                  child: Text(
                    _statusText,
                    textAlign: TextAlign.center,
                    style: theme.textTheme.headlineSmall?.copyWith(
                      color: Colors.white,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Text(
              _connectionDetail,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: Colors.white70,
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: FilledButton.tonalIcon(
                    onPressed: _connecting ? null : _retryConnection,
                    icon: const Icon(Icons.refresh),
                    label: const Text('재연결'),
                  ),
                ),
                if (widget.autoStreamMockFrames) ...[
                  const SizedBox(width: 12),
                  Expanded(
                    child: FilledButton.tonalIcon(
                      onPressed: _connected && !_streamingMockFrames
                          ? _startMockStream
                          : null,
                      icon: const Icon(Icons.play_arrow_outlined),
                      label: const Text('Mock 시작'),
                    ),
                  ),
                ],
              ],
            ),
            const SizedBox(height: 16),
            Text(
              _streamDescription,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: Colors.white70,
              ),
            ),
          ],
        ),
      ),
    );
  }

  List<LandmarkFrame> _buildMockFrames() {
    final timestamp = Int64(DateTime.now().millisecondsSinceEpoch);
    return [
      LandmarkFrame()
        ..timestampMs = timestamp
        ..leftHand.addAll(_points(seed: 0.1))
        ..rightHand.addAll(_points(seed: 0.7))
        ..pose.addAll(_points(seed: 0.4, count: 6))
        ..faceContour.addAll(_points(seed: 0.2, count: 8)),
    ];
  }

  List<Point3D> _points({required double seed, int count = 4}) {
    final random = Random(
      (DateTime.now().microsecondsSinceEpoch * seed).round(),
    );
    return List<Point3D>.generate(
      count,
      (_) => Point3D()
        ..x = random.nextDouble()
        ..y = random.nextDouble()
        ..z = random.nextDouble(),
    );
  }

  String _createSessionId() {
    final now = DateTime.now().millisecondsSinceEpoch;
    return 'session-$now';
  }
}
