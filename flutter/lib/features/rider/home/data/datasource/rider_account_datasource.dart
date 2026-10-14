import 'package:dio/dio.dart';
import 'package:swift_app/API/api_error_message.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';

/// Account endpoints use the authenticated ID, never the rider profile ID.
class RiderAccountDatasource {
  final Dio dio;
  final AuthLocalDatasource auth;
  final RiderApiEndpoints endpoints;
  RiderAccountDatasource(this.dio, this.auth, this.endpoints);

  int get _owner => auth.userId ?? (throw StateError('Please sign in again.'));

  dynamic _read(Response<dynamic> response) {
    final body = response.data;
    if (response.statusCode == null ||
        response.statusCode! < 200 ||
        response.statusCode! >= 300 ||
        body is! Map ||
        body['success'] == false) {
      throw StateError(
        body is Map
            ? body['message']?.toString() ?? 'Unable to save changes.'
            : 'Unable to reach Swift. Please retry.',
      );
    }
    return body['data'];
  }

  Future<dynamic> _readRequest(Future<Response<dynamic>> request) async {
    try {
      return _read(await request);
    } on DioException catch (error) {
      throw StateError(userFacingDioMessage(error));
    }
  }

  Future<Map<String, dynamic>> preferences() async => Map<String, dynamic>.from(
    await _readRequest(dio.get(endpoints.userPreferencesEndpoint)) as Map,
  );

  Future<void> savePreferences(Map<String, dynamic> data) async {
    await _readRequest(dio.put(endpoints.userPreferencesEndpoint, data: data));
  }

  Future<void> updateProfile(Map<String, dynamic> data) async {
    await _readRequest(
      dio.post(endpoints.updateUser('swift-mobile'), data: data),
    );
  }

  Future<List<Map<String, dynamic>>> contacts() async => _contacts(
    await _readRequest(dio.get(endpoints.getEmergencyContact(_owner))),
  );

  Future<List<Map<String, dynamic>>> addContact(
    Map<String, dynamic> data,
  ) async => _contacts(
    await _readRequest(dio.post(endpoints.addContact(_owner), data: [data])),
  );

  Future<List<Map<String, dynamic>>> removeContact(int id) async => _contacts(
    await _readRequest(dio.delete(endpoints.deleteContact(_owner, id))),
  );

  Future<void> removeAddress(int id) async {
    await _readRequest(dio.delete(endpoints.deleteAddress(_owner, id)));
  }

  static List<Map<String, dynamic>> _contacts(dynamic data) =>
      (data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
}
