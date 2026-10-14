import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';

/// Completes the OTP challenge only after the authenticated home is visible.
class AuthHomeReadyDatasource {
  AuthHomeReadyDatasource({
    required DioClient dioClient,
    required RiderApiEndpoints endpoints,
    required AuthLocalDatasource authLocalDatasource,
  }) : _dioClient = dioClient,
       _endpoints = endpoints,
       _auth = authLocalDatasource;

  final DioClient _dioClient;
  final RiderApiEndpoints _endpoints;
  final AuthLocalDatasource _auth;
  Future<bool>? _pending;
  String? _pendingToken;

  Future<bool> acknowledge() async {
    if (_pending != null) {
      if (_pendingToken == _auth.otpCompletionToken) return _pending!;
      // A second account/session must complete its own challenge.
      await _pending;
      return acknowledge();
    }
    _pendingToken = _auth.otpCompletionToken;
    final pending = _acknowledge();
    _pending = pending;
    try {
      return await pending;
    } finally {
      _pending = null;
      _pendingToken = null;
    }
  }

  Future<bool> _acknowledge() async {
    final completionToken = _auth.otpCompletionToken;
    // Existing sessions and social sign-in have no pending OTP to complete.
    if (completionToken == null || completionToken.isEmpty) return true;
    if (!_auth.isAuthenticated) return false;

    try {
      final response = await _dioClient.dio.post(
        _endpoints.homeReadyEndpoint,
        data: {'otpCompletionToken': completionToken},
      );
      final body = response.data;
      final status = response.statusCode ?? 500;
      if (status < 200 ||
          status >= 300 ||
          body is! Map<String, dynamic> ||
          body['success'] != true ||
          body['data'] != true) {
        return false;
      }
      await _auth.completeOtpLogin(completionToken);
      return true;
    } catch (_) {
      // Keep the pending token for a later retry or the next app launch.
      return false;
    }
  }
}
