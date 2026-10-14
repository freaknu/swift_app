import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:integration_test/integration_test.dart';
import 'package:swift_app/API/api_config.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/core/notifications/notification_inbox_datasource.dart';
import 'package:swift_app/core/notifications/notification_inbox_page.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_history_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/models/location_selection.dart';
import 'package:swift_app/features/rider/home/presentation/pages/home_page.dart';
import 'package:swift_app/features/rider/home/presentation/pages/trip_route_map_page.dart';
import 'package:swift_app/features/rider/ride/data/model/ride_response_model.dart';
import 'package:swift_app/features/rider/ride/presentation/blocs/ride_bloc.dart';
import 'package:swift_app/features/rider/ride/presentation/pages/ride_status_page.dart';

import 'qa_harness.dart';

const _riderId = int.fromEnvironment('QA_RIDER_PROFILE_ID');
const _riderAuthId = int.fromEnvironment('QA_RIDER_AUTH_ID');
const _riderToken = String.fromEnvironment('QA_RIDER_TOKEN');
const _driverId = int.fromEnvironment('QA_DRIVER_PROFILE_ID');
const _driverToken = String.fromEnvironment('QA_DRIVER_TOKEN');
const _vehicleType = String.fromEnvironment(
  'QA_EXPECTED_VEHICLE_TYPE',
  defaultValue: 'SEDAN',
);
final _pickup = LatLng(
  double.parse(
    const String.fromEnvironment('QA_PICKUP_LAT', defaultValue: '12.9716'),
  ),
  double.parse(
    const String.fromEnvironment('QA_PICKUP_LON', defaultValue: '77.5946'),
  ),
);
final _destination = LatLng(
  double.parse(
    const String.fromEnvironment('QA_DESTINATION_LAT', defaultValue: '12.9352'),
  ),
  double.parse(
    const String.fromEnvironment('QA_DESTINATION_LON', defaultValue: '77.6245'),
  ),
);

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('rider quotes, tracks a moving driver, and rates a live ride', (
    tester,
  ) async {
    expect(_riderId, greaterThan(0));
    expect(_riderAuthId, greaterThan(0));
    expect(_driverId, greaterThan(0));
    expect(_riderToken, isNotEmpty);
    expect(_driverToken, isNotEmpty);
    await launchQaApp(tester, 'RIDER');
    await waitForWidget(tester, find.byType(HomePage));
    final homeContext = tester.element(find.byType(HomePage));
    final user = homeContext.read<UserBloc>();
    final catalog = homeContext.read<RideTypeBloc>();
    final rideBloc = homeContext.read<RideBloc>();
    await waitForCondition(
      tester,
      () => user.state.user != null && catalog.state.rideTypes.isNotEmpty,
      description: 'rider profile and vehicle catalog',
    );
    expect(user.state.user?.id, _riderId);
    expect(
      catalog.state.rideTypes.any((type) => type.code == _vehicleType),
      isTrue,
    );
    expect(
      await sl<NotificationInboxDatasource>().fetch('RIDER'),
      isA<List<InboxNotification>>(),
    );
    await tester.tap(find.byType(NotificationBell));
    await waitForWidget(tester, find.byType(NotificationInboxPage));
    await tester.pageBack();
    await tester.pump();

    final trip = TripLocations(
      pickup: LocationSelection.fromCoordinates(
        latitude: _pickup.latitude,
        longitude: _pickup.longitude,
        label: 'QA pickup',
      ),
      destination: LocationSelection.fromCoordinates(
        latitude: _destination.latitude,
        longitude: _destination.longitude,
        label: 'QA destination',
      ),
    );
    Navigator.of(homeContext).push(
      MaterialPageRoute(
        builder: (_) => MultiBlocProvider(
          providers: [
            BlocProvider<UserBloc>.value(value: user),
            BlocProvider<RideTypeBloc>.value(value: catalog),
            BlocProvider<RideHistoryBloc>.value(
              value: homeContext.read<RideHistoryBloc>(),
            ),
            BlocProvider<OfferBloc>.value(value: homeContext.read<OfferBloc>()),
            BlocProvider<FareBloc>.value(value: homeContext.read<FareBloc>()),
            BlocProvider<RideBloc>.value(value: rideBloc),
          ],
          child: TripRouteMapPage(
            trip: trip,
            preferredRideTypeCode: _vehicleType,
          ),
        ),
      ),
    );
    await waitForWidget(tester, find.byType(TripRouteMapPage));
    expect(find.text('Choose your ride'), findsWidgets);
    final selectedType = catalog.state.rideTypes
        .where((type) => type.code == _vehicleType)
        .first;
    final bookButton = find.widgetWithText(
      FilledButton,
      'Book ${selectedType.name}',
    );
    await waitForCondition(
      tester,
      () =>
          bookButton.evaluate().isNotEmpty &&
          tester.widget<FilledButton>(bookButton).onPressed != null,
      description: 'selected vehicle fare quote and enabled booking',
    );
    expect(find.textContaining('₹'), findsWidgets);
    expect(find.text('Fare unavailable'), findsNothing);

    // launchQaApp has already verified the .qa package before any fixture write.
    final riderApi = _api(_riderToken);
    final driverApi = _api(_driverToken);
    int? rideId;
    String? oldDriverStatus;
    try {
      final profile = await driverApi.get<Map<String, dynamic>>(
        'api/v1/driver/profile',
      );
      _expectSuccess(profile);
      oldDriverStatus = (profile.data?['data'] as Map?)?['driverStatus']
          ?.toString();
      expect(oldDriverStatus, anyOf('ONLINE', 'OFFLINE'));
      if (oldDriverStatus == 'OFFLINE') {
        _expectSuccess(
          await driverApi.put<Map<String, dynamic>>(
            'api/v1/driver/status',
            queryParameters: {'driverStatus': 'ONLINE'},
          ),
        );
      }
      await _publishGps(
        driverApi,
        LatLng(_pickup.latitude - 0.0036, _pickup.longitude),
      );
      await _waitForAvailableDriver(tester, riderApi);

      await tester.ensureVisible(bookButton);
      await tester.tap(bookButton);
      await waitForWidget(tester, find.byType(RideStatusPage));
      final requestedRideId = tester
          .widget<RideStatusPage>(find.byType(RideStatusPage))
          .ride
          .id;
      expect(requestedRideId, greaterThan(0));
      final activeRideId = requestedRideId!;
      rideId = activeRideId;
      await _waitForOffer(tester, driverApi, activeRideId);
      _expectSuccess(
        await driverApi.post<Map<String, dynamic>>(
          'api/v1/driver/offers/$activeRideId/accept',
        ),
      );
      final assigned = await _waitForRide(tester, riderApi, activeRideId, {
        'DRIVER_ASSIGNED',
        'DRIVER_ARRIVED',
      });
      expect(assigned.driverId, _driverId);
      expect(find.byType(GoogleMap), findsOneWidget);

      final firstPoint = LatLng(_pickup.latitude - 0.0030, _pickup.longitude);
      final secondPoint = LatLng(_pickup.latitude - 0.0020, _pickup.longitude);
      await _publishGps(driverApi, firstPoint, rideId: activeRideId);
      final firstMarker = await _waitForMarker(tester, firstPoint);
      await _publishGps(driverApi, secondPoint, rideId: activeRideId);
      final secondMarker = await _waitForMarker(tester, secondPoint);
      expect(secondMarker.latitude, greaterThan(firstMarker.latitude));
      expect(find.text('Driver is approaching'), findsOneWidget);

      if ((await _getRide(riderApi, activeRideId)).currentStatus ==
          'DRIVER_ASSIGNED') {
        await _driverAction(driverApi, activeRideId, 'arrive');
      }
      await _waitForRide(tester, riderApi, activeRideId, {'DRIVER_ARRIVED'});
      await waitForWidget(tester, find.text('Your driver has arrived'));
      await _driverAction(driverApi, activeRideId, 'start');
      await _waitForRide(tester, riderApi, activeRideId, {'STARTED'});
      await waitForWidget(tester, find.text('Enjoy your ride'));
      await _driverAction(driverApi, activeRideId, 'complete');
      final completed = await _waitForRide(tester, riderApi, activeRideId, {
        'COMPLETED',
      });
      expect(completed.fareDetail?.totalFare, greaterThan(0));
      await waitForWidget(tester, find.text('Trip receipt'));
      expect(find.text('Ride completed'), findsOneWidget);
      expect(
        find.text('₹${completed.fareDetail!.totalFare!.toStringAsFixed(0)}'),
        findsWidgets,
      );
      await tester.scrollUntilVisible(
        find.text('Rate your driver'),
        180,
        scrollable: find.byType(Scrollable).first,
      );
      final fourthStar = find.byWidgetPredicate((w) => w is IconButton && w.tooltip == '4 stars');
      await waitForWidget(tester, fourthStar);
      await waitForCondition(
        tester,
        () =>
            tester.widget<IconButton>(fourthStar).onPressed !=
            null,
        description: 'rating lookup loaded',
      );
      await tester.ensureVisible(fourthStar);
      await tester.pump(const Duration(milliseconds: 300));
      await tester.tap(fourthStar);
      await tester.pump(const Duration(milliseconds: 300));
      await waitForWidget(tester, find.text('Submit rating'));
      await tester.ensureVisible(find.text('Submit rating'));
      await tester.pump(const Duration(milliseconds: 300));
      await tester.tap(find.text('Submit rating'));
      await waitForWidget(tester, find.text('Your driver rating'));
      final rating = await riderApi.get<Map<String, dynamic>>(
        'api/v1/rating/rides/$activeRideId/mine',
      );
      _expectSuccess(rating);
      expect((rating.data?['data'] as Map?)?['rate'], 4);
    } finally {
      rideId ??= rideBloc.state.ride?.id;
      if (rideId != null) await _cleanup(riderApi, driverApi, rideId);
      if (oldDriverStatus == 'OFFLINE') {
        try {
          await driverApi.put<Map<String, dynamic>>(
            'api/v1/driver/status',
            queryParameters: {'driverStatus': 'OFFLINE'},
          );
        } catch (_) {
          // Preserve the original test failure; fixture cleanup is best effort.
        }
      }
      riderApi.close();
      driverApi.close();
    }
  });
}

