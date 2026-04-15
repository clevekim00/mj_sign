import 'dart:async';
import 'dart:convert';

import 'package:web_socket_channel/web_socket_channel.dart';

import 'generated/schema/landmark.pb.dart';

typedef TranslationHandler = void Function(TranslationResult result);

class SignGemmaClient {
  SignGemmaClient({this.url = 'ws://127.0.0.1:8080/ws/sign'});

  final String url;
  WebSocketChannel? _channel;
  StreamSubscription<dynamic>? _subscription;

  TranslationHandler? onTranslation;

  bool get isConnected => _channel != null;

  void connect() {
    if (_channel != null) {
      return;
    }

    final channel = WebSocketChannel.connect(Uri.parse(url));
    _channel = channel;
    _subscription = channel.stream.listen(
      _handleMessage,
      onDone: () {
        _subscription = null;
        _channel = null;
      },
      onError: (_) {
        _subscription = null;
        _channel = null;
      },
      cancelOnError: true,
    );
  }

  void sendFrames(List<LandmarkFrame> frames, String sessionId) {
    final channel = _channel;
    if (channel == null) {
      throw StateError('SignGemmaClient is not connected.');
    }

    final chunk = ClientStreamChunk()
      ..sessionId = sessionId
      ..frames.addAll(frames);

    channel.sink.add(chunk.writeToBuffer());
  }

  Future<void> disconnect() async {
    await _subscription?.cancel();
    await _channel?.sink.close();
    _subscription = null;
    _channel = null;
  }

  void _handleMessage(dynamic message) {
    final handler = onTranslation;
    if (handler == null) {
      return;
    }

    if (message is String) {
      try {
        final decoded = jsonDecode(message);
        if (decoded is Map<String, dynamic>) {
          handler(
            TranslationResult(
              sessionId: decoded['session_id'] as String? ?? '',
              text: decoded['text'] as String? ?? '',
              isFinal: decoded['is_final'] as bool? ?? true,
              confidence: (decoded['confidence'] as num?)?.toDouble() ?? 0,
            ),
          );
          return;
        }
      } on FormatException {
        handler(
          TranslationResult(
            text: message,
            isFinal: true,
          ),
        );
        return;
      }
    }

    handler(
      TranslationResult(
        text: message.toString(),
        isFinal: true,
      ),
    );
  }
}
