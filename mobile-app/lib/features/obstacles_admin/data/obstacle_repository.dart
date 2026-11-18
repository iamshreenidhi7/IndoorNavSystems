import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/app_failure.dart';
import '../../../core/errors/result.dart';
import '../../../core/network/api_client.dart';
import '../domain/admin_obstacle.dart';

final obstacleRepositoryProvider = Provider<ObstacleRepository>((ref) {
  return ObstacleRepository(ref.watch(apiClientProvider));
});

class ObstacleRepository {
  ObstacleRepository(this._apiClient);

  final ApiClient _apiClient;

  Future<Result<List<AdminObstacle>>> list({
    required int buildingId,
    String? status,
  }) async {
    try {
      final data = await _apiClient.getObjectList(
        '/api/obstacles',
        queryParameters: <String, Object?>{
          'buildingId': buildingId,
          if (status != null) 'status': status,
        },
      );
      return Success(data.map(AdminObstacle.fromJson).toList(growable: false));
    } on AppFailure catch (failure) {
      return Failure(failure);
    } on FormatException {
      return const Failure(InvalidResponseFailure());
    }
  }

  Future<Result<AdminObstacle>> create({
    required String targetType,
    required int targetId,
    required String obstacleType,
    required String description,
  }) => _write(
    () => _apiClient.postObject(
      '/api/obstacles',
      data: <String, Object?>{
        'targetType': targetType,
        'targetId': targetId,
        'obstacleType': obstacleType,
        'description': description,
      },
    ),
  );

  Future<Result<AdminObstacle>> update(AdminObstacle obstacle) => _write(
    () => _apiClient.putObject(
      '/api/obstacles/${obstacle.id}',
      data: <String, Object?>{
        'obstacleType': obstacle.obstacleType,
        'description': obstacle.description,
        'status': obstacle.status,
        'version': obstacle.version,
      },
    ),
  );

  Future<Result<void>> delete(int obstacleId) async {
    try {
      await _apiClient.delete('/api/obstacles/$obstacleId');
      return const Success(null);
    } on AppFailure catch (failure) {
      return Failure(failure);
    }
  }

  Future<Result<AdminObstacle>> _write(
    Future<Map<String, Object?>> Function() request,
  ) async {
    try {
      return Success(AdminObstacle.fromJson(await request()));
    } on AppFailure catch (failure) {
      return Failure(failure);
    } on FormatException {
      return const Failure(InvalidResponseFailure());
    }
  }
}
