import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:swift_app/features/rider/ride/data/datasource/rider_route_datasource.dart';

void main() {
  test(
    'road route sends the assigned ride and decodes only backend geometry',
    () async {
      RequestOptions? sent;
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost/'));
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (request, handler) {
            sent = request;
            handler.resolve(
              Response(
                requestOptions: request,
                statusCode: 200,
                data: {
                  'data': {'polyline': '_p~iF~ps|U_ulLnnqC_mqNvxq`@'},
                },
              ),
            );
          },
        ),
      );

      final route = await RiderRouteDatasource(dio).fetch(
        rideId: 12,
        driverId: 7,
        origin: const LatLng(38.5, -120.2),
        destination: const LatLng(43.252, -126.453),
        rideTypeCode: 'SEDAN',
      );

      expect(sent?.path, 'api/v1/location/eta');
      expect(sent?.data, {
        'rideId': '12',
        'driverId': '7',
        'driverLatitude': 38.5,
        'driverLongitude': -120.2,
        'destinationLatitude': 43.252,
        'destinationLongitude': -126.453,
        'rideType': 'SEDAN',
      });
      expect(route?.points, const [
        LatLng(38.5, -120.2),
        LatLng(40.7, -120.95),
        LatLng(43.252, -126.453),
      ]);
    },
  );

  test('missing backend polyline does not fabricate a road route', () async {
    final dio = Dio(BaseOptions(baseUrl: 'http://localhost/'));
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (request, handler) {
          handler.resolve(
            Response(
              requestOptions: request,
              statusCode: 200,
              data: {
                'data': {'etaMinutes': 8},
              },
            ),
          );
        },
      ),
    );

    final route = await RiderRouteDatasource(dio).fetch(
      rideId: 12,
      driverId: 7,
      origin: const LatLng(12.9716, 77.5946),
      destination: const LatLng(12.98, 77.6),
    );
    expect(route, isNull);
  });
}
