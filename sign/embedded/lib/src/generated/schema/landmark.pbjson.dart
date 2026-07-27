// This is a generated file - do not edit.
//
// Generated from schema/landmark.proto.

// @dart = 3.3

// ignore_for_file: annotate_overrides, camel_case_types, comment_references
// ignore_for_file: constant_identifier_names
// ignore_for_file: curly_braces_in_flow_control_structures
// ignore_for_file: deprecated_member_use_from_same_package, library_prefixes
// ignore_for_file: non_constant_identifier_names, prefer_relative_imports
// ignore_for_file: unused_import

import 'dart:convert' as $convert;
import 'dart:core' as $core;
import 'dart:typed_data' as $typed_data;

@$core.Deprecated('Use point3DDescriptor instead')
const Point3D$json = {
  '1': 'Point3D',
  '2': [
    {'1': 'x', '3': 1, '4': 1, '5': 2, '10': 'x'},
    {'1': 'y', '3': 2, '4': 1, '5': 2, '10': 'y'},
    {'1': 'z', '3': 3, '4': 1, '5': 2, '10': 'z'},
  ],
};

/// Descriptor for `Point3D`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List point3DDescriptor = $convert.base64Decode(
    'CgdQb2ludDNEEgwKAXgYASABKAJSAXgSDAoBeRgCIAEoAlIBeRIMCgF6GAMgASgCUgF6');

@$core.Deprecated('Use landmarkFrameDescriptor instead')
const LandmarkFrame$json = {
  '1': 'LandmarkFrame',
  '2': [
    {'1': 'timestamp_ms', '3': 1, '4': 1, '5': 3, '10': 'timestampMs'},
    {
      '1': 'left_hand',
      '3': 2,
      '4': 3,
      '5': 11,
      '6': '.mj.sign.Point3D',
      '10': 'leftHand'
    },
    {
      '1': 'right_hand',
      '3': 3,
      '4': 3,
      '5': 11,
      '6': '.mj.sign.Point3D',
      '10': 'rightHand'
    },
    {
      '1': 'pose',
      '3': 4,
      '4': 3,
      '5': 11,
      '6': '.mj.sign.Point3D',
      '10': 'pose'
    },
    {
      '1': 'face_contour',
      '3': 5,
      '4': 3,
      '5': 11,
      '6': '.mj.sign.Point3D',
      '10': 'faceContour'
    },
  ],
};

/// Descriptor for `LandmarkFrame`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List landmarkFrameDescriptor = $convert.base64Decode(
    'Cg1MYW5kbWFya0ZyYW1lEiEKDHRpbWVzdGFtcF9tcxgBIAEoA1ILdGltZXN0YW1wTXMSLQoJbG'
    'VmdF9oYW5kGAIgAygLMhAubWouc2lnbi5Qb2ludDNEUghsZWZ0SGFuZBIvCgpyaWdodF9oYW5k'
    'GAMgAygLMhAubWouc2lnbi5Qb2ludDNEUglyaWdodEhhbmQSJAoEcG9zZRgEIAMoCzIQLm1qLn'
    'NpZ24uUG9pbnQzRFIEcG9zZRIzCgxmYWNlX2NvbnRvdXIYBSADKAsyEC5tai5zaWduLlBvaW50'
    'M0RSC2ZhY2VDb250b3Vy');

@$core.Deprecated('Use clientStreamChunkDescriptor instead')
const ClientStreamChunk$json = {
  '1': 'ClientStreamChunk',
  '2': [
    {'1': 'session_id', '3': 1, '4': 1, '5': 9, '10': 'sessionId'},
    {
      '1': 'frames',
      '3': 2,
      '4': 3,
      '5': 11,
      '6': '.mj.sign.LandmarkFrame',
      '10': 'frames'
    },
  ],
};

/// Descriptor for `ClientStreamChunk`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List clientStreamChunkDescriptor = $convert.base64Decode(
    'ChFDbGllbnRTdHJlYW1DaHVuaxIdCgpzZXNzaW9uX2lkGAEgASgJUglzZXNzaW9uSWQSLgoGZn'
    'JhbWVzGAIgAygLMhYubWouc2lnbi5MYW5kbWFya0ZyYW1lUgZmcmFtZXM=');

@$core.Deprecated('Use translationResultDescriptor instead')
const TranslationResult$json = {
  '1': 'TranslationResult',
  '2': [
    {'1': 'session_id', '3': 1, '4': 1, '5': 9, '10': 'sessionId'},
    {'1': 'text', '3': 2, '4': 1, '5': 9, '10': 'text'},
    {'1': 'is_final', '3': 3, '4': 1, '5': 8, '10': 'isFinal'},
    {'1': 'confidence', '3': 4, '4': 1, '5': 2, '10': 'confidence'},
  ],
};

/// Descriptor for `TranslationResult`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List translationResultDescriptor = $convert.base64Decode(
    'ChFUcmFuc2xhdGlvblJlc3VsdBIdCgpzZXNzaW9uX2lkGAEgASgJUglzZXNzaW9uSWQSEgoEdG'
    'V4dBgCIAEoCVIEdGV4dBIZCghpc19maW5hbBgDIAEoCFIHaXNGaW5hbBIeCgpjb25maWRlbmNl'
    'GAQgASgCUgpjb25maWRlbmNl');
