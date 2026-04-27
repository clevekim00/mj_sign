import 'dart:async';
import 'dart:math';

import 'package:fixnum/fixnum.dart';
import 'package:flutter/material.dart';

import 'generated/schema/landmark.pb.dart';
import 'landmark_frame_source.dart';
import 'sign_gemma_client.dart';

class SlrInputWidget extends StatefulWidget {
  const SlrInputWidget({
    super.key,
    required this.onSignRecognized,
    this.bridgeUrl = 'ws://127.0.0.1:8080/ws/sign',
    this.sessionId,
    this.languageContext,
    this.landmarkFrameStream,
    this.landmarkFrameSource,
    this.disposeLandmarkFrameSource = false,
    this.autoStreamMockFrames = false,
    this.frameInterval = const Duration(milliseconds: 900),
    this.placeholder = '수어로 입력하려면 아이콘을 누르세요...',
  });

  final ValueChanged<String> onSignRecognized;
  final String bridgeUrl;
  final String? sessionId;
  final SignLanguageContext? languageContext;
  final Stream<List<LandmarkFrame>>? landmarkFrameStream;
  final LandmarkFrameSource? landmarkFrameSource;
  final bool disposeLandmarkFrameSource;
  final bool autoStreamMockFrames;
  final Duration frameInterval;
  final String placeholder;

  @override
  State<SlrInputWidget> createState() => _SlrInputWidgetState();
}

class _SlrInputWidgetState extends State<SlrInputWidget> {
  late final SignGemmaClient _client;
  late final String _sessionId;
  Timer? _streamTimer;
  StreamSubscription<List<LandmarkFrame>>? _landmarkSubscription;
  String _statusText = '';
  String _connectionDetail = '브리지에 연결되지 않았습니다.';
  bool _connected = false;
  bool _connecting = false;
  bool _streamingMockFrames = false;
  bool _streamingSourceFrames = false;

  @override
  void initState() {
    super.initState();
    _sessionId = widget.sessionId ?? _createSessionId();
    _statusText = widget.placeholder;
    _client =
        SignGemmaClient(
            url: widget.bridgeUrl,
            languageContext: widget.languageContext,
          )
          ..onConnectionState = _handleConnectionState
          ..onEvent = _handleBridgeEvent;
    unawaited(_connect());
  }

  Future<void> _connect() async {
    setState(() {
      _connecting = true;
      _connectionDetail = '브리지에 연결 중입니다...';
    });

    try {
      await _client.connect();
      if (!mounted) {
        return;
      }
      setState(() {
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
    _streamTimer?.cancel();
    unawaited(_landmarkSubscription?.cancel() ?? Future<void>.value());
    if (widget.disposeLandmarkFrameSource) {
      unawaited(widget.landmarkFrameSource?.dispose() ?? Future<void>.value());
    } else {
      unawaited(widget.landmarkFrameSource?.stop() ?? Future<void>.value());
    }
    unawaited(_client.disconnect());
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

  void _handleConnectionState(SignGemmaConnectionState state, String? detail) {
    if (!mounted) {
      return;
    }
    setState(() {
      _connecting = state == SignGemmaConnectionState.connecting;
      _connected = state == SignGemmaConnectionState.connected;
      switch (state) {
        case SignGemmaConnectionState.disconnected:
          _connectionDetail = '브리지 연결이 종료되었습니다.';
          _stopOutgoingStreams();
          break;
        case SignGemmaConnectionState.connecting:
          _connectionDetail = '브리지에 연결 중입니다...';
          break;
        case SignGemmaConnectionState.connected:
          _connectionDetail = '브리지와 연결되었습니다.';
          break;
        case SignGemmaConnectionState.error:
          _connectionDetail = detail == null
              ? '브리지 연결에 실패했습니다.'
              : '브리지 연결에 실패했습니다: $detail';
          _stopOutgoingStreams();
          break;
      }
    });
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
        if (!_client.isConnected || frames.isEmpty) {
          return;
        }
        _client.sendFrames(frames, _sessionId);
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
      if (!_client.isConnected) {
        return;
      }
      _client.sendFrames(_buildMockFrames(), _sessionId);
    });
    setState(() {
      _streamingMockFrames = true;
      _streamingSourceFrames = false;
      _statusText = 'mock landmark stream을 전송 중입니다.';
    });
  }

  Future<void> _retryConnection() async {
    _stopOutgoingStreams();
    setState(() {
      _connected = false;
    });
    await _client.disconnect();
    await _connect();
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
    return '현재 위젯은 브리지 연결 상태만 검증합니다. 실제 landmark source 연결은 다음 단계 작업입니다.';
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
                    'Sign Bridge Stream',
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
