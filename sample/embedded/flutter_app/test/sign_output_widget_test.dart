import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fixnum/fixnum.dart';
import 'package:slr_input_kit/slr_input_kit.dart';

void main() {
  testWidgets('SignOutputWidget exposes playback controls', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SizedBox(
            width: 420,
            height: 320,
            child: SignOutputWidget(
              frames: [
                LandmarkFrame()..timestampMs = Int64(0),
                LandmarkFrame()..timestampMs = Int64(84),
              ],
            ),
          ),
        ),
      ),
    );

    expect(find.byTooltip('Pause playback'), findsOneWidget);
    expect(find.byTooltip('Replay motion'), findsOneWidget);
    expect(find.text('1.0x'), findsOneWidget);

    await tester.tap(find.text('1.0x'));
    await tester.pump();

    expect(find.text('1.5x'), findsOneWidget);
  });
}
