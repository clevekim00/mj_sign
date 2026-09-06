import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:slr_input_kit_example/samples/dual_mode_recognition_sample.dart';

void main() {
  testWidgets('embedded sample recognizes demo frames and exposes EOS', (
    tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(home: DualModeRecognitionSample()),
    );
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));
    await tester.pump();
    expect(find.text('기기 내장'), findsOneWidget);
    expect(find.text('서버 연결'), findsOneWidget);
    expect(find.text('문장 확정'), findsOneWidget);
    expect(find.textContaining('인식 결과: 안녕하세요'), findsOneWidget);
    await tester.tap(find.text('문장 확정'));
    await tester.pump();
    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pump();
    expect(tester.takeException(), isNull);
  });
}
