import 'package:dio/dio.dart';
import 'package:swift_app/API/api_config.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';

class DioClient {
  late final Dio dio;

  final Future<String?> Function()? _accessTokenReader;
  final Future<int?> Function()? _authUserIdReader;

  /// Reused across requests: building the endpoint object and the public
  /// path list on every request wastes allocations on a hot path.
  final RiderApiEndpoints _endpoints = RiderApiEndpoints();
  late final List<String> _publicEndpoints = [
    _endpoints.loginEndpoint,
    _endpoints.signupEndpoint,
    _endpoints.verifyOtpEndpoint,
    _endpoints.sendOtpEndpoint,
    _endpoints.googleLoginEndpoint,
    _endpoints.appleLoginEndpoint,
    _endpoints.refreshTokenEndpoint,
    _endpoints.validateTokenEndpoint,
    _endpoints.passwordResetRequestEndpoint,
    _endpoints.passwordResetConfirmEndpoint,
    _endpoints.verifyEmailEndpoint,
  ];

  DioClient({
    Future<String?> Function()? accessTokenReader,
    Future<int?> Function()? authUserIdReader,
  }) : _accessTokenReader = accessTokenReader,
       _authUserIdReader = authUserIdReader {
    dio = Dio(
      BaseOptions(
        baseUrl: ApiConfig.baseUrl,
        connectTimeout: const Duration(seconds: 10),
        receiveTimeout: const Duration(seconds: 10),
        sendTimeout: const Duration(seconds: 10),

        // Allows us to receive and handle non-2xx responses ourselves.
        validateStatus: (_) => true,

        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ),
    );

    // JWT / Authentication Interceptor
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final isPublicEndpoint = _publicEndpoints.any(
            (endpoint) => options.path.startsWith(endpoint),
          );

          if (!isPublicEndpoint) {
            final token = await _accessTokenReader?.call();

            if (token != null && token.isNotEmpty) {
              options.headers['Authorization'] = 'Bearer $token';
            }

            final authUserId = await _authUserIdReader?.call();
            if (authUserId != null) {
              options.headers['x-user-id'] = authUserId.toString();
            }
          }

          handler.next(options);
        },
      ),
    );

  }
}
