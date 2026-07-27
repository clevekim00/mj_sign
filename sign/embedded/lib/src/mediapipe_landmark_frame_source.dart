import 'dart:async';

import 'package:camera/camera.dart';

import 'generated/schema/landmark.pb.dart';
import 'landmark_frame_source.dart';

typedef MediaPipeLandmarkExtractor =
    FutureOr<MediaPipeLandmarkExtraction> Function(
      CameraImage image,
      CameraDescription camera,
      MediaPipeLandmarkExtractionConfig config,
    );

class MediaPipeLandmarkExtractionConfig {
  const MediaPipeLandmarkExtractionConfig({
    this.includeHands = true,
    this.includePose = true,
    this.includeFaceContour = true,
    this.mirrorFrontCamera = true,
    this.maxFramesPerBatch = 3,
  });

  final bool includeHands;
  final bool includePose;
  final bool includeFaceContour;
  final bool mirrorFrontCamera;
  final int maxFramesPerBatch;
}

class MediaPipeLandmarkExtraction {
  const MediaPipeLandmarkExtraction({required this.frames});

  final List<LandmarkFrame> frames;

  MediaPipeLandmarkExtraction limitedTo(int maxFrames) {
    if (maxFrames <= 0 || frames.length <= maxFrames) {
      return this;
    }
    return MediaPipeLandmarkExtraction(
      frames: List<LandmarkFrame>.of(frames.take(maxFrames)),
    );
  }

  MediaPipeLandmarkExtraction preparedFor(
    MediaPipeLandmarkExtractionConfig config, {
    required bool mirror,
  }) {
    return MediaPipeLandmarkExtraction(
      frames: frames
          .map(
            (frame) => LandmarkFrame()
              ..timestampMs = frame.timestampMs
              ..leftHand.addAll(
                config.includeHands
                    ? _preparePoints(frame.leftHand, mirror: mirror)
                    : const <Point3D>[],
              )
              ..rightHand.addAll(
                config.includeHands
                    ? _preparePoints(frame.rightHand, mirror: mirror)
                    : const <Point3D>[],
              )
              ..pose.addAll(
                config.includePose
                    ? _preparePoints(frame.pose, mirror: mirror)
                    : const <Point3D>[],
              )
              ..faceContour.addAll(
                config.includeFaceContour
                    ? _preparePoints(frame.faceContour, mirror: mirror)
                    : const <Point3D>[],
              ),
          )
          .toList(growable: false),
    );
  }
}

class MediaPipeLandmarkFrameSource extends CameraLandmarkFrameSource {
  MediaPipeLandmarkFrameSource({
    required MediaPipeLandmarkExtractor extractor,
    MediaPipeLandmarkExtractionConfig config =
        const MediaPipeLandmarkExtractionConfig(),
    super.camera,
    super.preferredLensDirection,
    super.resolutionPreset,
    super.imageFormatGroup,
    super.minFrameInterval,
  }) : super(
         extractor: (image, camera) async {
           final mirror =
               config.mirrorFrontCamera &&
               camera.lensDirection == CameraLensDirection.front;
           final extraction = await extractor(image, camera, config);
           return extraction
               .preparedFor(config, mirror: mirror)
               .limitedTo(config.maxFramesPerBatch)
               .frames;
         },
       );
}

List<Point3D> _preparePoints(List<Point3D> points, {required bool mirror}) {
  return points
      .map(
        (point) => Point3D()
          ..x = mirror ? (1.0 - point.x).clamp(0.0, 1.0).toDouble() : point.x
          ..y = point.y
          ..z = point.z,
      )
      .toList(growable: false);
}