Dio _api(String token) => Dio(
  BaseOptions(
    baseUrl: ApiConfig.baseUrl,
    validateStatus: (_) => true,
    headers: {
      'Authorization':
          'Bearer ${token.replaceFirst(RegExp(r'^Bearer\s+'), '')}',
      'Content-Type': 'application/json',
    },
  ),
);

void _expectSuccess(Response<Map<String, dynamic>> response) {
  expect(response.statusCode, inInclusiveRange(200, 299));
  expect(response.data?['success'], isNot(false));
}

Future<void> _publishGps(Dio driverApi, LatLng point, {int? rideId}) async {
  _expectSuccess(
    await driverApi.post<Map<String, dynamic>>(
      'api/v1/location/saveLocation',
      data: {
        'driverId': _driverId,
        'rideId': rideId,
        'latitude': point.latitude,
        'longitude': point.longitude,
      },
    ),
  );
}

Future<void> _waitForOffer(
  WidgetTester tester,
  Dio driverApi,
  int rideId,
) async {
  for (var i = 0; i < 60; i++) {
    final response = await driverApi.get<Map<String, dynamic>>(
      'api/v1/driver/offers',
    );
    if (response.statusCode == 200 && response.data?['data'] is List) {
      final offers = response.data!['data'] as List;
      if (offers.whereType<Map>().any(
        (offer) => offer['rideId']?.toString() == '$rideId',
      )) {
        return;
      }
    }
    await Future<void>.delayed(const Duration(milliseconds: 500));
    await tester.pump();
  }
  fail('The fixture driver did not receive ride $rideId.');
}

