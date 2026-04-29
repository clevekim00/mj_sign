import 'package:flutter_test/flutter_test.dart';

import 'package:slr_input_kit_example/main.dart';

void main() {
  testWidgets('renders LinguaSign sample gallery', (WidgetTester tester) async {
    await tester.pumpWidget(const SlrInputKitSampleApp());

    expect(find.text('LinguaSign samples'), findsOneWidget);
    expect(find.text('SignGemma T2S / STS'), findsOneWidget);
  });
}
