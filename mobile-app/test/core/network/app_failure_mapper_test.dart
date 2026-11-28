import 'package:flutter_test/flutter_test.dart';
import 'package:indoor_nav_mobile/core/errors/app_failure.dart';
import 'package:indoor_nav_mobile/core/network/api_client.dart';

void main() {
  group('AppFailureMapper', () {
    test('maps documented HTTP statuses to user-facing failures', () {
      expect(AppFailureMapper.fromStatus(400), isA<ValidationFailure>());
      expect(AppFailureMapper.fromStatus(401), isA<UnauthorizedFailure>());
      expect(AppFailureMapper.fromStatus(404), isA<NotFoundFailure>());
      expect(AppFailureMapper.fromStatus(409), isA<ConflictFailure>());
      expect(AppFailureMapper.fromStatus(429), isA<RateLimitedFailure>());
    });

    test('uses the documented error code when present', () {
      expect(
        AppFailureMapper.fromResponse(
          statusCode: 500,
          body: <String, Object?>{'code': 'CONFLICT'},
        ),
        isA<ConflictFailure>(),
      );
    });
  });
}