Future<void> _waitForAvailableDriver(WidgetTester tester, Dio riderApi) async {
  for (var i = 0; i < 30; i++) {
    final response = await riderApi.get<Map<String, dynamic>>(
      'api/v1/location/nearbyDrivers',
      queryParameters: {
        'latitude': _pickup.latitude,
        'longitude': _pickup.longitude,
        'distance': 5,
        'vehicleType': _vehicleType,
      },
    );
    if (response.statusCode == 200 && response.data?['data'] is List) {
      final drivers = response.data!['data'] as List;
      if (drivers.whereType<Map>().any(
        (driver) => driver['driverId']?.toString() == '$_driverId',
      )) {
        return;
      }
    }
    await Future<void>.delayed(const Duration(milliseconds: 500));
    await tester.pump();
  }
  fail('The fixture driver did not become available near pickup.');
}

Future<RideResponseModel> _getRide(Dio riderApi, int rideId) async {
  final response = await riderApi.get<Map<String, dynamic>>(
    'api/v1/ride/getRideDetail/$rideId',
  );
  _expectSuccess(response);
  final data = response.data?['data'];
  expect(data, isA<Map>());
  return RideResponseModel.fromJson(Map<String, dynamic>.from(data as Map));
}

Future<RideResponseModel> _waitForRide(
  WidgetTester tester,
  Dio riderApi,
  int rideId,
  Set<String> statuses,
) async {
  String? lastStatus;
  for (var i = 0; i < 40; i++) {
    final ride = await _getRide(riderApi, rideId);
    lastStatus = ride.currentStatus;
    if (lastStatus != null && statuses.contains(lastStatus)) return ride;
    await Future<void>.delayed(const Duration(milliseconds: 500));
    await tester.pump();
  }
  fail('Ride $rideId did not reach $statuses (last: $lastStatus).');
}

