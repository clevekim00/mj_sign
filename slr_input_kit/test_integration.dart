// ignore_for_file: avoid_print

import 'package:slr_input_kit/src/sign_gemma_client.dart';
import 'package:slr_input_kit/src/generated/schema/landmark.pb.dart';
import 'dart:io';
import 'package:fixnum/fixnum.dart';

void main() async {
  print('Starting End-to-End Simulation Test...');
  final client = SignGemmaClient(url: 'ws://127.0.0.1:8080/ws/sign');

  client.onTranslation = (result) {
    print('\n==============================');
    print('✅ [TRANSLATION RESULT]');
    print('TEXT: ${result.text}');
    print('IS_FINAL: ${result.isFinal}');
    print('==============================\n');
    
    // Disconnect and exit after receiving the translation
    client.disconnect();
    exit(0);
  };

  client.connect();

  // Wait for connection to establish
  await Future.delayed(Duration(seconds: 2));

  print('Generating Mock Protobuf Frame...');
  // Generate a mock frame
  final frame = LandmarkFrame()
    ..timestampMs = Int64(DateTime.now().millisecondsSinceEpoch)
    ..leftHand.add(Point3D()..x = 0.1..y = 0.2..z = 0.3)
    ..rightHand.add(Point3D()..x = 0.9..y = 0.8..z = 0.7);

  print('Sending Protobuf Frame...');
  client.sendFrames([frame], 'test_session_123');

  // Safety timeout
  await Future.delayed(Duration(seconds: 10));
  print('❌ Test timed out. Did not receive response from server.');
  client.disconnect();
  exit(1);
}
