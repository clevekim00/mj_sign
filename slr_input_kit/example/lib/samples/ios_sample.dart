import 'package:flutter/material.dart';

import 'sample_profile.dart';

const iosSampleProfile = PlatformSampleProfile(
  platform: SlrSamplePlatform.ios,
  title: 'iPhone',
  tagline: 'compact sign input surface for chat, search, and form fields.',
  recommendedBridgeUrl: 'ws://127.0.0.1:8080/ws/sign',
  runCommand: 'flutter run -d ios',
  landmarkSource:
      'AVFoundation camera frames through CameraLandmarkFrameSource',
  accentColor: Color(0xFF69B7FF),
  icon: Icons.phone_iphone,
  setupChecklist: [
    'Use 127.0.0.1 for iOS Simulator and a LAN IP for a physical iPhone.',
    'Add NSCameraUsageDescription to the host app Info.plist.',
    'Keep the sign input widget near the active text field for fast correction.',
  ],
  productionChecklist: [
    'Package a native or Dart landmark extractor behind LandmarkFrameSource.',
    'Handle app lifecycle pause/resume by stopping and restarting the source.',
    'Use WSS for remote bridge access outside local development.',
  ],
);
