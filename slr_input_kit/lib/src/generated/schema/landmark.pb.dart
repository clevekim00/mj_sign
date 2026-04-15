// This is a generated file - do not edit.
//
// Generated from schema/landmark.proto.

// @dart = 3.3

// ignore_for_file: annotate_overrides, camel_case_types, comment_references
// ignore_for_file: constant_identifier_names
// ignore_for_file: curly_braces_in_flow_control_structures
// ignore_for_file: deprecated_member_use_from_same_package, library_prefixes
// ignore_for_file: non_constant_identifier_names, prefer_relative_imports

import 'dart:core' as $core;

import 'package:fixnum/fixnum.dart' as $fixnum;
import 'package:protobuf/protobuf.dart' as $pb;

export 'package:protobuf/protobuf.dart' show GeneratedMessageGenericExtensions;

/// Represents a single 3D point
class Point3D extends $pb.GeneratedMessage {
  factory Point3D({
    $core.double? x,
    $core.double? y,
    $core.double? z,
  }) {
    final result = create();
    if (x != null) result.x = x;
    if (y != null) result.y = y;
    if (z != null) result.z = z;
    return result;
  }

  Point3D._();

  factory Point3D.fromBuffer($core.List<$core.int> data,
          [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) =>
      create()..mergeFromBuffer(data, registry);
  factory Point3D.fromJson($core.String json,
          [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) =>
      create()..mergeFromJson(json, registry);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(
      _omitMessageNames ? '' : 'Point3D',
      package: const $pb.PackageName(_omitMessageNames ? '' : 'mj.sign'),
      createEmptyInstance: create)
    ..aD(1, _omitFieldNames ? '' : 'x', fieldType: $pb.PbFieldType.OF)
    ..aD(2, _omitFieldNames ? '' : 'y', fieldType: $pb.PbFieldType.OF)
    ..aD(3, _omitFieldNames ? '' : 'z', fieldType: $pb.PbFieldType.OF)
    ..hasRequiredFields = false;

  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  Point3D clone() => deepCopy();
  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  Point3D copyWith(void Function(Point3D) updates) =>
      super.copyWith((message) => updates(message as Point3D)) as Point3D;

  @$core.override
  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static Point3D create() => Point3D._();
  @$core.override
  Point3D createEmptyInstance() => create();
  @$core.pragma('dart2js:noInline')
  static Point3D getDefault() =>
      _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<Point3D>(create);
  static Point3D? _defaultInstance;

  @$pb.TagNumber(1)
  $core.double get x => $_getN(0);
  @$pb.TagNumber(1)
  set x($core.double value) => $_setFloat(0, value);
  @$pb.TagNumber(1)
  $core.bool hasX() => $_has(0);
  @$pb.TagNumber(1)
  void clearX() => $_clearField(1);

  @$pb.TagNumber(2)
  $core.double get y => $_getN(1);
  @$pb.TagNumber(2)
  set y($core.double value) => $_setFloat(1, value);
  @$pb.TagNumber(2)
  $core.bool hasY() => $_has(1);
  @$pb.TagNumber(2)
  void clearY() => $_clearField(2);

  @$pb.TagNumber(3)
  $core.double get z => $_getN(2);
  @$pb.TagNumber(3)
  set z($core.double value) => $_setFloat(2, value);
  @$pb.TagNumber(3)
  $core.bool hasZ() => $_has(2);
  @$pb.TagNumber(3)
  void clearZ() => $_clearField(3);
}

/// Represents a frame of landmarks extracted from MediaPipe
class LandmarkFrame extends $pb.GeneratedMessage {
  factory LandmarkFrame({
    $fixnum.Int64? timestampMs,
    $core.Iterable<Point3D>? leftHand,
    $core.Iterable<Point3D>? rightHand,
    $core.Iterable<Point3D>? pose,
    $core.Iterable<Point3D>? faceContour,
  }) {
    final result = create();
    if (timestampMs != null) result.timestampMs = timestampMs;
    if (leftHand != null) result.leftHand.addAll(leftHand);
    if (rightHand != null) result.rightHand.addAll(rightHand);
    if (pose != null) result.pose.addAll(pose);
    if (faceContour != null) result.faceContour.addAll(faceContour);
    return result;
  }

  LandmarkFrame._();

  factory LandmarkFrame.fromBuffer($core.List<$core.int> data,
          [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) =>
      create()..mergeFromBuffer(data, registry);
  factory LandmarkFrame.fromJson($core.String json,
          [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) =>
      create()..mergeFromJson(json, registry);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(
      _omitMessageNames ? '' : 'LandmarkFrame',
      package: const $pb.PackageName(_omitMessageNames ? '' : 'mj.sign'),
      createEmptyInstance: create)
    ..aInt64(1, _omitFieldNames ? '' : 'timestampMs')
    ..pPM<Point3D>(2, _omitFieldNames ? '' : 'leftHand',
        subBuilder: Point3D.create)
    ..pPM<Point3D>(3, _omitFieldNames ? '' : 'rightHand',
        subBuilder: Point3D.create)
    ..pPM<Point3D>(4, _omitFieldNames ? '' : 'pose', subBuilder: Point3D.create)
    ..pPM<Point3D>(5, _omitFieldNames ? '' : 'faceContour',
        subBuilder: Point3D.create)
    ..hasRequiredFields = false;

  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  LandmarkFrame clone() => deepCopy();
  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  LandmarkFrame copyWith(void Function(LandmarkFrame) updates) =>
      super.copyWith((message) => updates(message as LandmarkFrame))
          as LandmarkFrame;

  @$core.override
  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static LandmarkFrame create() => LandmarkFrame._();
  @$core.override
  LandmarkFrame createEmptyInstance() => create();
  @$core.pragma('dart2js:noInline')
  static LandmarkFrame getDefault() => _defaultInstance ??=
      $pb.GeneratedMessage.$_defaultFor<LandmarkFrame>(create);
  static LandmarkFrame? _defaultInstance;

  /// Timestamp in milliseconds
  @$pb.TagNumber(1)
  $fixnum.Int64 get timestampMs => $_getI64(0);
  @$pb.TagNumber(1)
  set timestampMs($fixnum.Int64 value) => $_setInt64(0, value);
  @$pb.TagNumber(1)
  $core.bool hasTimestampMs() => $_has(0);
  @$pb.TagNumber(1)
  void clearTimestampMs() => $_clearField(1);

  /// Filtered hand landmarks (typically 21 points per hand)
  @$pb.TagNumber(2)
  $pb.PbList<Point3D> get leftHand => $_getList(1);

  @$pb.TagNumber(3)
  $pb.PbList<Point3D> get rightHand => $_getList(2);

  /// Filtered pose landmarks (essential upper body joints)
  @$pb.TagNumber(4)
  $pb.PbList<Point3D> get pose => $_getList(3);

  /// Filtered face landmarks (essential lip/jaw contour points)
  @$pb.TagNumber(5)
  $pb.PbList<Point3D> get faceContour => $_getList(4);
}

class ClientStreamChunk extends $pb.GeneratedMessage {
  factory ClientStreamChunk({
    $core.String? sessionId,
    $core.Iterable<LandmarkFrame>? frames,
  }) {
    final result = create();
    if (sessionId != null) result.sessionId = sessionId;
    if (frames != null) result.frames.addAll(frames);
    return result;
  }

  ClientStreamChunk._();

  factory ClientStreamChunk.fromBuffer($core.List<$core.int> data,
          [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) =>
      create()..mergeFromBuffer(data, registry);
  factory ClientStreamChunk.fromJson($core.String json,
          [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) =>
      create()..mergeFromJson(json, registry);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(
      _omitMessageNames ? '' : 'ClientStreamChunk',
      package: const $pb.PackageName(_omitMessageNames ? '' : 'mj.sign'),
      createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'sessionId')
    ..pPM<LandmarkFrame>(2, _omitFieldNames ? '' : 'frames',
        subBuilder: LandmarkFrame.create)
    ..hasRequiredFields = false;

  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  ClientStreamChunk clone() => deepCopy();
  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  ClientStreamChunk copyWith(void Function(ClientStreamChunk) updates) =>
      super.copyWith((message) => updates(message as ClientStreamChunk))
          as ClientStreamChunk;

  @$core.override
  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static ClientStreamChunk create() => ClientStreamChunk._();
  @$core.override
  ClientStreamChunk createEmptyInstance() => create();
  @$core.pragma('dart2js:noInline')
  static ClientStreamChunk getDefault() => _defaultInstance ??=
      $pb.GeneratedMessage.$_defaultFor<ClientStreamChunk>(create);
  static ClientStreamChunk? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get sessionId => $_getSZ(0);
  @$pb.TagNumber(1)
  set sessionId($core.String value) => $_setString(0, value);
  @$pb.TagNumber(1)
  $core.bool hasSessionId() => $_has(0);
  @$pb.TagNumber(1)
  void clearSessionId() => $_clearField(1);

  @$pb.TagNumber(2)
  $pb.PbList<LandmarkFrame> get frames => $_getList(1);
}

class TranslationResult extends $pb.GeneratedMessage {
  factory TranslationResult({
    $core.String? sessionId,
    $core.String? text,
    $core.bool? isFinal,
    $core.double? confidence,
  }) {
    final result = create();
    if (sessionId != null) result.sessionId = sessionId;
    if (text != null) result.text = text;
    if (isFinal != null) result.isFinal = isFinal;
    if (confidence != null) result.confidence = confidence;
    return result;
  }

  TranslationResult._();

  factory TranslationResult.fromBuffer($core.List<$core.int> data,
          [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) =>
      create()..mergeFromBuffer(data, registry);
  factory TranslationResult.fromJson($core.String json,
          [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) =>
      create()..mergeFromJson(json, registry);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(
      _omitMessageNames ? '' : 'TranslationResult',
      package: const $pb.PackageName(_omitMessageNames ? '' : 'mj.sign'),
      createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'sessionId')
    ..aOS(2, _omitFieldNames ? '' : 'text')
    ..aOB(3, _omitFieldNames ? '' : 'isFinal')
    ..aD(4, _omitFieldNames ? '' : 'confidence', fieldType: $pb.PbFieldType.OF)
    ..hasRequiredFields = false;

  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  TranslationResult clone() => deepCopy();
  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  TranslationResult copyWith(void Function(TranslationResult) updates) =>
      super.copyWith((message) => updates(message as TranslationResult))
          as TranslationResult;

  @$core.override
  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static TranslationResult create() => TranslationResult._();
  @$core.override
  TranslationResult createEmptyInstance() => create();
  @$core.pragma('dart2js:noInline')
  static TranslationResult getDefault() => _defaultInstance ??=
      $pb.GeneratedMessage.$_defaultFor<TranslationResult>(create);
  static TranslationResult? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get sessionId => $_getSZ(0);
  @$pb.TagNumber(1)
  set sessionId($core.String value) => $_setString(0, value);
  @$pb.TagNumber(1)
  $core.bool hasSessionId() => $_has(0);
  @$pb.TagNumber(1)
  void clearSessionId() => $_clearField(1);

  @$pb.TagNumber(2)
  $core.String get text => $_getSZ(1);
  @$pb.TagNumber(2)
  set text($core.String value) => $_setString(1, value);
  @$pb.TagNumber(2)
  $core.bool hasText() => $_has(1);
  @$pb.TagNumber(2)
  void clearText() => $_clearField(2);

  @$pb.TagNumber(3)
  $core.bool get isFinal => $_getBF(2);
  @$pb.TagNumber(3)
  set isFinal($core.bool value) => $_setBool(2, value);
  @$pb.TagNumber(3)
  $core.bool hasIsFinal() => $_has(2);
  @$pb.TagNumber(3)
  void clearIsFinal() => $_clearField(3);

  @$pb.TagNumber(4)
  $core.double get confidence => $_getN(3);
  @$pb.TagNumber(4)
  set confidence($core.double value) => $_setFloat(3, value);
  @$pb.TagNumber(4)
  $core.bool hasConfidence() => $_has(3);
  @$pb.TagNumber(4)
  void clearConfidence() => $_clearField(4);
}

const $core.bool _omitFieldNames =
    $core.bool.fromEnvironment('protobuf.omit_field_names');
const $core.bool _omitMessageNames =
    $core.bool.fromEnvironment('protobuf.omit_message_names');
