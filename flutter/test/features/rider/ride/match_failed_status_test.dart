import 'dart:async';

import 'package:swift_app/features/driver/data/model/realtime_ride_event.dart';
import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';
import 'package:swift_app/features/rider/ride/domain/repository/ride_repository.dart';
import 'package:swift_app/features/rider/ride/domain/usecases/ride_lifecycle_usecase.dart';
import 'package:swift_app/features/rider/ride/presentation/pages/ride_status_page.dart';
import 'package:swift_app/features/common/shared/widgets/app_loading_page.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

class _RideRepositoryFake implements RideRepository {
  int fetchCount = 0;
  final RideResponse fetchedRide;

  _RideRepositoryFake(this.fetchedRide);

  @override
  Future<RideResponse> getRide(int rideId) async {
    fetchCount++;
    return fetchedRide;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  for (final size in [const Size(320, 568), const Size(568, 320)]) {
    testWidgets('matching controls fit $size with notch and enlarged text', (
      tester,
    ) async {
      tester.view.physicalSize = size;
      tester.view.devicePixelRatio = 1;
      tester.view.padding = const FakeViewPadding(top: 47, bottom: 34);
      tester.view.viewPadding = const FakeViewPadding(top: 47, bottom: 34);
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      addTearDown(tester.view.resetPadding);
      addTearDown(tester.view.resetViewPadding);
      final events = StreamController<Map<String, dynamic>>.broadcast();
      addTearDown(events.close);
      final repository = _RideRepositoryFake(
        const RideResponse(id: 42, currentStatus: 'MATCHING'),
      );
      const mapKey = ValueKey('small-matching-map');
      await tester.pumpWidget(
        MaterialApp(
          builder: (context, child) => MediaQuery(
            data: MediaQuery.of(context).copyWith(
              textScaler: TextScaler.linear(2),
              disableAnimations: true,
            ),
            child: child!,
          ),
          home: RideStatusPage(
            ride: repository.fetchedRide,
            riderId: 9,
            lifecycle: RideLifecycleUsecase(repository),
            realtimeEvents: events.stream,
            mapOverride: const SizedBox.expand(key: mapKey),
          ),
        ),
      );
      await tester.pump();
      expect(tester.takeException(), isNull);
      final mapElement = tester.element(find.byKey(mapKey));
      final toolbar = tester.getRect(find.byType(SwiftAppBar));
      final sheet = tester.getRect(find.byType(GlassSurface).first);
      expect(toolbar.top, greaterThanOrEqualTo(47));
      expect(toolbar.bottom, lessThanOrEqualTo(sheet.top));
      expect(find.text('Finding your driver'), findsOneWidget);
      await tester.ensureVisible(find.text('Cancel request'));
      await tester.pump();
      expect(find.text('Cancel request').hitTestable(), findsOneWidget);
      expect(tester.takeException(), isNull);
      events.add(const {'rideId': 42, 'rideStatus': 'MATCH_FAILED'});
      await tester.pump();
      await tester.pump();
      expect(tester.element(find.byKey(mapKey)), same(mapElement));
      expect(find.byType(AppLoadingPage), findsNothing);
      await tester.ensureVisible(find.text('Try another ride'));
      await tester.pump();
      expect(find.text('Try another ride').hitTestable(), findsOneWidget);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox.shrink());
    });
  }

  test('MATCH_FAILED is a terminal realtime ride status', () {
    final event = RealtimeRideEvent.fromJson(const {
      'rideId': 42,
      'rideStatus': 'MATCH_FAILED',
    });
    expect(event.isFinished, isTrue);
  });

