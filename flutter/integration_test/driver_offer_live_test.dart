import 'package:dio/dio.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:swift_app/API/api_config.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/driver/presentation/pages/driver_home_page.dart';

import 'qa_harness.dart';

const _driverId = int.fromEnvironment('QA_DRIVER_PROFILE_ID');
const _riderId = int.fromEnvironment('QA_RIDER_PROFILE_ID');
const _riderAuthId = int.fromEnvironment('QA_RIDER_AUTH_ID');
const _riderToken = String.fromEnvironment('QA_RIDER_TOKEN');
const _vehicleType = String.fromEnvironment(
  'QA_EXPECTED_VEHICLE_TYPE',
  defaultValue: 'SEDAN',
);
const _driverLatitude = 12.9680;
const _driverLongitude = 77.5946;

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('driver receives, declines, accepts and completes live rides', (
    tester,
  ) async {
    expect(_driverId, greaterThan(0));
    expect(_riderId, greaterThan(0));
    expect(_riderToken, isNotEmpty);
    final riderApi = Dio(
      BaseOptions(
        baseUrl: ApiConfig.baseUrl,
        headers: {
          'Authorization':
              'Bearer ${_riderToken.replaceFirst(RegExp(r'^Bearer\s+'), '')}',
          'Content-Type': 'application/json',
        },
      ),
    );
    int? declinedRideId;
    int? acceptedRideId;
    try {
      await launchQaApp(tester, 'DRIVER');
      await waitForWidget(tester, find.byType(DriverHomePage));
      final driver = sl<DriverRemoteDatasource>();
      await waitForWidget(tester, find.text('Drive'));

      final profile = await driver.getProfile();
      expect(profile.success, isTrue, reason: profile.message);
      expect(profile.data?.driverId, _driverId);
      if (profile.data?.driverStatus != 'ONLINE') {
        await waitForCondition(
          tester,
          () =>
              find.byType(CupertinoSwitch).evaluate().isNotEmpty &&
              tester
                      .widget<CupertinoSwitch>(
                        find.byType(CupertinoSwitch).first,
                      )
                      .onChanged !=
                  null,
          description: 'loaded driver availability control',
        );
        await tester.tap(find.byType(CupertinoSwitch).first);
        await _waitForStatus(tester, driver, 'ONLINE');
      }
      try {
        await waitForWidget(tester, find.text('You’re online'));
      } catch (_) {
        final statusTexts = tester
            .widgetList<Text>(find.byType(Text))
            .map((w) => w.data ?? '')
            .where(
              (s) =>
                  s.contains('online') ||
                  s.contains('offline') ||
                  s.contains('Location') ||
                  s.contains('location') ||
                  s.contains('availability') ||
                  s.contains('On a ride'),
            )
            .toList();
        debugPrint('QA availability UI: $statusTexts');
        debugPrint(
          'QA availability switch: ${tester.widget<CupertinoSwitch>(find.byType(CupertinoSwitch).first).value}',
        );
        rethrow;
      }
      final position = await driver.publishDriverLocation(
        driverId: _driverId,
        latitude: _driverLatitude,
        longitude: _driverLongitude,
      );
      expect(position.success, isTrue, reason: position.message);

      declinedRideId = await _bookRide(riderApi);
      await waitForWidget(tester, find.text('Ride #$declinedRideId'));
      expect(find.text('New ride request'), findsOneWidget);
      expect(find.text('Accept ride'), findsOneWidget);
      expect(find.text('Decline'), findsOneWidget);
      await _revealButton(tester, 'Decline');
      await tester.pump(const Duration(milliseconds: 400));
      await tester.tap(find.text('Decline'));
      await waitForWidget(tester, find.text('Request declined'));
      // The matcher may already mark the ride MATCH_FAILED when this was the
      // last eligible driver; cancelling a terminal ride returns 409.
      await _cancelRide(riderApi, declinedRideId, ignoreFailure: true);
      declinedRideId = null;

      // A declined request must leave the driver available for the next ride.
      await _waitForStatus(tester, driver, 'ONLINE');
      final nextPosition = await driver.publishDriverLocation(
        driverId: _driverId,
        latitude: _driverLatitude,
        longitude: _driverLongitude,
      );
      expect(nextPosition.success, isTrue, reason: nextPosition.message);
      acceptedRideId = await _bookRide(riderApi);
      await waitForWidget(tester, find.text('Ride #$acceptedRideId'));
      await _revealButton(tester, 'Accept ride');
      await tester.pump(const Duration(milliseconds: 400));
      await tester.tap(find.text('Accept ride'));
      await tester.pump(const Duration(seconds: 2));
      final decisionText = tester
          .widgetList<Text>(find.byType(Text))
          .map((widget) => widget.data ?? '')
          .where(
            (value) =>
                value.contains('Accepted') ||
                value.contains('expired') ||
                value.contains('Couldn’t') ||
                value.contains('No longer') ||
                value.contains('Request'),
          )
          .join(' | ');
      debugPrint('QA accept UI: $decisionText');
      final assignmentStatus = await _waitForRideStatusAny(
        tester,
        driver,
        acceptedRideId,
        {'DRIVER_ASSIGNED', 'DRIVER_ARRIVED'},
      );
      if (assignmentStatus == 'DRIVER_ASSIGNED') {
        await tester.dragUntilVisible(
          find.text('Arrived at pickup'),
          find.byType(CustomScrollView),
          const Offset(0, -300),
        );
        await tester.ensureVisible(find.text('Arrived at pickup'));
        await tester.pump(const Duration(milliseconds: 400));
        await tester.tap(find.text('Arrived at pickup'));
        await _waitForRideStatus(
          tester,
          driver,
          acceptedRideId,
          'DRIVER_ARRIVED',
        );
      }
      await tester.dragUntilVisible(
        find.text('Start ride'),
        find.byType(CustomScrollView),
        const Offset(0, -300),
      );
      await tester.ensureVisible(find.text('Start ride'));
      await tester.pump(const Duration(milliseconds: 400));
      await tester.tap(find.text('Start ride'));
      await _waitForRideStatus(tester, driver, acceptedRideId, 'STARTED');
      await tester.dragUntilVisible(
        find.text('Complete ride'),
        find.byType(CustomScrollView),
        const Offset(0, -300),
      );
      await tester.ensureVisible(find.text('Complete ride'));
      await tester.pump(const Duration(milliseconds: 400));
      await tester.tap(find.text('Complete ride'));
      await _waitForRideStatus(tester, driver, acceptedRideId, 'COMPLETED');
      acceptedRideId = null;
    } finally {
      if (declinedRideId != null) {
        await _cancelRide(riderApi, declinedRideId, ignoreFailure: true);
      }
      if (acceptedRideId != null) {
        await _cancelRide(riderApi, acceptedRideId, ignoreFailure: true);
      }
      riderApi.close();
    }
  });
}

