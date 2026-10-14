import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/rider/home/data/datasource/rider_account_datasource.dart';

Future<(RiderAccountDatasource, List<RequestOptions>)> _fixture({
  int? authUserId = 410,
  bool rejectPreferencesSave = false,
}) async {
  final initialPreferences = <String, Object>{};
  if (authUserId != null) initialPreferences['auth.userId'] = authUserId;
  SharedPreferences.setMockInitialValues(initialPreferences);
  final prefs = await SharedPreferences.getInstance();
  final auth = AuthLocalDatasource(preferences: prefs);
  final client = DioClient(
    accessTokenReader: () async => authUserId == null ? null : 'fixture-token',
    authUserIdReader: () async => auth.userId,
  );
  final requests = <RequestOptions>[];
  client.dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (request, handler) {
        requests.add(request);
        if (rejectPreferencesSave &&
            request.path == 'api/v1/user/preferences' &&
            request.method == 'PUT') {
          handler.resolve(
            Response(
              requestOptions: request,
              statusCode: 403,
              data: {'success': false, 'message': 'Preference owner mismatch'},
            ),
          );
          return;
        }
        final data = switch ('${request.method} ${request.path}') {
          'GET api/v1/user/preferences' => {
            'pushNotificationEnabled': true,
            'emailNotificationEnabled': false,
            'smsNotificationEnabled': false,
          },
          'PUT api/v1/user/preferences' => request.data,
          'POST api/v1/user/updateUser/swift-mobile' => request.data,
          'GET api/v1/user/getEmergencyContact/410' => [
            {'id': 8, 'contactPersonName': 'Ravi'},
          ],
          'POST api/v1/user/addContact/410' => [
            {'id': 8, 'contactPersonName': 'Ravi'},
            {'id': 9, 'contactPersonName': 'Asha'},
          ],
          'DELETE api/v1/user/deleteContact/410/9' => [
            {'id': 8, 'contactPersonName': 'Ravi'},
          ],
          'DELETE api/v1/user/deleteAddress/410/99' => true,
          _ => throw StateError('Unexpected account request: ${request.path}'),
        };
        handler.resolve(
          Response(
            requestOptions: request,
            statusCode: 200,
            data: {'success': true, 'statusCode': 200, 'data': data},
          ),
        );
      },
    ),
  );
  return (
    RiderAccountDatasource(client.dio, auth, RiderApiEndpoints()),
    requests,
  );
}

void main() {
  test(
    'preferences, profile and deletes use authenticated account ownership',
    () async {
      // Auth user 410 and rider profile 7 are intentionally different IDs.
      final (api, requests) = await _fixture();
      final preferences = await api.preferences();
      expect(preferences['pushNotificationEnabled'], isTrue);
      final changed = {
        ...preferences,
        'pushNotificationEnabled': false,
        'emailNotificationEnabled': true,
      };
      await api.savePreferences(changed);
      await api.updateProfile({
        'id': 7,
        'authUserId': 410,
        'fullName': 'Maya Rider',
      });
      expect((await api.contacts()).single['id'], 8);
      final updatedContacts = await api.addContact({
        'contactPersonName': 'Asha',
        'phoneNumber': '9999999999',
      });
      expect(updatedContacts.last['id'], 9);
      expect((await api.removeContact(9)).single['id'], 8);
      await api.removeAddress(99);

      expect(requests.map((request) => '${request.method} ${request.path}'), [
        'GET api/v1/user/preferences',
        'PUT api/v1/user/preferences',
        'POST api/v1/user/updateUser/swift-mobile',
        'GET api/v1/user/getEmergencyContact/410',
        'POST api/v1/user/addContact/410',
        'DELETE api/v1/user/deleteContact/410/9',
        'DELETE api/v1/user/deleteAddress/410/99',
      ]);
      expect(requests[1].data, changed);
      expect(requests[2].data, {
        'id': 7,
        'authUserId': 410,
        'fullName': 'Maya Rider',
      });
      expect(requests[4].data, [
        {'contactPersonName': 'Asha', 'phoneNumber': '9999999999'},
      ]);
      expect(
        requests.every(
          (request) =>
              request.headers['Authorization'] == 'Bearer fixture-token' &&
              request.headers['x-user-id'] == '410',
        ),
        isTrue,
      );
    },
  );

  test('backend ownership rejection is surfaced on preference save', () async {
    final (api, _) = await _fixture(rejectPreferencesSave: true);

    await expectLater(
      api.savePreferences({'pushNotificationEnabled': true}),
      throwsA(
        isA<StateError>().having(
          (error) => error.message,
          'message',
          'Preference owner mismatch',
        ),
      ),
    );
  });

  test(
    'signed-out user cannot request account-owned contacts or deletes',
    () async {
      final (api, requests) = await _fixture(authUserId: null);

      await expectLater(api.contacts(), throwsStateError);
      await expectLater(api.removeContact(9), throwsStateError);
      await expectLater(api.removeAddress(99), throwsStateError);
      expect(requests, isEmpty);
    },
  );
}
