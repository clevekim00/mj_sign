import 'dart:async';

import 'package:flutter/material.dart';
import 'package:slr_input_kit/slr_input_kit.dart';

import 'demo_landmark_frame_source.dart';
import 'sample_profiles.dart';

/// Demonstrates that the same input widget and landmark source can switch
/// between an on-device delegate and the SignBridge backend protocol.
class DualModeRecognitionSample extends StatefulWidget {
  const DualModeRecognitionSample({super.key});

  @override
  State<DualModeRecognitionSample> createState() =>
      _DualModeRecognitionSampleState();
}

class _DualModeRecognitionSampleState extends State<DualModeRecognitionSample> {
  SignRecognitionMode _mode = SignRecognitionMode.embedded;
  late DemoLandmarkFrameSource _source;
  late SignRecognitionEngine _engine;
  String _result = '결과 대기 중';

  @override
  void initState() {
    super.initState();
    _recreateRuntime();
  }

  void _recreateRuntime() {
    _source = DemoLandmarkFrameSource(profile: platformSampleProfiles.first);
    _engine = switch (_mode) {
      SignRecognitionMode.embedded => EmbeddedSignRecognitionEngine(
        delegate: _demoEmbeddedDelegate,
        languageContext: const SignLanguageContext(
          locale: 'ko-KR',
          signLanguage: 'ksl',
          modelProfile: 'embedded-demo-ko',
        ),
      ),
      SignRecognitionMode.backend => BackendSignRecognitionEngine(
        client: SignGemmaClient(
          languageContext: const SignLanguageContext(
            locale: 'ko-KR',
            signLanguage: 'ksl',
            modelProfile: 'sign-gemma-ko',
          ),
        ),
      ),
    };
  }

  Future<TranslationResult> _demoEmbeddedDelegate(
    EmbeddedRecognitionRequest request,
  ) async {
    // Replace this delegate with a TFLite/Core ML/MediaPipe Tasks invocation.
    final pointCount = request.frames.fold<int>(
      0,
      (total, frame) => total + frame.leftHand.length + frame.rightHand.length,
    );
    return TranslationResult(
      sessionId: request.sessionId,
      text: pointCount > 0 ? '안녕하세요 (내장 데모)' : '',
      isFinal: true,
      confidence: pointCount > 0 ? 0.9 : 0,
    );
  }

  Future<void> _switchMode(SignRecognitionMode mode) async {
    if (mode == _mode) return;
    final oldEngine = _engine;
    final oldSource = _source;
    setState(() {
      _mode = mode;
      _result = '모드 전환 중';
      _recreateRuntime();
    });
    await oldSource.dispose();
    await oldEngine.stop();
  }

  @override
  void dispose() {
    unawaited(_source.dispose());
    unawaited(_engine.stop());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Dual-mode sign recognition')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            SegmentedButton<SignRecognitionMode>(
              segments: const [
                ButtonSegment(
                  value: SignRecognitionMode.embedded,
                  label: Text('기기 내장'),
                  icon: Icon(Icons.phone_android),
                ),
                ButtonSegment(
                  value: SignRecognitionMode.backend,
                  label: Text('서버 연결'),
                  icon: Icon(Icons.cloud_outlined),
                ),
              ],
              selected: {_mode},
              onSelectionChanged: (selection) => _switchMode(selection.single),
            ),
            const SizedBox(height: 16),
            const Text('구조 검증용 데모입니다. 실제 수어 인식 모델은 연결되어 있지 않습니다.'),
            const SizedBox(height: 8),
            Expanded(
              child: SlrInputWidget(
                key: ValueKey(_mode),
                recognitionEngine: _engine,
                landmarkFrameSource: _source,
                autoReconnect: _mode == SignRecognitionMode.backend,
                onSignRecognized: (text) => setState(() => _result = text),
              ),
            ),
            const SizedBox(height: 12),
            Semantics(liveRegion: true, child: Text('인식 결과: $_result')),
          ],
        ),
      ),
    );
  }
}
