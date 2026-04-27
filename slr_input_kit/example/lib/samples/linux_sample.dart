import 'package:flutter/material.dart';

import 'sample_profile.dart';

const linuxSampleProfile = PlatformSampleProfile(
  platform: SlrSamplePlatform.linux,
  title: 'Linux',
  tagline: 'edge workstation sample for local GPU and broker experiments.',
  recommendedBridgeUrl: 'ws://127.0.0.1:8080/ws/sign',
  runCommand: 'flutter run -d linux',
  landmarkSource: 'V4L2 camera frames or a local Python landmark sidecar',
  accentColor: Color(0xFFFACC15),
  icon: Icons.terminal,
  setupChecklist: [
    'Install Flutter Linux desktop dependencies and a working CMake toolchain.',
    'Run the integrated Kafka or RabbitMQ stack when testing queue providers.',
    'Use the demo source first to confirm WebSocket and inference contracts.',
  ],
  productionChecklist: [
    'Pin GPU drivers and container runtime versions for edge deployments.',
    'Run the bridge, broker, and serving backend under supervised services.',
    'Export health, readiness, and metrics endpoints to the local observability stack.',
  ],
);
