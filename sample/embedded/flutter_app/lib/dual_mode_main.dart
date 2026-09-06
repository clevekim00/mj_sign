/// Standalone entry point for the embedded/backend recognition sample.
library;

import 'package:flutter/material.dart';

import 'samples/dual_mode_recognition_sample.dart';

void main() {
  runApp(
    MaterialApp(
      theme: ThemeData(useMaterial3: true),
      home: const DualModeRecognitionSample(),
    ),
  );
}
