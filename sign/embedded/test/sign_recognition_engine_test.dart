import 'dart:async';

import 'package:fixnum/fixnum.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:slr_input_kit/slr_input_kit.dart';

void main() {
  test('stopping during inference suppresses late results', () async {
    final completion = Completer<TranslationResult>();
    final engine = EmbeddedSignRecognitionEngine(
      delegate: (_) => completion.future,
    );
    final results = <TranslationResult>[];
    final subscription = engine.events.listen((event) {
      if (event.result != null) {
        results.add(event.result!);
      }
    });
    await engine.start();
    final pending = engine.recognize(sessionId: 's', frames: [LandmarkFrame()]);
    await engine.stop();
    completion.complete(TranslationResult(text: 'late'));
    await pending;
    expect(results, isEmpty);
    expect(engine.isReady, isFalse);
    await subscription.cancel();
  });

  test('pending requests are bounded and processed serially', () async {
    final completion = Completer<TranslationResult>();
    var calls = 0;
    final engine = EmbeddedSignRecognitionEngine(
      maxPendingRequests: 1,
      delegate: (_) async {
        calls++;
        return completion.future;
      },
    );
    await engine.start();
    final first = engine.recognize(sessionId: 's', frames: [LandmarkFrame()]);
    await engine.recognize(sessionId: 's', frames: [LandmarkFrame()]);
    await expectLater(
      engine.recognize(sessionId: 's', frames: [LandmarkFrame()]),
      throwsStateError,
    );
    expect(calls, 1);
    completion.complete(TranslationResult(text: 'done'));
    await first;
    expect(calls, 2);
    await engine.stop();
  });

  test('embedded engine emits a result without network access', () async {
    final engine = EmbeddedSignRecognitionEngine(
      delegate: (request) async => TranslationResult(
        sessionId: request.sessionId,
        text: 'local-result',
        isFinal: true,
        confidence: 0.8,
      ),
    );
    final resultReceived = engine.events.firstWhere(
      (event) => event.result != null,
    );

    await engine.start();
    await engine.recognize(
      sessionId: 'embedded-1',
      frames: [LandmarkFrame()..timestampMs = Int64(1)],
    );

    expect(engine.mode, SignRecognitionMode.embedded);
    final result = await resultReceived.timeout(const Duration(seconds: 2));
    expect(result.result!.text, 'local-result');
    await engine.stop();
  });

  test('embedded engine rejects requests before start', () async {
    final engine = EmbeddedSignRecognitionEngine(
      delegate: (_) async => TranslationResult(),
    );

    await expectLater(
      engine.recognize(sessionId: 'not-ready', frames: [LandmarkFrame()]),
      throwsStateError,
    );
    await engine.stop();
  });

  test(
    'embedded engine reports delegate failures and remains usable',
    () async {
      var calls = 0;
      final engine = EmbeddedSignRecognitionEngine(
        delegate: (request) async {
          calls++;
          if (calls == 1) throw StateError('model failure');
          return TranslationResult(
            sessionId: request.sessionId,
            text: 'recovered',
          );
        },
      );
      final failureReceived = engine.events.firstWhere(
        (event) => event.state == SignRecognitionEngineState.error,
      );

      await engine.start();
      await engine.recognize(sessionId: 's', frames: [LandmarkFrame()]);
      await engine.recognize(sessionId: 's', frames: [LandmarkFrame()]);

      final failure = await failureReceived.timeout(const Duration(seconds: 2));
      expect(failure.message, contains('model failure'));
      expect(calls, 2);
      await engine.stop();
    },
  );
}
