import 'dart:async';
import 'dart:convert';
import 'dart:ui' as ui;

import 'package:web_socket_channel/web_socket_channel.dart';

import 'generated/schema/landmark.pb.dart';

typedef TranslationHandler = void Function(TranslationResult result);
typedef BridgeEventHandler = void Function(SignGemmaBridgeEvent event);
typedef ConnectionStateHandler =
    void Function(SignGemmaConnectionState state, String? detail);

enum SignGemmaConnectionState { disconnected, connecting, connected, error }

class SignLanguageContext {
  const SignLanguageContext({
    required this.locale,
    required this.signLanguage,
    this.modelProfile,
    this.protocolVersion = 'signbridge-model-v1',
  });

  final String locale;
  final String signLanguage;
  final String? modelProfile;
  final String protocolVersion;

  factory SignLanguageContext.fromPlatformDispatcher() {
    return SignLanguageContext.fromLocale(
      ui.PlatformDispatcher.instance.locale,
    );
  }

  factory SignLanguageContext.fromLocale(ui.Locale locale) {
    final languageCode = locale.languageCode.toLowerCase();
    final countryCode = locale.countryCode;
    final localeTag = countryCode == null || countryCode.isEmpty
        ? languageCode
        : '$languageCode-$countryCode';
    final signLanguage =
        _signLanguageByLocaleLanguage[languageCode] ?? 'international-sign';
    return SignLanguageContext(
      locale: localeTag,
      signLanguage: signLanguage,
      modelProfile:
          _modelProfileBySignLanguage[signLanguage] ??
          'sign-gemma-$signLanguage',
    );
  }

  Uri applyTo(Uri uri) {
    final nextQuery = <String, String>{
      ...uri.queryParameters,
      'locale': locale,
      'sign_language': signLanguage,
      'protocol_version': protocolVersion,
    };
    final profile = modelProfile;
    if (profile != null && profile.isNotEmpty) {
      nextQuery['model_profile'] = profile;
    }
    return uri.replace(queryParameters: nextQuery);
  }

  static const Map<String, String> _signLanguageByLocaleLanguage = {
    'ko': 'ksl',
    'en': 'asl',
    'ja': 'jsl',
    'zh': 'csl',
    'fr': 'lsf',
    'de': 'dgs',
    'es': 'lse',
  };

  static const Map<String, String> _modelProfileBySignLanguage = {
    'ksl': 'sign-gemma-ko',
    'asl': 'sign-gemma',
    'jsl': 'sign-gemma-ja',
    'csl': 'sign-gemma-zh',
    'lsf': 'sign-gemma-fr',
    'dgs': 'sign-gemma-de',
    'lse': 'sign-gemma-es',
  };
}

class SignGemmaBridgeEvent {
  const SignGemmaBridgeEvent({
    required this.eventType,
    required this.sessionId,
    this.status,
    this.statusText,
    this.resultText,
    this.errorCode,
    required this.isFinal,
    required this.confidence,
  });

  final String eventType;
  final String sessionId;
  final String? status;
  final String? statusText;
  final String? resultText;
  final String? errorCode;
  final bool isFinal;
  final double confidence;

  bool get isResult => eventType == 'result';
  bool get isStatus => eventType == 'status';
  bool get isError => eventType == 'error';

  TranslationResult toTranslationResult() {
    return TranslationResult(
      sessionId: sessionId,
      text: resultText ?? '',
      isFinal: isFinal,
      confidence: confidence,
    );
  }

  factory SignGemmaBridgeEvent.fromJson(Map<String, dynamic> json) {
    final eventType =
        json['event_type'] as String? ??
        (json.containsKey('error_code')
            ? 'error'
            : json.containsKey('status_text')
            ? 'status'
            : 'result');
    final resultText =
        json['result_text'] as String? ?? json['text'] as String?;

    return SignGemmaBridgeEvent(
      eventType: eventType,
      sessionId: json['session_id'] as String? ?? '',
      status: json['status'] as String?,
      statusText: json['status_text'] as String?,
      resultText: resultText,
      errorCode: json['error_code'] as String?,
      isFinal: json['is_final'] as bool? ?? eventType != 'status',
      confidence: (json['confidence'] as num?)?.toDouble() ?? 0,
    );
  }
}

class SignGemmaClient {
  SignGemmaClient({
    this.url = 'ws://127.0.0.1:8080/ws/sign',
    SignLanguageContext? languageContext,
  }) : languageContext =
           languageContext ?? SignLanguageContext.fromPlatformDispatcher();

  final String url;
  final SignLanguageContext languageContext;
  WebSocketChannel? _channel;
  StreamSubscription<dynamic>? _subscription;

  TranslationHandler? onTranslation;
  BridgeEventHandler? onEvent;
  ConnectionStateHandler? onConnectionState;

  bool get isConnected => _channel != null;

  Future<void> connect() async {
    if (_channel != null) {
      return;
    }

    onConnectionState?.call(SignGemmaConnectionState.connecting, null);

    final channel = WebSocketChannel.connect(
      languageContext.applyTo(Uri.parse(url)),
    );
    _subscription = channel.stream.listen(
      _handleMessage,
      onDone: () {
        _subscription = null;
        _channel = null;
        onConnectionState?.call(SignGemmaConnectionState.disconnected, null);
      },
      onError: (Object error) {
        _subscription = null;
        _channel = null;
        onConnectionState?.call(
          SignGemmaConnectionState.error,
          error.toString(),
        );
      },
      cancelOnError: true,
    );

    try {
      await channel.ready;
      _channel = channel;
      onConnectionState?.call(SignGemmaConnectionState.connected, null);
    } catch (error) {
      await _subscription?.cancel();
      _subscription = null;
      _channel = null;
      onConnectionState?.call(SignGemmaConnectionState.error, error.toString());
      rethrow;
    }
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
    onConnectionState?.call(SignGemmaConnectionState.disconnected, null);
  }

  void _handleMessage(dynamic message) {
    if (message is String) {
      try {
        final decoded = jsonDecode(message);
        if (decoded is Map<String, dynamic>) {
          final event = SignGemmaBridgeEvent.fromJson(decoded);
          _emitEvent(event);
          return;
        }
      } on FormatException {
        _emitEvent(
          SignGemmaBridgeEvent(
            eventType: 'result',
            sessionId: '',
            resultText: message,
            isFinal: true,
            confidence: 0,
          ),
        );
        return;
      }
    }

    _emitEvent(
      SignGemmaBridgeEvent(
        eventType: 'result',
        sessionId: '',
        resultText: message.toString(),
        isFinal: true,
        confidence: 0,
      ),
    );
  }

  void _emitEvent(SignGemmaBridgeEvent event) {
    onEvent?.call(event);
    if (event.isResult) {
      onTranslation?.call(event.toTranslationResult());
    }
  }
}
