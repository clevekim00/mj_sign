import 'dart:convert';

import 'package:http/http.dart' as http;

import 'sign_synthesis_models.dart';

class SignSynthesisHttpClient {
  const SignSynthesisHttpClient({this.baseUrl = 'http://127.0.0.1:8080'});

  final String baseUrl;

  Future<SignSynthesisResult> synthesizeText({
    required String sessionId,
    required String text,
    String locale = 'en-US',
    String signLanguage = 'asl',
    String modelProfile = 'sign-gemma',
  }) {
    return _post('/api/v2/sign/synthesize', {
      'session_id': sessionId,
      'source_type': 'text',
      'text': text,
      'locale': locale,
      'sign_language': signLanguage,
      'model_profile': modelProfile,
      'output_format': 'landmarks',
      'protocol_version': 'signbridge-synthesis-v1',
    });
  }

  Future<SignSynthesisResult> synthesizeSpeechTranscript({
    required String sessionId,
    required String transcript,
    String locale = 'en-US',
    String signLanguage = 'asl',
    String modelProfile = 'sign-gemma',
  }) {
    return _post('/api/v2/speech/sign', {
      'session_id': sessionId,
      'source_type': 'speech',
      'transcript': transcript,
      'locale': locale,
      'sign_language': signLanguage,
      'model_profile': modelProfile,
      'output_format': 'landmarks',
      'protocol_version': 'signbridge-synthesis-v1',
    });
  }

  Future<SignSynthesisResult> _post(
    String path,
    Map<String, Object?> request,
  ) async {
    final response = await http.post(
      Uri.parse(_joinUrl(baseUrl, path)),
      headers: const {'Content-Type': 'application/json'},
      body: jsonEncode(request),
    );
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw SignSynthesisException(
        'Sign synthesis failed with HTTP ${response.statusCode}.',
        response.body,
      );
    }
    return SignSynthesisResult.fromJsonString(response.body);
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

class SignSynthesisException implements Exception {
  const SignSynthesisException(this.message, this.responseBody);

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
