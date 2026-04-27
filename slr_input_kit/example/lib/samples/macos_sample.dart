import 'package:flutter/material.dart';

import 'sample_profile.dart';

const macosSampleProfile = PlatformSampleProfile(
  platform: SlrSamplePlatform.macos,
  title: 'macOS / OSX',
  tagline: 'native desktop bridge validation with camera permission handling.',
  recommendedBridgeUrl: 'ws://127.0.0.1:8080/ws/sign',
  runCommand: 'flutter run -d macos',
  landmarkSource:
      'FaceTime/USB camera frames converted into LandmarkFrame data',
  accentColor: Color(0xFFA7F3D0),
  icon: Icons.laptop_mac,
  setupChecklist: [
    'Enable macOS desktop support in Flutter before running the sample.',
    'Add camera permission text to the host app when replacing demo frames.',
    'Use 127.0.0.1 for local bridge testing on the same Mac.',
  ],
  productionChecklist: [
    'Use a signed and notarized app when distributing outside development.',
    'Stop the camera source when the app is backgrounded or hidden.',
    'Keep the bridge URL configurable for edge GPU deployments.',
  ],
);
