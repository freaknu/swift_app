import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/features/rider/home/data/datasource/nearby_driver_datasource.dart';

void main() {
  test('nearby map requests live drivers and filters stale or invalid points',
      () async {
    RequestOptions? sent;
    final dio = Dio(BaseOptions(baseUrl: 'http://localhost/'));
    dio.interceptors.add(InterceptorsWrapper(onRequest: (request, handler) {
      sent = request;
      handler.resolve(Response(
        requestOptions: request,
        statusCode: 200,
        data: {
          'data': [
            {
              'driverId': 7,
              'latitude': 12.9716,
              'longitude': 77.5946,
              'vehicleType': 'SEDAN',
              'updatedAt': DateTime.now().toUtc().toIso8601String(),
            },
            {
              'driverId': 8,
              'latitude': 12.97,
              'longitude': 77.59,
              'vehicleType': 'SUV',
              'updatedAt': DateTime.now()
                  .toUtc()
                  .subtract(const Duration(minutes: 2))
                  .toIso8601String(),
            },
            {
              'driverId': 9,
              'latitude': 100,
              'longitude': 77.59,
              'updatedAt': DateTime.now().toUtc().toIso8601String(),
            },
          ],
        },
      ));
    }));
    final nearby = await NearbyDriverDatasource(dio).fetch(
      latitude: 12.9716,
      longitude: 77.5946,
    );
    expect(nearby.map((driver) => driver.driverId), [7]);
    expect(nearby.single.vehicleType, 'SEDAN');
    expect(sent?.path, 'api/v1/location/nearbyDrivers');
    expect(sent?.queryParameters, {
      'latitude': 12.9716,
      'longitude': 77.5946,
      'distance': 5.0,
    });
  });
}
