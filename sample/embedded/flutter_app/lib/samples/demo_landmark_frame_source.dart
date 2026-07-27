import 'dart:async';
import 'dart:math';

import 'package:fixnum/fixnum.dart';
import 'package:slr_input_kit/slr_input_kit.dart';

import 'sample_profile.dart';

class DemoLandmarkFrameSource implements LandmarkFrameSource {
  DemoLandmarkFrameSource({
    required this.profile,
    this.interval = const Duration(milliseconds: 180),
  });

  final PlatformSampleProfile profile;
  final Duration interval;

  final StreamController<List<LandmarkFrame>> _controller =
      StreamController<List<LandmarkFrame>>.broadcast();

  Timer? _timer;
  int _tick = 0;

  @override
  Stream<List<LandmarkFrame>> get frames => _controller.stream;

  @override
  Future<void> start() async {
    if (_timer != null) {
      return;
    }
    _emitFrameBatch();
    _timer = Timer.periodic(interval, (_) => _emitFrameBatch());
  }

  @override
  Future<void> stop() async {
    _timer?.cancel();
    _timer = null;
  }

  @override
  Future<void> dispose() async {
    await stop();
    await _controller.close();
  }

  void _emitFrameBatch() {
    if (_controller.isClosed) {
      return;
    }
    _controller.add([
      _buildFrame(phaseOffset: 0),
      _buildFrame(phaseOffset: 0.35),
      _buildFrame(phaseOffset: 0.7),
    ]);
    _tick++;
  }

  LandmarkFrame _buildFrame({required double phaseOffset}) {
    final phase = (_tick / 8) + phaseOffset + profile.platform.index;
    return LandmarkFrame()
      ..timestampMs = Int64(DateTime.now().millisecondsSinceEpoch)
      ..leftHand.addAll(_handPoints(phase, mirrored: true))
      ..rightHand.addAll(_handPoints(phase, mirrored: false))
      ..pose.addAll(_posePoints(phase))
      ..faceContour.addAll(_facePoints(phase));
  }

  List<Point3D> _handPoints(double phase, {required bool mirrored}) {
    final direction = mirrored ? -1.0 : 1.0;
    final baseX = mirrored ? 0.34 : 0.66;
    return List<Point3D>.generate(21, (index) {
      final finger = index % 5;
      final joint = index ~/ 5;
      final wave = sin(phase + index * 0.28) * 0.035;
      return Point3D()
        ..x = _unit(baseX + direction * finger * 0.025 + wave)
        ..y = _unit(0.34 + joint * 0.055 + cos(phase + index) * 0.018)
        ..z = _unit(0.45 + sin(phase + finger) * 0.08);
    });
  }

  List<Point3D> _posePoints(double phase) {
    return List<Point3D>.generate(12, (index) {
      final side = index.isEven ? -1.0 : 1.0;
      return Point3D()
        ..x = _unit(0.5 + side * (0.08 + (index % 3) * 0.035))
        ..y = _unit(0.18 + (index ~/ 2) * 0.08 + sin(phase) * 0.01)
        ..z = _unit(0.5 + cos(phase + index) * 0.05);
    });
  }

  List<Point3D> _facePoints(double phase) {
    return List<Point3D>.generate(16, (index) {
      final angle = (2 * pi * index / 16) + phase * 0.08;
      return Point3D()
        ..x = _unit(0.5 + cos(angle) * 0.08)
        ..y = _unit(0.18 + sin(angle) * 0.06)
        ..z = _unit(0.48 + sin(phase + index) * 0.02);
    });
  }

  double _unit(double value) => value.clamp(0.0, 1.0).toDouble();
}
