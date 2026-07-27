import 'package:flutter/material.dart';

enum SlrSamplePlatform { android, ios, ipad, web, windows, macos, linux }

class PlatformSampleProfile {
  const PlatformSampleProfile({
    required this.platform,
    required this.title,
    required this.tagline,
    required this.recommendedBridgeUrl,
    required this.runCommand,
    required this.landmarkSource,
    required this.accentColor,
    required this.icon,
    required this.setupChecklist,
    required this.productionChecklist,
    this.locale = 'en-US',
    this.signLanguage = 'asl',
    this.modelProfile = 'sign-gemma',
    this.synthesisPrompt = 'I need help tomorrow.',
  });

  final SlrSamplePlatform platform;
  final String title;
  final String tagline;
  final String recommendedBridgeUrl;
  final String runCommand;
  final String landmarkSource;
  final Color accentColor;
  final IconData icon;
  final List<String> setupChecklist;
  final List<String> productionChecklist;
  final String locale;
  final String signLanguage;
  final String modelProfile;
  final String synthesisPrompt;

  String get id => platform.name;
}
