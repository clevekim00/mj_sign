import 'package:flutter/material.dart';

import 'sample_profile.dart';

const ipadSampleProfile = PlatformSampleProfile(
  platform: SlrSamplePlatform.ipad,
  title: 'iPad',
  tagline:
      'split-pane layout for classroom, kiosk, and accessibility stations.',
  recommendedBridgeUrl: 'ws://127.0.0.1:8080/ws/sign',
  runCommand: 'flutter run -d <ipad-device-id>',
  landmarkSource: 'wide preview camera source with larger correction controls',
  accentColor: Color(0xFFFFB454),
  icon: Icons.tablet_mac,
  setupChecklist: [
    'Use the same iOS target with an iPad-optimized responsive layout.',
    'Prefer landscape orientation when presenting live camera and text together.',
    'Use a LAN bridge URL when the backend runs on another machine.',
  ],
  productionChecklist: [
    'Expose larger result editing controls for accessibility workflows.',
    'Persist session IDs when users move between split-view apps.',
    'Tune idle flush longer for classroom or guided input scenarios.',
  ],
);
