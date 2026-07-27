import 'package:fixnum/fixnum.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:slr_input_kit/slr_input_kit.dart';

void main() {
  test('MediaPipeLandmarkExtraction limits oversized batches', () {
    final extraction = MediaPipeLandmarkExtraction(
      frames: List<LandmarkFrame>.generate(
        5,
        (index) => LandmarkFrame()..timestampMs = Int64(index),
      ),
    );

    final limited = extraction.limitedTo(3);

    expect(limited.frames, hasLength(3));
    expect(limited.frames.last.timestampMs.toInt(), 2);
  });

  test(
    'MediaPipeLandmarkExtraction applies include flags and front mirror',
    () {
      final extraction = MediaPipeLandmarkExtraction(
        frames: [
          LandmarkFrame()
            ..timestampMs = Int64(42)
            ..leftHand.add(Point3D()..x = 0.2)
            ..rightHand.add(Point3D()..x = 0.8)
            ..pose.add(Point3D()..x = 0.4)
            ..faceContour.add(Point3D()..x = 0.6),
        ],
      );

      final prepared = extraction.preparedFor(
        const MediaPipeLandmarkExtractionConfig(
          includeHands: true,
          includePose: false,
          includeFaceContour: true,
        ),
        mirror: true,
      );

      final frame = prepared.frames.single;
      expect(frame.timestampMs.toInt(), 42);
      expect(frame.leftHand.single.x, closeTo(0.8, 0.0001));
      expect(frame.rightHand.single.x, closeTo(0.2, 0.0001));
      expect(frame.pose, isEmpty);
      expect(frame.faceContour.single.x, closeTo(0.4, 0.0001));
    },
  );

  test(
    'MediaPipeLandmarkFrameSource can be constructed with extractor config',
    () {
      final source = MediaPipeLandmarkFrameSource(
        config: const MediaPipeLandmarkExtractionConfig(maxFramesPerBatch: 2),
        extractor: (image, camera, config) {
          expect(config.maxFramesPerBatch, 2);
          return const MediaPipeLandmarkExtraction(frames: <LandmarkFrame>[]);
        },
      );

      expect(source.frames, isA<Stream<List<LandmarkFrame>>>());
    },
  );
}
