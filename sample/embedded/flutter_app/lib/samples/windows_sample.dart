import 'package:flutter/material.dart';

import 'sample_profile.dart';

const windowsSampleProfile = PlatformSampleProfile(
  platform: SlrSamplePlatform.windows,
  title: 'Windows',
  tagline: 'desktop sign input for assistive typing and kiosk workflows.',
  recommendedBridgeUrl: 'ws://127.0.0.1:8080/ws/sign',
  runCommand: 'flutter run -d windows',
  landmarkSource: 'USB webcam source feeding LandmarkFrame batches',
  accentColor: Color(0xFF60A5FA),
  icon: Icons.desktop_windows,
  setupChecklist: [
    'Install Visual Studio C++ desktop tooling for the Flutter Windows runner.',
    'Run the bridge locally or point the sample at a reachable LAN endpoint.',
    'Use the demo landmark source when validating bridge behavior without GPU.',
  ],
  productionChecklist: [
    'Bundle a native camera and landmark extraction module with the app.',
    'Offer an always-on-top compact input panel for accessibility usage.',
    'Record bridge metrics during kiosk testing to tune frame batching.',
  ],
);
