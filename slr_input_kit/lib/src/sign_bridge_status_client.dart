import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

class SignBridgeStatusSnapshot {
  const SignBridgeStatusSnapshot({
    required this.baseUrl,
    required this.checkedAt,
    this.healthStatus,
    this.readinessStatus,
    this.provider,
    this.modelBaseUrl,
    this.error,
  });

  final String baseUrl;
  final DateTime checkedAt;
  final String? healthStatus;
  final String? readinessStatus;
  final String? provider;
  final String? modelBaseUrl;
  final String? error;

  bool get healthUp => healthStatus == 'UP';
  bool get ready => readinessStatus == 'READY';
  bool get hasError => error != null && error!.isNotEmpty;

  String get swaggerUiUrl => _joinUrl(baseUrl, '/swagger-ui.html');
  String get openApiUrl => _joinUrl(baseUrl, '/v3/api-docs');
  String get profilesUrl => _joinUrl(baseUrl, '/api/v2/model-profiles');
  String get readinessUrl => _joinUrl(baseUrl, '/internal/readyz');
  String get prometheusMetricsUrl =>
      _joinUrl(baseUrl, '/internal/metrics.prometheus');

  SignBridgeStatusSnapshot copyWith({
    String? healthStatus,
    String? readinessStatus,
    String? provider,
    String? modelBaseUrl,
    String? error,
  }) {
    return SignBridgeStatusSnapshot(
      baseUrl: baseUrl,
      checkedAt: checkedAt,
      healthStatus: healthStatus ?? this.healthStatus,
      readinessStatus: readinessStatus ?? this.readinessStatus,
      provider: provider ?? this.provider,
      modelBaseUrl: modelBaseUrl ?? this.modelBaseUrl,
      error: error ?? this.error,
    );
  }

  static String _joinUrl(String base, String path) {
    if (base.endsWith('/') && path.startsWith('/')) {
      return '${base.substring(0, base.length - 1)}$path';
    }
    if (!base.endsWith('/') && !path.startsWith('/')) {
      return '$base/$path';
    }
    return '$base$path';
  }
}

class SignBridgeStatusHttpClient {
  const SignBridgeStatusHttpClient({
    this.baseUrl = 'http://127.0.0.1:8080',
    this.timeout = const Duration(seconds: 3),
  });

  final String baseUrl;
  final Duration timeout;

  Future<SignBridgeStatusSnapshot> probe() async {
    var snapshot = SignBridgeStatusSnapshot(
      baseUrl: baseUrl,
      checkedAt: DateTime.now(),
    );

    try {
      final health = await _getJson('/internal/healthz');
      snapshot = snapshot.copyWith(
        healthStatus: health['status'] as String?,
        provider: _stringAt(health, const ['gpu', 'provider']),
        modelBaseUrl: _stringAt(health, const ['gpu', 'base_url']),
      );
    } catch (error) {
      return snapshot.copyWith(error: 'Health check failed: $error');
    }

    try {
      final readiness = await _getJson('/internal/readyz');
      return snapshot.copyWith(
        readinessStatus: readiness['status'] as String?,
        provider: _stringAt(readiness, const ['gpu', 'provider']),
        modelBaseUrl:
            _stringAt(readiness, const ['gpu', 'base_url']) ??
            snapshot.modelBaseUrl,
      );
    } catch (error) {
      return snapshot.copyWith(
        readinessStatus: 'NOT_READY',
        error: 'Readiness check failed: $error',
      );
    }
  }

  Future<Map<String, dynamic>> _getJson(String path) async {
    final response = await http.get(Uri.parse(_joinUrl(path))).timeout(timeout);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      if (response.statusCode == 503 && response.body.isNotEmpty) {
        return _decodeObject(response.body);
      }
      throw SignBridgeStatusException(
        'HTTP ${response.statusCode}',
        response.body,
      );
    }
    return _decodeObject(response.body);
  }

  Map<String, dynamic> _decodeObject(String source) {
    final decoded = jsonDecode(source);
    if (decoded is! Map<String, dynamic>) {
      throw const FormatException('Expected JSON object.');
    }
    return decoded;
  }

  String _joinUrl(String path) {
    if (baseUrl.endsWith('/') && path.startsWith('/')) {
      return '${baseUrl.substring(0, baseUrl.length - 1)}$path';
    }
    if (!baseUrl.endsWith('/') && !path.startsWith('/')) {
      return '$baseUrl/$path';
    }
    return '$baseUrl$path';
  }

  String? _stringAt(Map<String, dynamic> json, List<String> path) {
    Object? current = json;
    for (final segment in path) {
      if (current is! Map<String, dynamic>) {
        return null;
      }
      current = current[segment];
    }
    return current as String?;
  }
}

class SignBridgeStatusException implements Exception {
  const SignBridgeStatusException(this.message, this.responseBody);

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
