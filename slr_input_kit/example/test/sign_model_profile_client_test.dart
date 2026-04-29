import 'package:flutter_test/flutter_test.dart';
import 'package:slr_input_kit/slr_input_kit.dart';

void main() {
  test('parses model profile discovery response', () {
    final catalog = SignModelProfileCatalog.fromJsonString('''
{
  "default_profile": {
    "locale": "ko-KR",
    "sign_language": "ksl",
    "model_profile": "sign-gemma-ko",
    "protocol_version": "signbridge-model-v1",
    "is_default": true
  },
  "profiles": [
    {
      "locale": "ko-KR",
      "sign_language": "ksl",
      "model_profile": "sign-gemma-ko",
      "protocol_version": "signbridge-model-v1",
      "is_default": true
    },
    {
      "locale": "en-US",
      "sign_language": "asl",
      "model_profile": "sign-gemma",
      "protocol_version": "signbridge-model-v1",
      "is_default": false
    }
  ]
}
''');

    expect(catalog.defaultProfile.modelProfile, 'sign-gemma-ko');
    expect(catalog.profiles, hasLength(2));
    expect(catalog.profiles.last.toLanguageContext().signLanguage, 'asl');
  });
}
