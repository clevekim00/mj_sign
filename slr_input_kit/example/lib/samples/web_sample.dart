import 'package:flutter/material.dart';

import 'sample_profile.dart';

const webSampleProfile = PlatformSampleProfile(
  platform: SlrSamplePlatform.web,
  title: 'Web',
  tagline: 'browser-first sign input through WebSocket bridge events.',
  recommendedBridgeUrl: 'ws://localhost:8080/ws/sign',
  runCommand: 'flutter run -d chrome',
  landmarkSource:
      'browser camera frames or a JavaScript/WASM landmark pipeline',
  accentColor: Color(0xFF38BDF8),
  icon: Icons.public,
  setupChecklist: [
    'Run the Spring bridge on localhost:8080 before opening the web sample.',
    'Use secure origins when enabling camera capture outside localhost.',
    'Keep CORS and WebSocket proxy rules aligned with the deployed bridge host.',
  ],
  productionChecklist: [
    'Move landmark extraction to WebAssembly or WebGPU where available.',
    'Use WSS and short-lived session IDs for public deployments.',
    'Add browser capability checks before showing camera capture controls.',
  ],
);
