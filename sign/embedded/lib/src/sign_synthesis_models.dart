import 'dart:convert';

import 'package:fixnum/fixnum.dart';

import 'generated/schema/landmark.pb.dart';

class SignSynthesisResult {
  const SignSynthesisResult({
    required this.sessionId,
    required this.sourceType,
    required this.text,
    required this.locale,
    required this.signLanguage,
    required this.modelProfile,
    required this.protocolVersion,
    required this.glosses,
    required this.nonManualMarkers,
    required this.frames,
    required this.isFinal,
    required this.confidence,
    this.error,
  });

  final String sessionId;
  final String sourceType;
  final String text;
  final String locale;
  final String signLanguage;
  final String modelProfile;
  final String protocolVersion;
  final List<String> glosses;
  final List<String> nonManualMarkers;
  final List<LandmarkFrame> frames;
  final bool isFinal;
  final double confidence;
  final String? error;

  factory SignSynthesisResult.fromJsonString(String value) {
    final decoded = jsonDecode(value);
    if (decoded is! Map<String, dynamic>) {
      throw const FormatException(
        'Sign synthesis response must be a JSON object.',
      );
    }
    return SignSynthesisResult.fromJson(decoded);
  }

  factory SignSynthesisResult.fromJson(Map<String, dynamic> json) {
    final plan = _mapValue(json['sign_plan']);
    final motion = _mapValue(json['motion']);
    final frames = _listValue(
      motion['frames'],
    ).map(_mapValue).map(_landmarkFrameFromJson).toList(growable: false);

    return SignSynthesisResult(
      sessionId: json['session_id'] as String? ?? '',
      sourceType: json['source_type'] as String? ?? 'text',
      text: json['text'] as String? ?? '',
      locale: json['locale'] as String? ?? 'ko-KR',
      signLanguage: json['sign_language'] as String? ?? 'ksl',
      modelProfile: json['model_profile'] as String? ?? 'sign-gemma-ko',
      protocolVersion:
          json['protocol_version'] as String? ?? 'signbridge-synthesis-v1',
      glosses: _stringList(plan['glosses']),
      nonManualMarkers: _stringList(plan['non_manual_markers']),
      frames: frames,
      isFinal: json['is_final'] as bool? ?? true,
      confidence: (json['confidence'] as num?)?.toDouble() ?? 0,
      error: json['error'] as String?,
    );
  }
}

LandmarkFrame _landmarkFrameFromJson(Map<String, dynamic> json) {
  return LandmarkFrame()
    ..timestampMs = Int64((json['timestamp_ms'] as num?)?.toInt() ?? 0)
    ..leftHand.addAll(_pointsFromJson(json['left_hand']))
    ..rightHand.addAll(_pointsFromJson(json['right_hand']))
    ..pose.addAll(_pointsFromJson(json['pose']))
    ..faceContour.addAll(_pointsFromJson(json['face_contour']));
}

List<Point3D> _pointsFromJson(Object? value) {
  return _listValue(value)
      .map(_mapValue)
      .map(
        (point) => Point3D()
          ..x = (point['x'] as num?)?.toDouble() ?? 0
          ..y = (point['y'] as num?)?.toDouble() ?? 0
          ..z = (point['z'] as num?)?.toDouble() ?? 0,
      )
      .toList(growable: false);
}

List<String> _stringList(Object? value) {
  return _listValue(
    value,
  ).map((item) => item.toString()).toList(growable: false);
}

List<dynamic> _listValue(Object? value) {
  if (value is List<dynamic>) {
    return value;
  }
  return const <dynamic>[];
}

Map<String, dynamic> _mapValue(Object? value) {
  if (value is Map<String, dynamic>) {
    return value;
  }
  if (value is Map) {
    return value.map((key, value) => MapEntry(key.toString(), value));
  }
  return const <String, dynamic>{};
}