Future<LatLng> _waitForMarker(WidgetTester tester, LatLng expected) async {
  for (var i = 0; i < 35; i++) {
    await tester.pump(const Duration(milliseconds: 150));
    final maps = find.byType(GoogleMap);
    if (maps.evaluate().isNotEmpty) {
      final marker = tester
          .widget<GoogleMap>(maps.last)
          .markers
          .where((item) => item.markerId == const MarkerId('live-driver'))
          .firstOrNull;
      if (marker != null &&
          (marker.position.latitude - expected.latitude).abs() < 0.00005 &&
          (marker.position.longitude - expected.longitude).abs() < 0.00005) {
        return marker.position;
      }
    }
    await Future<void>.delayed(const Duration(milliseconds: 500));
  }
  fail('The live driver marker did not reach the GPS fix.');
}

Future<void> _driverAction(Dio api, int rideId, String action) async {
  _expectSuccess(
    await api.post<Map<String, dynamic>>(
      'api/v1/ride/rides/$rideId/$action',
      data: {'driverId': _driverId},
    ),
  );
}

Future<void> _cleanup(Dio riderApi, Dio driverApi, int rideId) async {
  try {
    final status = (await _getRide(riderApi, rideId)).currentStatus;
    if (status == 'STARTED') {
      await _driverAction(driverApi, rideId, 'complete');
    } else if (const {
      'REQUESTED',
      'MATCHING',
      'DRIVER_ASSIGNED',
      'DRIVER_ARRIVED',
    }.contains(status)) {
      await riderApi.post<Map<String, dynamic>>(
        'api/v1/ride/rides/$rideId/cancel',
        data: {
          'actorId': _riderId,
          'actorType': 'RIDER',
          'reason': 'QA rider tracking cleanup',
        },
      );
    }
  } catch (_) {
    // Preserve the original assertion failure; fixtures are disposable.
  }
}
