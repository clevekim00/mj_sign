import 'dart:convert';

import 'package:http/http.dart' as http;

import 'sign_gemma_client.dart';

class SignModelProfile {
  const SignModelProfile({
    required this.locale,
    required this.signLanguage,
    required this.modelProfile,
    this.protocolVersion = 'signbridge-model-v1',
    this.isDefault = false,
  });

  final String locale;
  final String signLanguage;
  final String modelProfile;
  final String protocolVersion;
  final bool isDefault;

  SignLanguageContext toLanguageContext() {
    return SignLanguageContext(
      locale: locale,
      signLanguage: signLanguage,
      modelProfile: modelProfile,
      protocolVersion: protocolVersion,
    );
  }

  factory SignModelProfile.fromJson(Map<String, dynamic> json) {
    return SignModelProfile(
      locale: json['locale'] as String? ?? 'en-US',
      signLanguage: json['sign_language'] as String? ?? 'asl',
      modelProfile: json['model_profile'] as String? ?? 'sign-gemma',
      protocolVersion:
          json['protocol_version'] as String? ?? 'signbridge-model-v1',
      isDefault: json['is_default'] as bool? ?? false,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is SignModelProfile &&
        locale == other.locale &&
        signLanguage == other.signLanguage &&
        modelProfile == other.modelProfile &&
        protocolVersion == other.protocolVersion;
  }

  @override
  int get hashCode {
    return Object.hash(locale, signLanguage, modelProfile, protocolVersion);
  }
}

class SignModelProfileCatalog {
  const SignModelProfileCatalog({
    required this.defaultProfile,
    required this.profiles,
  });

  final SignModelProfile defaultProfile;
  final List<SignModelProfile> profiles;

  factory SignModelProfileCatalog.fromJson(Map<String, dynamic> json) {
    final profilesJson = json['profiles'];
    final profiles = profilesJson is List
        ? profilesJson
              .whereType<Map<String, dynamic>>()
              .map(SignModelProfile.fromJson)
              .toList(growable: false)
        : const <SignModelProfile>[];
    final defaultJson = json['default_profile'];
    final defaultProfile = defaultJson is Map<String, dynamic>
        ? SignModelProfile.fromJson(defaultJson)
        : profiles.firstWhere(
            (profile) => profile.isDefault,
            orElse: () => const SignModelProfile(
              locale: 'ko-KR',
              signLanguage: 'ksl',
              modelProfile: 'sign-gemma-ko',
              isDefault: true,
            ),
          );

    return SignModelProfileCatalog(
      defaultProfile: defaultProfile,
      profiles: profiles.isEmpty
          ? <SignModelProfile>[defaultProfile]
          : profiles,
    );
  }

  factory SignModelProfileCatalog.fromJsonString(String source) {
    final json = jsonDecode(source);
    if (json is! Map<String, dynamic>) {
      throw const FormatException('Expected model profile catalog object.');
    }
    return SignModelProfileCatalog.fromJson(json);
  }
}

class SignModelProfileHttpClient {
  const SignModelProfileHttpClient({
    this.baseUrl = 'http://127.0.0.1:8080',
    this.timeout = const Duration(seconds: 3),
  });

  final String baseUrl;
  final Duration timeout;

  Future<SignModelProfileCatalog> fetchProfiles() async {
    final response = await http
        .get(Uri.parse(_joinUrl(baseUrl, '/api/v2/model-profiles')))
        .timeout(timeout);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw SignModelProfileException(
        'Model profile discovery failed with HTTP ${response.statusCode}.',
        response.body,
      );
    }
    return SignModelProfileCatalog.fromJsonString(response.body);
  }

  String _joinUrl(String base, String path) {
    if (base.endsWith('/') && path.startsWith('/')) {
      return '${base.substring(0, base.length - 1)}$path';
    }
    if (!base.endsWith('/') && !path.startsWith('/')) {
      return '$base/$path';
    }
    return '$base$path';
  }
}

class SignModelProfileException implements Exception {
  const SignModelProfileException(this.message, this.responseBody);

  final String message;
  final String responseBody;

  @override
  String toString() {
    if (responseBody.isEmpty) {
      return message;
    }
    return '$message $responseBody';
  }
}