Future<int> _bookRide(Dio riderApi) async {
  final response = await riderApi.post<Map<String, dynamic>>(
    'api/v1/ride/requestRide',
    data: {
      'authUserId': _riderAuthId,
      'riderId': _riderId,
      'rideType': _vehicleType,
      'route': {
        'pickupLatitude': 12.9716,
        'pickupLongitude': 77.5946,
        'destinationLatitude': 12.9751,
        'destinationLongitude': 77.5992,
        'rideType': _vehicleType,
      },
    },
  );
  expect(response.statusCode, inInclusiveRange(200, 299));
  final ride = response.data?['data'];
  expect(ride, isA<Map>());
  final id = int.tryParse((ride as Map)['id'].toString());
  expect(id, greaterThan(0));
  return id!;
}

Future<void> _cancelRide(
  Dio riderApi,
  int rideId, {
  bool ignoreFailure = false,
}) async {
  try {
    final response = await riderApi.post<Map<String, dynamic>>(
      'api/v1/ride/rides/$rideId/cancel',
      data: {
        'actorId': _riderId,
        'actorType': 'RIDER',
        'reason': 'QA offer flow cleanup',
      },
    );
    if (!ignoreFailure) {
      expect(response.statusCode, inInclusiveRange(200, 299));
    }
  } catch (_) {
    if (!ignoreFailure) rethrow;
  }
}

Future<void> _waitForStatus(
  WidgetTester tester,
  DriverRemoteDatasource driver,
  String status,
) async {
  for (var i = 0; i < 25; i++) {
    await tester.pump(const Duration(seconds: 1));
    final profile = await driver.getProfile();
    if (profile.data?.driverStatus == status) return;
  }
  fail('Driver did not reach $status.');
}

Future<void> _waitForRideStatus(
  WidgetTester tester,
  DriverRemoteDatasource driver,
  int rideId,
  String status,
) async {
  String? lastStatus;
  String? lastMessage;
  for (var i = 0; i < 25; i++) {
    await tester.pump(const Duration(seconds: 1));
    final ride = await driver.getRideDetail(rideId);
    lastStatus = ride.data?.currentStatus;
    lastMessage = ride.message;
    if (ride.data?.currentStatus == status) return;
  }
  fail('Ride $rideId did not reach $status (last: $lastStatus; $lastMessage).');
}

Future<String> _waitForRideStatusAny(
  WidgetTester tester,
  DriverRemoteDatasource driver,
  int rideId,
  Set<String> statuses,
) async {
  String? lastStatus;
  for (var i = 0; i < 25; i++) {
    await tester.pump(const Duration(seconds: 1));
    final ride = await driver.getRideDetail(rideId);
    lastStatus = ride.data?.currentStatus;
    if (lastStatus != null && statuses.contains(lastStatus)) {
      return lastStatus;
    }
  }
  fail('Ride $rideId did not reach $statuses (last: $lastStatus).');
}

Future<void> _revealButton(WidgetTester tester, String label) async {
  final finder = find.text(label);
  for (var i = 0; i < 10; i++) {
    await tester.pump(const Duration(milliseconds: 150));
    if (finder.hitTestable().evaluate().isNotEmpty) return;
    await tester.drag(
      find.byType(CustomScrollView).first,
      const Offset(0, -240),
    );
  }
  fail('The $label action could not be brought into view.');
}