  testWidgets('persisted match failure shows retry and returns to booking', (
    tester,
  ) async {
    final repository = _RideRepositoryFake(
      const RideResponse(id: 42, currentStatus: 'MATCH_FAILED'),
    );
    bool? retry;
    await tester.pumpWidget(
      MaterialApp(
        home: Builder(
          builder: (context) => Scaffold(
            body: TextButton(
              onPressed: () async {
                retry = await Navigator.of(context).push<bool>(
                  MaterialPageRoute(
                    builder: (_) => RideStatusPage(
                      ride: repository.fetchedRide,
                      riderId: 9,
                      lifecycle: RideLifecycleUsecase(repository),
                      mapOverride: const SizedBox.expand(),
                    ),
                  ),
                );
              },
              child: const Text('Open ride'),
            ),
          ),
        ),
      ),
    );
    await tester.tap(find.text('Open ride'));
    await tester.pumpAndSettle();

    expect(find.text('No drivers available'), findsOneWidget);
    expect(
      find.text('No drivers are available right now. Please try again.'),
      findsOneWidget,
    );
    expect(find.text('Try another ride'), findsOneWidget);
    expect(find.text('Searching within'), findsNothing);
    expect(repository.fetchCount, 0);

    await tester.tap(find.text('Try another ride'));
    await tester.pumpAndSettle();
    expect(retry, isTrue);
  });

  testWidgets('realtime match failure ends search and stops polling', (
    tester,
  ) async {
    final events = StreamController<Map<String, dynamic>>.broadcast();
    addTearDown(events.close);
    final repository = _RideRepositoryFake(
      const RideResponse(id: 42, currentStatus: 'MATCH_FAILED'),
    );
    await tester.pumpWidget(
      MaterialApp(
        home: RideStatusPage(
          ride: const RideResponse(id: 42, currentStatus: 'MATCHING'),
          riderId: 9,
          lifecycle: RideLifecycleUsecase(repository),
          realtimeEvents: events.stream,
          mapOverride: const SizedBox.expand(),
        ),
      ),
    );
    expect(find.byType(AppLoadingPage), findsNothing);
    expect(find.text('Cancel request'), findsOneWidget);

    events.add(const {
      'rideId': 42,
      'rideStatus': 'SEARCHING',
      'radiusKm': 2.0,
      'attempt': 1,
      'driverCount': 0,
    });
    await tester.pump();
    expect(find.textContaining('Searching within'), findsOneWidget);

    events.add(const {
      'rideId': 42,
      'rideStatus': 'MATCH_FAILED',
      'reason': 'NO_AVAILABLE_DRIVER',
      'message': 'No drivers are available right now. Please try again.',
    });
    await tester.pump();
    await tester.pump();
    expect(find.text('No drivers available'), findsOneWidget);
    expect(find.byType(AppLoadingPage), findsNothing);
    expect(find.textContaining('Searching within'), findsNothing);
    expect(repository.fetchCount, 1);

    await tester.pump(const Duration(seconds: 6));
    expect(repository.fetchCount, 1);
  });

  testWidgets('a stale REST search cannot undo a terminal socket failure', (
    tester,
  ) async {
    final events = StreamController<Map<String, dynamic>>.broadcast();
    addTearDown(events.close);
    final repository = _RideRepositoryFake(
      const RideResponse(id: 42, currentStatus: 'MATCHING'),
    );
    await tester.pumpWidget(
      MaterialApp(
        home: RideStatusPage(
          ride: const RideResponse(id: 42, currentStatus: 'MATCHING'),
          riderId: 9,
          lifecycle: RideLifecycleUsecase(repository),
          realtimeEvents: events.stream,
          mapOverride: const SizedBox.expand(),
        ),
      ),
    );

    events.add(const {'rideId': '42', 'rideStatus': 'MATCH_FAILED'});
    await tester.pump();
    await tester.pump();

    expect(repository.fetchCount, 1);
    expect(find.text('No drivers available'), findsOneWidget);
    expect(find.text('Finding your driver'), findsNothing);
    await tester.pump(const Duration(seconds: 6));
    expect(repository.fetchCount, 1);
  });

