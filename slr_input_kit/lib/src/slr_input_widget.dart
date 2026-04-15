import 'dart:async';
import 'dart:math';

import 'package:fixnum/fixnum.dart';
import 'package:flutter/material.dart';

import 'generated/schema/landmark.pb.dart';
import 'sign_gemma_client.dart';

class SlrInputWidget extends StatefulWidget {
  const SlrInputWidget({
    super.key,
    required this.onSignRecognized,
    this.bridgeUrl = 'ws://127.0.0.1:8080/ws/sign',
    this.sessionId,
    this.autoStreamMockFrames = true,
    this.frameInterval = const Duration(milliseconds: 900),
    this.placeholder = '브리지 서버에 연결 중입니다...',
  });

  final ValueChanged<String> onSignRecognized;
  final String bridgeUrl;
  final String? sessionId;
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
  String _statusText = '';
  bool _connected = false;

  @override
  void initState() {
    super.initState();
    _sessionId = widget.sessionId ?? _createSessionId();
    _statusText = widget.placeholder;
    _client = SignGemmaClient(url: widget.bridgeUrl)
      ..onTranslation = (result) {
        if (!mounted) {
          return;
        }
        setState(() {
          _statusText = result.text.isEmpty ? widget.placeholder : result.text;
          _connected = true;
        });
        widget.onSignRecognized(result.text);
      };
    _connect();
  }

  Future<void> _connect() async {
    _client.connect();
    if (!mounted) {
      return;
    }
    setState(() {
      _connected = true;
      _statusText = '연결되었습니다. 랜드마크 스트림을 전송하는 중입니다...';
    });

    if (widget.autoStreamMockFrames) {
      _streamTimer = Timer.periodic(widget.frameInterval, (_) {
        _client.sendFrames(_buildMockFrames(), _sessionId);
      });
    }
  }

  @override
  void dispose() {
    _streamTimer?.cancel();
    unawaited(_client.disconnect());
    super.dispose();
  }

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
                  _connected ? Icons.cloud_done_outlined : Icons.cloud_off_outlined,
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
              '현재 구현은 V2 브리지 검증용 mock landmark stream을 전송합니다.',
              style: theme.textTheme.bodyMedium?.copyWith(color: Colors.white70),
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
    final random = Random((DateTime.now().microsecondsSinceEpoch * seed).round());
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
