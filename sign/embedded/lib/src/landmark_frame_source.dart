import 'dart:async';

import 'package:camera/camera.dart';

import 'generated/schema/landmark.pb.dart';

typedef LandmarkFrameBatchExtractor =
    FutureOr<List<LandmarkFrame>> Function(
      CameraImage image,
      CameraDescription camera,
    );

abstract interface class LandmarkFrameSource {
  Stream<List<LandmarkFrame>> get frames;

  Future<void> start();

  Future<void> stop();

  Future<void> dispose();
}

class CameraLandmarkFrameSource implements LandmarkFrameSource {
  CameraLandmarkFrameSource({
    required LandmarkFrameBatchExtractor extractor,
    CameraDescription? camera,
    CameraLensDirection preferredLensDirection = CameraLensDirection.front,
    ResolutionPreset resolutionPreset = ResolutionPreset.medium,
    ImageFormatGroup imageFormatGroup = ImageFormatGroup.yuv420,
    Duration minFrameInterval = const Duration(milliseconds: 120),
  }) : _extractor = extractor,
       _camera = camera,
       _preferredLensDirection = preferredLensDirection,
       _resolutionPreset = resolutionPreset,
       _imageFormatGroup = imageFormatGroup,
       _minFrameInterval = minFrameInterval;

  final LandmarkFrameBatchExtractor _extractor;
  final CameraDescription? _camera;
  final CameraLensDirection _preferredLensDirection;
  final ResolutionPreset _resolutionPreset;
  final ImageFormatGroup _imageFormatGroup;
  final Duration _minFrameInterval;

  final StreamController<List<LandmarkFrame>> _framesController =
      StreamController<List<LandmarkFrame>>.broadcast();

  CameraController? _controller;
  DateTime? _lastAcceptedFrameAt;
  bool _extracting = false;
  bool _running = false;

  CameraController? get cameraController => _controller;

  @override
  Stream<List<LandmarkFrame>> get frames => _framesController.stream;

  @override
  Future<void> start() async {
    if (_running) {
      return;
    }
    if (_framesController.isClosed) {
      throw StateError('CameraLandmarkFrameSource has already been disposed.');
    }

    final camera = _camera ?? await _selectCamera();
    final controller = CameraController(
      camera,
      _resolutionPreset,
      enableAudio: false,
      imageFormatGroup: _imageFormatGroup,
    );

    try {
      await controller.initialize();
      _controller = controller;
      _running = true;

      await controller.startImageStream((image) {
        unawaited(_handleImage(camera, image));
      });
    } catch (_) {
      _running = false;
      _controller = null;
      await controller.dispose();
      rethrow;
    }
  }

  @override
  Future<void> stop() async {
    _running = false;
    final controller = _controller;
    if (controller == null) {
      return;
    }

    if (controller.value.isStreamingImages) {
      await controller.stopImageStream();
    }
    _lastAcceptedFrameAt = null;
    _extracting = false;
  }

  @override
  Future<void> dispose() async {
    await stop();
    await _controller?.dispose();
    _controller = null;
    await _framesController.close();
  }

  Future<void> _handleImage(CameraDescription camera, CameraImage image) async {
    if (!_running || _extracting || !_shouldAcceptFrame()) {
      return;
    }

    _extracting = true;
    try {
      final frames = await _extractor(image, camera);
      if (frames.isNotEmpty && !_framesController.isClosed) {
        _framesController.add(frames);
      }
    } catch (error, stackTrace) {
      if (!_framesController.isClosed) {
        _framesController.addError(error, stackTrace);
      }
    } finally {
      _extracting = false;
    }
  }

  bool _shouldAcceptFrame() {
    final now = DateTime.now();
    final lastAcceptedFrameAt = _lastAcceptedFrameAt;
    if (lastAcceptedFrameAt != null &&
        now.difference(lastAcceptedFrameAt) < _minFrameInterval) {
      return false;
    }
    _lastAcceptedFrameAt = now;
    return true;
  }

  Future<CameraDescription> _selectCamera() async {
    final cameras = await availableCameras();
    if (cameras.isEmpty) {
      throw StateError('No cameras available for landmark capture.');
    }

    return cameras.firstWhere(
      (camera) => camera.lensDirection == _preferredLensDirection,
      orElse: () => cameras.first,
    );
  }
}

class StreamLandmarkFrameSource implements LandmarkFrameSource {
  StreamLandmarkFrameSource(this.frames);

  @override
  final Stream<List<LandmarkFrame>> frames;

  @override
  Future<void> start() async {}

  @override
  Future<void> stop() async {}

  @override
  Future<void> dispose() async {}
}