  testWidgets('terminal completion updates immediately from socket event', (
    tester,
  ) async {
    final events = StreamController<Map<String, dynamic>>.broadcast();
    addTearDown(events.close);
    final repository = _RideRepositoryFake(
      const RideResponse(id: 42, currentStatus: 'STARTED'),
    );
    await tester.pumpWidget(
      MaterialApp(
        home: RideStatusPage(
          ride: const RideResponse(id: 42, currentStatus: 'STARTED'),
          riderId: 9,
          lifecycle: RideLifecycleUsecase(repository),
          realtimeEvents: events.stream,
          mapOverride: const SizedBox.expand(),
        ),
      ),
    );

    events.add(const {'rideId': 42, 'rideStatus': 'COMPLETED'});
    await tester.pump();
    await tester.pump();
    expect(find.text('Ride completed'), findsOneWidget);
    expect(find.byType(AppLoadingPage), findsNothing);
    expect(repository.fetchCount, 1);
    await tester.pump(const Duration(seconds: 6));
    expect(repository.fetchCount, 1);
  });

  testWidgets('resuming the app refreshes an active ride immediately', (
    tester,
  ) async {
    final events = StreamController<Map<String, dynamic>>.broadcast();
    addTearDown(events.close);
    final repository = _RideRepositoryFake(
      const RideResponse(id: 42, currentStatus: 'DRIVER_ASSIGNED'),
    );
    await tester.pumpWidget(
      MaterialApp(
        home: RideStatusPage(
          ride: const RideResponse(id: 42, currentStatus: 'MATCHING'),
          riderId: 9,
          lifecycle: RideLifecycleUsecase(repository),
          realtimeEvents: events.stream,
          mapOverride: const SizedBox.expand(),
        ),
      ),
    );
    expect(repository.fetchCount, 0);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();
    expect(repository.fetchCount, 1);
    expect(find.text('Driver is on the way'), findsOneWidget);
  });

  testWidgets('fallback polling pauses in background and resumes immediately', (
    tester,
  ) async {
    final repository = _RideRepositoryFake(
      const RideResponse(id: 42, currentStatus: 'MATCHING'),
    );
    await tester.pumpWidget(
      MaterialApp(
        home: RideStatusPage(
          ride: repository.fetchedRide,
          riderId: 9,
          lifecycle: RideLifecycleUsecase(repository),
          mapOverride: const SizedBox.expand(),
        ),
      ),
    );
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    await tester.pump(const Duration(seconds: 16));
    expect(repository.fetchCount, 0);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();
    expect(repository.fetchCount, 1);
    await tester.pump(const Duration(seconds: 5));
    expect(repository.fetchCount, 2);
    await tester.pumpWidget(const SizedBox.shrink());
  });

  testWidgets('driver assignment reveals the map before a REST reply arrives', (
    tester,
  ) async {
    final events = StreamController<Map<String, dynamic>>.broadcast();
    addTearDown(events.close);
    final repository = _DeferredRideRepository();
    const mapKey = ValueKey('live-map');
    await tester.pumpWidget(
      MaterialApp(
        home: RideStatusPage(
          ride: const RideResponse(id: 42, currentStatus: 'MATCHING'),
          riderId: 9,
          lifecycle: RideLifecycleUsecase(repository),
          realtimeEvents: events.stream,
          mapOverride: const SizedBox.expand(key: mapKey),
        ),
      ),
    );
    expect(find.byType(AppLoadingPage), findsNothing);
    expect(find.byKey(mapKey), findsOneWidget);

    events.add(const {'rideId': 42, 'rideStatus': 'DRIVER_ASSIGNED'});
    await tester.pump();
    expect(repository.fetchCount, 1);
    expect(find.byType(AppLoadingPage), findsNothing);
    expect(find.byKey(mapKey), findsOneWidget);
    expect(find.text('Driver is on the way'), findsOneWidget);
    repository.response.complete(
      const RideResponse(id: 42, currentStatus: 'DRIVER_ASSIGNED'),
    );
    await tester.pump();
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox.shrink());
  });
}

class _DeferredRideRepository extends _RideRepositoryFake {
  _DeferredRideRepository()
    : super(const RideResponse(id: 42, currentStatus: 'DRIVER_ASSIGNED'));

  final response = Completer<RideResponse>();
  @override
  Future<RideResponse> getRide(int rideId) {
    fetchCount++;
    return response.future;
  }
}
