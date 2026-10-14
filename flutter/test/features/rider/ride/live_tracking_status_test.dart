import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/features/rider/ride/data/datasource/rider_location_datasource.dart';
import 'package:swift_app/features/rider/ride/domain/entity/driver_position_fix.dart';
import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';
import 'package:swift_app/features/rider/ride/domain/repository/ride_repository.dart';
import 'package:swift_app/features/rider/ride/domain/usecases/ride_lifecycle_usecase.dart';
import 'package:swift_app/features/rider/ride/presentation/pages/ride_status_page.dart';

class _LocationFake extends RiderLocationDatasource {
  final DriverPositionFix fix;

  _LocationFake(this.fix) : super(Dio());

  @override
  Future<DriverPositionFix?> latestDriverPosition(int rideId) async => fix;
}

class _RideFake implements RideRepository {
  final RideResponse ride;

  _RideFake(this.ride);

  @override
  Future<RideResponse> getRide(int rideId) async => ride;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  testWidgets('reduced motion keeps matching feedback without a pulse ticker', (
    tester,
  ) async {
    final events = StreamController<Map<String, dynamic>>.broadcast();
    addTearDown(events.close);
    const ride = RideResponse(id: 17, currentStatus: 'MATCHING');
    await tester.pumpWidget(
      MaterialApp(
        home: MediaQuery(
          data: const MediaQueryData(disableAnimations: true),
          child: RideStatusPage(
            ride: ride,
            riderId: 3,
            lifecycle: RideLifecycleUsecase(_RideFake(ride)),
            realtimeEvents: events.stream,
            mapOverride: const SizedBox.expand(),
          ),
        ),
      ),
    );
    events.add({
      'rideId': 17,
      'rideStatus': 'SEARCHING',
      'radiusKm': 2,
      'attempt': 2,
      'driverCount': 3,
    });
    await tester.pump();
    await tester.pump();
    await tester.pumpAndSettle();
    expect(find.textContaining('Searching within 2 km'), findsOneWidget);
    expect(tester.binding.transientCallbackCount, 0);
    await tester.pumpWidget(const SizedBox.shrink());
  });

  testWidgets('marker interpolation does not rebuild the ride screen', (
    tester,
  ) async {
    final events = StreamController<Map<String, dynamic>>.broadcast();
    addTearDown(events.close);
    const ride = RideResponse(
      id: 17,
      riderId: 3,
      driverId: 5,
      currentStatus: 'DRIVER_ASSIGNED',
      pickupLatitude: 12.9716,
      pickupLongitude: 77.5946,
      destinationLatitude: 12.98,
      destinationLongitude: 77.60,
    );
    final now = DateTime.now().toUtc();
    final fix = DriverPositionFix.tryParse({
      'rideId': 17,
      'driverId': 5,
      'eventId': 'first',
      'eventTime': now.toIso8601String(),
      'latitude': 12.969,
      'longitude': 77.595,
      'etaMinutes': 4,
    })!;
    var rideBuilds = 0;
    final previousObserver = debugOnRebuildDirtyWidget;
    debugOnRebuildDirtyWidget = (element, builtOnce) {
      previousObserver?.call(element, builtOnce);
      if (element.widget is RideStatusPage) rideBuilds++;
    };
    addTearDown(() => debugOnRebuildDirtyWidget = previousObserver);
    await tester.pumpWidget(
      MaterialApp(
        home: RideStatusPage(
          ride: ride,
          riderId: 3,
          lifecycle: RideLifecycleUsecase(_RideFake(ride)),
          locationDatasource: _LocationFake(fix),
          realtimeEvents: events.stream,
          mapOverride: const SizedBox.expand(),
        ),
      ),
    );
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 1200));
    events.add({
      'rideId': 17,
      'driverId': 5,
      'eventId': 'next',
      'eventTime': now.add(const Duration(seconds: 1)).toIso8601String(),
      'latitude': 12.970,
      'longitude': 77.596,
      'etaMinutes': 3,
    });
    await tester.pump();
    await tester.pump();
    expect(find.text('3 min'), findsOneWidget);
    final buildsAfterFix = rideBuilds;
    expect(buildsAfterFix, greaterThan(0));
    await tester.pump(const Duration(milliseconds: 550));
    await tester.pump(const Duration(milliseconds: 650));
    expect(rideBuilds, buildsAfterFix);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox.shrink());
  });

  testWidgets(
    'assigned ride hydrates driver GPS and ignores older socket ETA',
    (tester) async {
      final events = StreamController<Map<String, dynamic>>.broadcast();
      addTearDown(events.close);
      const ride = RideResponse(
        id: 17,
        riderId: 3,
        driverId: 5,
        currentStatus: 'DRIVER_ASSIGNED',
        pickupLatitude: 12.9716,
        pickupLongitude: 77.5946,
        destinationLatitude: 12.98,
        destinationLongitude: 77.60,
      );
      final now = DateTime.now().toUtc();
      final fix = DriverPositionFix.tryParse({
        'rideId': 17,
        'driverId': 5,
        'eventId': 'new',
        'eventTime': now.toIso8601String(),
        'latitude': 12.969,
        'longitude': 77.595,
        'distanceToPickupMeters': 430,
        'etaMinutes': 4,
      })!;
      await tester.pumpWidget(
        MaterialApp(
          home: RideStatusPage(
            ride: ride,
            riderId: 3,
            lifecycle: RideLifecycleUsecase(_RideFake(ride)),
            locationDatasource: _LocationFake(fix),
            realtimeEvents: events.stream,
            mapOverride: const SizedBox.expand(),
          ),
        ),
      );
      await tester.pump();
      expect(find.text('4 min'), findsOneWidget);
      expect(find.text('430 m from pickup'), findsOneWidget);

      events.add({
        'rideId': 17,
        'driverId': 5,
        'eventId': 'old',
        'eventTime': now.subtract(const Duration(seconds: 4)).toIso8601String(),
        'latitude': 12.968,
        'longitude': 77.595,
        'etaMinutes': 1,
      });
      await tester.pump();
      expect(find.text('4 min'), findsOneWidget);
      expect(find.text('1 min'), findsNothing);
      expect(tester.takeException(), isNull);
    },
  );
}
