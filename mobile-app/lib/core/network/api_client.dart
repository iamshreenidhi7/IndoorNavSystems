import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../config/app_config.dart';
import '../errors/app_failure.dart';

final adminTokenProvider = NotifierProvider<AdminTokenNotifier, String?>(
  AdminTokenNotifier.new,
);

class AdminTokenNotifier extends Notifier<String?> {
  @override
  String? build() => null;

  void setToken(String token) => state = token;

  void clear() => state = null;
}

final appConfigProvider = Provider<AppConfig>((ref) {
  return AppConfig.fromEnvironment();
});

final dioProvider = Provider<Dio>((ref) {
  final config = ref.watch(appConfigProvider);
  final client = Dio(
    BaseOptions(
      baseUrl: config.apiBaseUrl,
      connectTimeout: const Duration(seconds: 5),
      receiveTimeout: const Duration(seconds: 10),
      responseType: ResponseType.json,
    ),
  );
  client.interceptors.add(
    InterceptorsWrapper(
      onRequest: (options, handler) {
        final token = ref.read(adminTokenProvider);
        if (token != null) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        handler.next(options);
      },
      onError: (error, handler) {
        if (error.response?.statusCode == 401) {
          ref.read(adminTokenProvider.notifier).clear();
        }
        handler.next(error);
      },
    ),
  );
  return client;
});

final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(ref.watch(dioProvider));
});

class ApiClient {
  ApiClient(this._dio);

  final Dio _dio;

  Future<List<Map<String, Object?>>> getObjectList(
    String path, {
    Map<String, Object?>? queryParameters,
  }) async {
    try {
      final response = await _dio.get<Object?>(
        path,
        queryParameters: queryParameters,
      );
      final data = response.data;
      if (data is! List<Object?>) {
        throw const InvalidResponseFailure();
      }

      return data.map(_asObjectMap).toList(growable: false);
    } on DioException catch (error) {
      throw _failureFor(error);
    } on FormatException {
      throw const InvalidResponseFailure();
    }
  }

  Future<Map<String, Object?>> getObject(String path) async {
    try {
      final response = await _dio.get<Object?>(path);
      return _asObjectMap(response.data);
    } on DioException catch (error) {
      throw _failureFor(error);
    } on FormatException {
      throw const InvalidResponseFailure();
    }
  }

  Future<List<String>> getStringList(
    String path, {
    Map<String, Object?>? queryParameters,
  }) async {
    try {
      final response = await _dio.get<Object?>(
        path,
        queryParameters: queryParameters,
      );
      final data = response.data;
      if (data is! List<Object?> || data.any((item) => item is! String)) {
        throw const InvalidResponseFailure();
      }
      return data.cast<String>();
    } on DioException catch (error) {
      throw _failureFor(error);
    } on FormatException {
      throw const InvalidResponseFailure();
    }
  }

  Future<String> getText(String path) async {
    try {
      final response = await _dio.get<String>(
        path,
        options: Options(responseType: ResponseType.plain),
      );
      final content = response.data;
      if (content == null || content.isEmpty) {
        throw const InvalidResponseFailure();
      }
      return content;
    } on DioException catch (error) {
      throw _failureFor(error);
    }
  }

  Future<Map<String, Object?>> postObject(
    String path, {
    required Map<String, Object?> data,
    Duration? receiveTimeout,
    CancelToken? cancelToken,
  }) async {
    try {
      final response = await _dio.post<Object?>(
        path,
        data: data,
        options: receiveTimeout == null
            ? null
            : Options(receiveTimeout: receiveTimeout),
        cancelToken: cancelToken,
      );
      return _asObjectMap(response.data);
    } on DioException catch (error) {
      throw _failureFor(error);
    } on FormatException {
      throw const InvalidResponseFailure();
    }
  }

  Future<Map<String, Object?>> putObject(
    String path, {
    required Map<String, Object?> data,
  }) async {
    try {
      final response = await _dio.put<Object?>(path, data: data);
      return _asObjectMap(response.data);
    } on DioException catch (error) {
      throw _failureFor(error);
    } on FormatException {
      throw const InvalidResponseFailure();
    }
  }

  Future<void> delete(String path) async {
    try {
      await _dio.delete<void>(path);
    } on DioException catch (error) {
      throw _failureFor(error);
    }
  }

  Map<String, Object?> _asObjectMap(Object? value) {
    if (value is! Map<Object?, Object?>) {
      throw const FormatException('Expected an object.');
    }

    final result = <String, Object?>{};
    for (final entry in value.entries) {
      if (entry.key is! String) {
        throw const FormatException('Expected a string key.');
      }
      result[entry.key as String] = entry.value;
    }
    return result;
  }

  AppFailure _failureFor(DioException error) {
    return switch (error.type) {
      DioExceptionType.connectionTimeout ||
      DioExceptionType.sendTimeout ||
      DioExceptionType.receiveTimeout => const TimeoutFailure(),
      DioExceptionType.cancel => const CancelledFailure(),
      DioExceptionType.connectionError => const ConnectionFailure(),
      _ => AppFailureMapper.fromResponse(
        statusCode: error.response?.statusCode,
        body: error.response?.data,
      ),
    };
  }
}

abstract final class AppFailureMapper {
  static AppFailure fromResponse({int? statusCode, Object? body}) {
    final code = _errorCode(body);
    return switch (code) {
      'VALIDATION_FAILED' ||
      'MALFORMED_REQUEST' ||
      'NODES_NOT_IN_SAME_BUILDING' => const ValidationFailure(),
      'UNAUTHORIZED' => const UnauthorizedFailure(),
      'RESOURCE_NOT_FOUND' => const NotFoundFailure(),
      'CONFLICT' => const ConflictFailure(),
      'TOO_MANY_REQUESTS' => const RateLimitedFailure(),
      _ => fromStatus(statusCode),
    };
  }

  static AppFailure fromStatus(int? statusCode) {
    return switch (statusCode) {
      400 => const ValidationFailure(),
      401 => const UnauthorizedFailure(),
      404 => const NotFoundFailure(),
      409 => const ConflictFailure(),
      429 => const RateLimitedFailure(),
      _ => const ServerFailure(),
    };
  }

  static String? _errorCode(Object? body) {
    if (body is! Map<Object?, Object?>) return null;
    final code = body['code'];
    return code is String ? code : null;
  }
}
