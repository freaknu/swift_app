import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/driver/data/model/driver_notification_preferences_model.dart';

void main() {
  test('document file upload sends an authenticated multipart form', () async {
    SharedPreferences.setMockInitialValues({'auth.userId': 410});
    final prefs = await SharedPreferences.getInstance();
    final client = DioClient(
      accessTokenReader: () async => 'fixture-token',
      authUserIdReader: () async => 410,
    );
    RequestOptions? request;
    client.dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          request = options;
          handler.resolve(
            Response(
              requestOptions: options,
              statusCode: 200,
              data: {
                'success': true,
                'statusCode': 200,
                'data': {
                  'id': 9,
                  'documentType': 'DRIVING_LICENCE',
                  'documentUrl': '/api/v1/document/files/owned-id',
                  'verificationStatus': 'PENDING',
                },
              },
            ),
          );
        },
      ),
    );
    final datasource = DriverRemoteDatasource(
      dioClient: client,
      endpoints: RiderApiEndpoints(),
      authLocalDatasource: AuthLocalDatasource(preferences: prefs),
    );
    final response = await datasource.uploadDocumentFile(
      documentType: 'DRIVING_LICENCE',
      fileName: 'licence.pdf',
      bytes: Uint8List.fromList([37, 80, 68, 70]),
    );

    expect(response.success, isTrue);
    expect(response.data?.verificationStatus, 'PENDING');
    expect(request?.path, 'api/v1/document/upload-file');
    expect(request?.headers['Authorization'], 'Bearer fixture-token');
    expect(request?.headers['x-user-id'], '410');
    final form = request?.data as FormData;
    expect(form.fields.single.value, 'DRIVING_LICENCE');
    expect(form.files.single.key, 'file');
    expect(form.files.single.value.filename, 'licence.pdf');
  });

  test(
    'driver dashboard reads owned stats, ratings, and saved preferences',
    () async {
      SharedPreferences.setMockInitialValues({'auth.userId': 410});
      final prefs = await SharedPreferences.getInstance();
      final client = DioClient(
        accessTokenReader: () async => 'fixture-token',
        authUserIdReader: () async => 410,
      );
      final requests = <RequestOptions>[];
      client.dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            requests.add(options);
            final data = switch (options.path) {
              'api/v1/driverStatic/getDriverStatic' => {
                'completedTrips': 8,
                'cancelledTrips': 2,
                'lifetimeDistance': 92.5,
                'lifetimeEarning': 3480.75,
                'averageRating': 4.7,
              },
              'api/v1/driver/earnings' => {
                'thisWeekIncome': 500,
                'thisMonthIncome': 1800,
                'thisLifetimeIncome': 3480,
              },
              'api/v1/ride/drivers/me/rides/history' => {
                'content': [
                  {
                    'id': 88,
                    'rideTypeCode': 'SEDAN',
                    'currentStatus': 'COMPLETED',
                    'requestedAt': '2026-10-11T10:00:00',
                    'actualDistance': 4.2,
                    'fareDetail': {'totalFare': 320.5, 'currency': 'INR'},
                  },
                ],
                'page': 0,
                'size': 20,
                'totalElements': 1,
                'totalPages': 1,
                'last': true,
              },
              'api/v1/rating/getRating' => {
                'id': 12,
                'averageRating': 4.7,
                'allRatings': [
                  {'id': 19, 'rideId': 88, 'rate': 5.0},
                ],
              },
              'api/v1/ride/rides/88/rider-summary' => {
                'riderId': 19,
                'riderName': 'Maya',
              },
              'api/v1/driver/notification-preferences' =>
                options.method == 'PUT'
                    ? options.data
                    : {
                        'pushEnabled': true,
                        'smsEnabled': false,
                        'emailEnabled': true,
                        'rideAlertsEnabled': true,
                        'surgeAlertsEnabled': false,
                      },
              _ => throw StateError(
                'Unexpected driver endpoint ${options.path}',
              ),
            };
            handler.resolve(
              Response(
                requestOptions: options,
                statusCode: 200,
                data: {'success': true, 'statusCode': 200, 'data': data},
              ),
            );
          },
        ),
      );
      final datasource = DriverRemoteDatasource(
        dioClient: client,
        endpoints: RiderApiEndpoints(),
        authLocalDatasource: AuthLocalDatasource(preferences: prefs),
      );

      final stats = await datasource.getStatistics(72);
      final earnings = await datasource.getEarnings();
      final journeys = await datasource.getJourneys();
      await datasource.getJourneys(status: 'COMPLETED');
      final rating = await datasource.getRating(72);
      final riderSummary = await datasource.getRiderSummary(88);
      final preferences = await datasource.getNotificationPreferences();
      final updated = await datasource.updateNotificationPreferences(
        preferences.data!.copyWith(pushEnabled: false),
      );

      expect(stats.data!.completedTrips, 8);
      expect(stats.data!.lifetimeEarning, 3480.75);
      expect(earnings.data!.thisWeek, 500);
      expect(journeys.data!.content.single.rideTypeCode, 'SEDAN');
      expect(journeys.data!.content.single.fare, 320.5);
      expect(journeys.data!.last, isTrue);
      expect(rating.data!.entries.single.rideId, 88);
      expect(rating.data!.averageRating, 4.7);
      expect(riderSummary.data!.riderName, 'Maya');
      expect(updated.data!.pushEnabled, isFalse);
      expect(requests.map((request) => '${request.method} ${request.path}'), [
        'GET api/v1/driverStatic/getDriverStatic',
        'GET api/v1/driver/earnings',
        'GET api/v1/ride/drivers/me/rides/history',
        'GET api/v1/ride/drivers/me/rides/history',
        'GET api/v1/rating/getRating',
        'GET api/v1/ride/rides/88/rider-summary',
        'GET api/v1/driver/notification-preferences',
        'PUT api/v1/driver/notification-preferences',
      ]);
      expect(requests.first.queryParameters['driverId'], 72);
      expect(requests[2].queryParameters, {'page': 0, 'size': 20});
      expect(requests[3].queryParameters, {
        'page': 0,
        'size': 20,
        'status': 'COMPLETED',
      });
      expect(requests[4].queryParameters['driverId'], 72);
      expect(requests.last.data, isA<Map<String, dynamic>>());
      expect(
        DriverNotificationPreferencesModel.fromJson(
          requests.last.data,
        ).pushEnabled,
        isFalse,
      );
      expect(
        requests.every((request) => request.headers['x-user-id'] == '410'),
        isTrue,
      );
    },
  );
}
