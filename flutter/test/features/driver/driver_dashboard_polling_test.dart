import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/driver/data/datasource/driver_realtime_service.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/driver/data/model/pending_offer_model.dart';
import 'package:swift_app/features/driver/data/model/realtime_ride_event.dart';
import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_active_ride_feed.dart';
import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_offer_inbox.dart';

ApiResponseFormat<T> _ok<T>(T data) => ApiResponseFormat.success(
  data: data,
  statusCode: 200,
  message: 'OK',
  endpoint: 'fixture',
);

class _Datasource implements DriverRemoteDatasource {
  int inboxCalls = 0;
  int activeRideCalls = 0;
  Completer<ApiResponseFormat<List<PendingOfferModel>>>? pendingInbox;
  Completer<ApiResponseFormat<int?>>? pendingActiveRide;

  @override
  Future<ApiResponseFormat<List<PendingOfferModel>>> getPendingOffers() async {
    inboxCalls++;
    return pendingInbox == null ? _ok(const []) : pendingInbox!.future;
  }

  @override
  Future<ApiResponseFormat<int?>> getActiveRideId() async {
    activeRideCalls++;
    return pendingActiveRide == null
        ? _ok<int?>(null)
        : pendingActiveRide!.future;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _Realtime implements DriverRealtimeService {
  final events = StreamController<RealtimeRideEvent>.broadcast();

  @override
  Stream<RealtimeRideEvent> get rideEvents => events.stream;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _Fixture {
  final datasource = _Datasource();
  final realtime = _Realtime();
  final offers = DriverOfferInboxActions();
  final rides = DriverActiveRideActions();
  final newOffers = <int>[];
  int offersSettled = 0;
  int rideSettled = 0;

  Widget get page => MaterialApp(
    home: Scaffold(
      // A retained Drive tab remains hidden while Trips/Account is visible.
      // That must not disable its REST recovery or its realtime subscription.
      body: TickerMode(
        enabled: false,
        child: CustomScrollView(
          slivers: [
            DriverOfferInbox(
              actions: offers,
              firstOfferCardKey: GlobalKey(),
              datasource: datasource,
              realtimeService: realtime,
              driverId: 7,
              online: true,
              onAccepted: () {},
              onNewOffer: newOffers.add,
              onInitialLoadSettled: () => offersSettled++,
            ),
            DriverActiveRideFeed(
              actions: rides,
              datasource: datasource,
              realtimeService: realtime,
              driverId: 7,
              busyWithRide: false,
              vehicle: null,
              onAction: (_) async {},
              onCancel: (_) async {},
              onChat: (_) async {},
              onActiveRideChanged: (_) {},
              onInitialLoadSettled: () => rideSettled++,
            ),
          ],
        ),
      ),
    ),
  );
}

Future<void> _pumpFixture(WidgetTester tester, _Fixture fixture) async {
  addTearDown(() async {
    await tester.pumpWidget(const SizedBox.shrink());
    await fixture.realtime.events.close();
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
  });
  await tester.pumpWidget(fixture.page);
  await tester.pump();
  await tester.pump();
}

void main() {
  testWidgets(
    'background stops recovery polls; resume immediately reconciles',
    (tester) async {
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      final fixture = _Fixture();
      await _pumpFixture(tester, fixture);
      expect(fixture.datasource.inboxCalls, 1);
      expect(fixture.datasource.activeRideCalls, 1);

      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      await tester.pump(const Duration(seconds: 60));
      await fixture.offers.refresh();
      await fixture.rides.refresh();
      expect(fixture.datasource.inboxCalls, 1);
      expect(fixture.datasource.activeRideCalls, 1);

      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await tester.pump();
      expect(fixture.datasource.inboxCalls, 2);
      expect(fixture.datasource.activeRideCalls, 2);
      await tester.pump(const Duration(seconds: 15));
      await tester.pump();
      expect(fixture.datasource.inboxCalls, greaterThan(2));
      expect(fixture.datasource.activeRideCalls, greaterThan(2));
      expect(fixture.offersSettled, 1);
      expect(fixture.rideSettled, 1);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('components mounted in background wait for foreground recovery', (
    tester,
  ) async {
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    final fixture = _Fixture();
    await _pumpFixture(tester, fixture);
    await tester.pump(const Duration(seconds: 30));
    expect(fixture.datasource.inboxCalls, 0);
    expect(fixture.datasource.activeRideCalls, 0);
    expect(fixture.offersSettled, 0);
    expect(fixture.rideSettled, 0);

    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();
    await tester.pump();
    expect(fixture.datasource.inboxCalls, 1);
    expect(fixture.datasource.activeRideCalls, 1);
    expect(fixture.offersSettled, 1);
    expect(fixture.rideSettled, 1);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'retained hidden tab still recovers and receives realtime offers',
    (tester) async {
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      final fixture = _Fixture();
      await _pumpFixture(tester, fixture);
      await tester.pump(const Duration(seconds: 15));
      await tester.pump();
      expect(fixture.datasource.inboxCalls, greaterThan(1));
      expect(fixture.datasource.activeRideCalls, greaterThan(1));

      fixture.realtime.events.add(
        RealtimeRideEvent.fromJson({
          'rideStatus': 'OFFER_SENT',
          'rideId': 42,
          'driverId': 7,
          'expiresAt': DateTime.now()
              .add(const Duration(minutes: 2))
              .toIso8601String(),
        }),
      );
      await tester.pump();
      expect(fixture.newOffers, [42]);
      expect(fixture.offers.hasOffer(42), isTrue);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('resume during an in-flight lookup queues fresh reconciliation', (
    tester,
  ) async {
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    final fixture = _Fixture();
    fixture.datasource.pendingInbox = Completer();
    fixture.datasource.pendingActiveRide = Completer();
    await _pumpFixture(tester, fixture);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    await tester.pump(const Duration(seconds: 30));
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    fixture.datasource.pendingInbox!.complete(_ok(const []));
    fixture.datasource.pendingActiveRide!.complete(_ok<int?>(null));
    await tester.pump();
    await tester.pump();

    expect(fixture.datasource.inboxCalls, 2);
    expect(fixture.datasource.activeRideCalls, 2);
    expect(fixture.offersSettled, 1);
    expect(fixture.rideSettled, 1);
    expect(tester.takeException(), isNull);
  });
}
