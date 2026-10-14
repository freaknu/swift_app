import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/features/rider/ride/data/datasource/rider_location_datasource.dart';
import 'package:swift_app/features/rider/ride/domain/entity/driver_position_fix.dart';

void main() {
  final first = {
    'eventId': 'gps-1',
    'eventTime': '2026-10-11T10:00:00Z',
    'rideId': 17,
    'driverId': 5,
    'latitude': 12.9716,
    'longitude': 77.5946,
    'etaMinutes': 4,
    'distanceToPickupMeters': 850.0,
  };

  test('driver fix validates identity, freshness, and update order', () {
    final fix = DriverPositionFix.tryParse(first)!;
    expect(fix.belongsTo(rideId: 17, driverId: 5), isTrue);
    expect(fix.belongsTo(rideId: 17, driverId: 6), isFalse);
    expect(fix.isFreshAt(DateTime.utc(2026, 10, 11, 10, 0, 30)), isTrue);
    expect(fix.isFreshAt(DateTime.utc(2026, 10, 11, 10, 1, 10)), isFalse);
    expect(fix.etaMinutes, 4);

    final duplicate = DriverPositionFix.tryParse({...first})!;
    final older = DriverPositionFix.tryParse({
      ...first,
      'eventId': 'gps-older',
      'eventTime': '2026-10-11T09:59:59Z',
    })!;
    final newer = DriverPositionFix.tryParse({
      ...first,
      'eventId': 'gps-2',
      'eventTime': '2026-10-11T10:00:10Z',
    })!;
    expect(duplicate.isNewerThan(fix), isFalse);
    expect(older.isNewerThan(fix), isFalse);
    expect(newer.isNewerThan(fix), isTrue);
    expect(DriverPositionFix.tryParse({...first, 'latitude': 140}), isNull);
    expect(DriverPositionFix.tryParse({...first, 'eventTime': null}), isNull);
  });

  test(
    'snapshot datasource reads latest position and nullable snapshot',
    () async {
      final paths = <String>[];
      Map<String, dynamic>? payload = first;
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost/'));
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (request, handler) {
            paths.add(request.path);
            handler.resolve(
              Response(
                requestOptions: request,
                statusCode: 200,
                data: {'data': payload},
              ),
            );
          },
        ),
      );
      final datasource = RiderLocationDatasource(dio);

      expect((await datasource.latestDriverPosition(17))?.driverId, 5);
      expect(paths.single, 'api/v1/location/rides/17/driver-position');
      payload = {};
      await expectLater(
        datasource.latestDriverPosition(17),
        throwsA(isA<FormatException>()),
      );
      payload = null;
      expect(await datasource.latestDriverPosition(17), isNull);
    },
  );
}
