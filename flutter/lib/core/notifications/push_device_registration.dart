import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';

/// One installation may change accounts or refresh its FCM token. PUT replaces
/// that installation's previous binding instead of accumulating stale tokens.
class PushDeviceRegistration {
  PushDeviceRegistration(this.dio, this.preferences);

  final Dio dio;
  final SharedPreferences preferences;
  static const _installationKey = 'push.installationId';

  Future<String> _installationId() async {
    final existing = preferences.getString(_installationKey);
    if (existing != null) return existing;
    final id = const Uuid().v4();
    await preferences.setString(_installationKey, id);
    return id;
  }

  Future<void> register({
    required String token,
    required String role,
    required bool enabled,
  }) async {
    final id = await _installationId();
    final response = await dio.put<dynamic>(
      'api/v1/notifications/devices/$id',
      data: {'token': token, 'role': role, 'enabled': enabled},
    );
    _check(response);
  }

  Future<void> unregister() async {
    final id = preferences.getString(_installationKey);
    if (id == null) return;
    _check(await dio.delete<dynamic>('api/v1/notifications/devices/$id'));
  }

  void _check(Response<dynamic> response) {
    if (response.statusCode != 200 ||
        response.data is! Map ||
        response.data['success'] != true) {
      throw StateError(
        'Notification registration failed (${response.statusCode})',
      );
    }
  }
}
