import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/rider/home/data/datasource/rider_account_datasource.dart';
import 'package:swift_app/features/rider/home/data/datasource/user_remote_datasource.dart';

void main() {
  test(
    'rider and driver timeout failures show retry text, not Dio internals',
    () async {
      SharedPreferences.setMockInitialValues({'auth.userId': 410});
      final preferences = await SharedPreferences.getInstance();
      final auth = AuthLocalDatasource(preferences: preferences);
      final client = DioClient(authUserIdReader: () async => auth.userId);
      client.dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (request, handler) {
            handler.reject(
              DioException(
                requestOptions: request,
                type: DioExceptionType.connectionTimeout,
                message:
                    'RequestOptions.connectTimeout exceeded for ${request.uri}',
              ),
            );
          },
        ),
      );
      final endpoints = RiderApiEndpoints();
      const friendly =
          'Connection timed out. Check your connection and try again.';

      final rider = UserRemoteDatasource(
        dioClient: client,
        riderApiEndpoints: endpoints,
        authLocalDatasource: auth,
      );
      final riderResponse = await rider.fetchUserDetail();
      expect(riderResponse.success, isFalse);
      expect(riderResponse.message, friendly);
      expect(riderResponse.message, isNot(contains('RequestOptions')));

      final driver = DriverRemoteDatasource(
        dioClient: client,
        endpoints: endpoints,
        authLocalDatasource: auth,
      );
      final driverResponse = await driver.getStatistics(410);
      expect(driverResponse.success, isFalse);
      expect(driverResponse.message, friendly);

      final account = RiderAccountDatasource(client.dio, auth, endpoints);
      await expectLater(
        account.preferences(),
        throwsA(
          isA<StateError>().having(
            (error) => error.message,
            'message',
            friendly,
          ),
        ),
      );
    },
  );
}
