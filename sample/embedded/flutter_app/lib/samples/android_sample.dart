import 'package:flutter/material.dart';

import 'sample_profile.dart';

const androidSampleProfile = PlatformSampleProfile(
  platform: SlrSamplePlatform.android,
  title: 'Android phone',
  tagline:
      'front camera gesture capture with emulator-friendly bridge routing.',
  recommendedBridgeUrl: 'ws://10.0.2.2:8080/ws/sign',
  runCommand: 'flutter run -d android',
  landmarkSource: 'CameraLandmarkFrameSource + MediaPipe/ML Kit hand landmarks',
  accentColor: Color(0xFF3DDC84),
  icon: Icons.android,
  setupChecklist: [
    'Use 10.0.2.2 when the Spring bridge runs on the host machine.',
    'Grant camera permission before starting the landmark stream.',
    'Prefer the front camera and medium resolution for stable real-time input.',
  ],
  productionChecklist: [
    'Add an on-device hand landmark extractor instead of the demo frame source.',
    'Throttle landmark batches to 8-12 FPS before sending them to the bridge.',
    'Use a LAN or HTTPS/WSS endpoint for physical device testing.',
  ],
);
