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
