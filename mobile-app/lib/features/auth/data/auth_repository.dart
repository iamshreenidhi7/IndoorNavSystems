import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_failure.dart';
import '../../../core/errors/result.dart';
import '../../../core/network/api_client.dart';

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(ref.watch(apiClientProvider));
});

class AuthRepository {
  AuthRepository(this._apiClient);

  final ApiClient _apiClient;

  Future<Result<String>> login({
    required String username,
    required String password,
  }) async {
    try {
      final data = await _apiClient.postObject(
        '/api/auth/login',
        data: <String, Object?>{'username': username, 'password': password},
      );
      final token = data['accessToken'];
      if (token is! String || token.isEmpty) {
        return const Failure(InvalidResponseFailure());
      }
      return Success(token);
    } on AppFailure catch (failure) {
      return Failure(failure);
    }
  }
}
