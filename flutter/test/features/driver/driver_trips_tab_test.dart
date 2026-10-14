import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/driver/data/model/driver_journey_model.dart';
import 'package:swift_app/features/driver/data/model/driver_rating_model.dart';
import 'package:swift_app/features/driver/data/model/driver_statistics_model.dart';
import 'package:swift_app/features/driver/presentation/pages/driver_trips_tab.dart';

class _TripsDatasource extends DriverRemoteDatasource {
  _TripsDatasource({
    required super.dioClient,
    required super.endpoints,
    required super.authLocalDatasource,
  });

  final requestedStatuses = <String?>[];
  final requestedRatingDrivers = <int>[];
  bool failHistory = false;
  int rideCount = 0;

  @override
  Future<ApiResponseFormat<DriverJourneyPage>> getJourneys({
    int page = 0,
    int size = 20,
    String? status,
  }) async {
    requestedStatuses.add(status);
    if (failHistory) {
      return ApiResponseFormat.failure(
        statusCode: 503,
        message: 'History service unavailable',
        endpoint: 'fixture',
      );
    }
    final fixtures = rideCount > 0
        ? List.generate(
            rideCount,
            (index) => DriverJourney(
              id: index + 1,
              status: 'COMPLETED',
              fare: 250,
              currency: 'INR',
            ),
          )
        : [
            const DriverJourney(
              id: 11,
              status: 'COMPLETED',
              fare: 250,
              currency: 'INR',
            ),
            const DriverJourney(id: 12, status: 'CANCELLED'),
          ];
    final rides = fixtures
        .where((ride) => status == null || ride.status == status)
        .toList();
    return ApiResponseFormat.success(
      data: DriverJourneyPage(
        content: rides,
        page: page,
        totalPages: 1,
        totalElements: rides.length,
        last: true,
      ),
      statusCode: 200,
      message: 'OK',
      endpoint: 'fixture',
    );
  }

  @override
  Future<ApiResponseFormat<DriverRatingModel>> getRating(int driverId) async {
    requestedRatingDrivers.add(driverId);
    return ApiResponseFormat.failure(
      statusCode: 404,
      message: 'Rating was not found',
      endpoint: 'fixture',
    );
  }
}

