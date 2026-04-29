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
           final extraction = await extractor(image, camera, config);
           return extraction.limitedTo(config.maxFramesPerBatch).frames;
         },
       );
}
