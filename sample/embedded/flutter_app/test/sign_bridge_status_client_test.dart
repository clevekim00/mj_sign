import 'package:flutter_test/flutter_test.dart';
import 'package:slr_input_kit/slr_input_kit.dart';

void main() {
  test('builds SignBridge diagnostics endpoint URLs', () {
    final snapshot = SignBridgeStatusSnapshot(
      baseUrl: 'http://127.0.0.1:8080/',
      checkedAt: DateTime.utc(2026, 4, 29),
      healthStatus: 'UP',
      readinessStatus: 'READY',
      provider: 'http',
      modelBaseUrl: 'http://localhost:8000',
    );

    expect(snapshot.healthUp, isTrue);
    expect(snapshot.ready, isTrue);
    expect(snapshot.swaggerUiUrl, 'http://127.0.0.1:8080/swagger-ui.html');
    expect(snapshot.openApiUrl, 'http://127.0.0.1:8080/v3/api-docs');
    expect(snapshot.profilesUrl, 'http://127.0.0.1:8080/api/v2/model-profiles');
    expect(snapshot.readinessUrl, 'http://127.0.0.1:8080/internal/readyz');
    expect(
      snapshot.prometheusMetricsUrl,
      'http://127.0.0.1:8080/internal/metrics.prometheus',
    );
  });
}