void main() {
  testWidgets('long trip history mounts visible cards as it scrolls', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(430, 850);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });
    SharedPreferences.setMockInitialValues({'auth.userId': 7});
    final preferences = await SharedPreferences.getInstance();
    final datasource = _TripsDatasource(
      dioClient: DioClient(),
      endpoints: RiderApiEndpoints(),
      authLocalDatasource: AuthLocalDatasource(preferences: preferences),
    )..rideCount = 100;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: DriverTripsTab(
            datasource: datasource,
            driverId: 8,
            statistics: const DriverStatisticsModel(
              completedTrips: 100,
              cancelledTrips: 0,
              lifetimeDistanceKm: 800,
              lifetimeEarning: 25000,
              averageRating: 0,
            ),
            onRefreshStatistics: () async {},
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Ride #1'), findsOneWidget);
    expect(find.text('Ride #100'), findsNothing);
    expect(
      find
          .byWidgetPredicate(
            (widget) =>
                widget is Text && widget.data?.startsWith('Ride #') == true,
          )
          .evaluate()
          .length,
      lessThan(12),
    );
    await tester.scrollUntilVisible(
      find.text('Ride #100'),
      350,
      maxScrolls: 80,
    );
    expect(find.text('Ride #100'), findsOneWidget);
    expect(find.text('Ride #1'), findsNothing);
    expect(datasource.requestedStatuses, [null]);
    expect(tester.takeException(), isNull);
  });

  testWidgets('profile replacement refreshes after the parent build once', (
    tester,
  ) async {
    SharedPreferences.setMockInitialValues({'auth.userId': 7});
    final preferences = await SharedPreferences.getInstance();
    final datasource = _TripsDatasource(
      dioClient: DioClient(),
      endpoints: RiderApiEndpoints(),
      authLocalDatasource: AuthLocalDatasource(preferences: preferences),
    );
    int? driverId;
    var summaryRefreshes = 0;

    await tester.pumpWidget(
      MaterialApp(
        home: StatefulBuilder(
          builder: (context, updateParent) => Scaffold(
            body: Column(
              children: [
                TextButton(
                  onPressed: () => updateParent(() => driverId = 8),
                  child: const Text('Driver 8'),
                ),
                TextButton(
                  onPressed: () => updateParent(() => driverId = 9),
                  child: const Text('Driver 9'),
                ),
                TextButton(
                  onPressed: () => updateParent(() {}),
                  child: const Text('Rebuild parent'),
                ),
                Expanded(
                  child: DriverTripsTab(
                    datasource: datasource,
                    driverId: driverId,
                    statistics: const DriverStatisticsModel(
                      completedTrips: 0,
                      cancelledTrips: 0,
                      lifetimeDistanceKm: 0,
                      lifetimeEarning: 0,
                      averageRating: 0,
                    ),
                    onRefreshStatistics: () async {
                      updateParent(() => summaryRefreshes++);
                    },
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
    await tester.pump();
    expect(summaryRefreshes, 0);

    await tester.tap(find.text('Driver 8'));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    expect(summaryRefreshes, 1);
    expect(datasource.requestedRatingDrivers, [8]);

    await tester.tap(find.text('Rebuild parent'));
    await tester.pumpAndSettle();
    expect(summaryRefreshes, 1);

    await tester.tap(find.text('Driver 9'));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    expect(summaryRefreshes, 2);
    expect(datasource.requestedRatingDrivers, [8, 9]);
  });

  testWidgets('trip filters request server pages and do not invent earnings', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(430, 1100);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });
    SharedPreferences.setMockInitialValues({'auth.userId': 7});
    final preferences = await SharedPreferences.getInstance();
    final datasource = _TripsDatasource(
      dioClient: DioClient(),
      endpoints: RiderApiEndpoints(),
      authLocalDatasource: AuthLocalDatasource(preferences: preferences),
    );
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: DriverTripsTab(
            datasource: datasource,
            driverId: 8,
            statistics: const DriverStatisticsModel(
              completedTrips: 1,
              cancelledTrips: 1,
              lifetimeDistanceKm: 8.5,
              lifetimeEarning: 250,
              averageRating: 0,
            ),
            onRefreshStatistics: () async {},
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(datasource.requestedStatuses, [null]);
    expect(find.text('Ride #11'), findsOneWidget);
    expect(find.text('Ride #12'), findsOneWidget);
    expect(find.text('₹250'), findsWidgets);
    expect(find.text('No ratings yet'), findsOneWidget);

    await tester.tap(find.text('Completed').first);
    await tester.pumpAndSettle();
    expect(datasource.requestedStatuses.last, 'COMPLETED');
    expect(find.text('Ride #11'), findsOneWidget);
    expect(find.text('Ride #12'), findsNothing);

    await tester.tap(find.text('Cancelled').first);
    await tester.pumpAndSettle();
    expect(datasource.requestedStatuses.last, 'CANCELLED');
    expect(find.text('Ride #11'), findsNothing);
    expect(find.text('Ride #12'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('history error shows retry without overflow at large text', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(320, 700);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });
    SharedPreferences.setMockInitialValues({'auth.userId': 7});
    final preferences = await SharedPreferences.getInstance();
    final datasource = _TripsDatasource(
      dioClient: DioClient(),
      endpoints: RiderApiEndpoints(),
      authLocalDatasource: AuthLocalDatasource(preferences: preferences),
    )..failHistory = true;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: MediaQuery(
            data: const MediaQueryData(textScaler: TextScaler.linear(1.5)),
            child: DriverTripsTab(
              datasource: datasource,
              driverId: 8,
              statistics: const DriverStatisticsModel(
                completedTrips: 0,
                cancelledTrips: 0,
                lifetimeDistanceKm: 0,
                lifetimeEarning: 0,
                averageRating: 0,
              ),
              onRefreshStatistics: () async {},
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.text('Could not load rides'),
      250,
      scrollable: find.byType(Scrollable).first,
    );
    expect(find.text('Could not load rides'), findsOneWidget);
    expect(find.text('History service unavailable'), findsOneWidget);
    expect(find.text('Retry'), findsWidgets);
    expect(tester.takeException(), isNull);
  });
}
