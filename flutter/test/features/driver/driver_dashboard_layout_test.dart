import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/features/driver/data/model/driver_profile_model.dart';
import 'package:swift_app/features/driver/data/model/driver_statistics_model.dart';
import 'package:swift_app/features/driver/data/model/pending_offer_model.dart';
import 'package:swift_app/features/driver/data/model/realtime_ride_event.dart';
import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_active_ride_card.dart';
import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_availability_card.dart';
import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_lifetime_summary.dart';
import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_offer_card.dart';
import 'package:swift_app/themes/app_theme.dart';

Future<void> _pumpNarrowDashboard(
  WidgetTester tester,
  Widget child, {
  Brightness brightness = Brightness.light,
}) async {
  tester.view.physicalSize = const Size(320, 640);
  tester.view.devicePixelRatio = 1;
  tester.view.padding = const FakeViewPadding(top: 47, bottom: 34);
  tester.view.viewPadding = const FakeViewPadding(top: 47, bottom: 34);
  addTearDown(() {
    tester.view.resetPhysicalSize();
    tester.view.resetDevicePixelRatio();
    tester.view.resetPadding();
    tester.view.resetViewPadding();
  });
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.forBrightness(brightness),
      home: Builder(
        builder: (context) => MediaQuery(
          data: MediaQuery.of(context).copyWith(
            textScaler: const TextScaler.linear(2),
            disableAnimations: true,
          ),
          child: Scaffold(
            body: SafeArea(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(16),
                child: child,
              ),
            ),
          ),
        ),
      ),
    ),
  );
  await tester.pump();
}

void main() {
  testWidgets('active trip retains readable status and actions at 2x text', (
    tester,
  ) async {
    var advanced = 0;
    await _pumpNarrowDashboard(
      tester,
      DriverActiveRideCard(
        event: const RealtimeRideEvent(
          rideId: 42,
          rideStatus: 'STARTED',
          estimatedDistance: 12.8,
          estimatedDuration: 1800,
          totalFare: 425,
          currency: 'INR',
        ),
        riderName: 'Ananya Krishnamurthy',
        vehicle: const DriverVehicleModel(vehicleType: 'SEDAN'),
        busy: false,
        onAction: () => advanced++,
        onCancel: () {},
        onChat: () {},
      ),
    );
    expect(tester.takeException(), isNull);
    expect(find.text('In progress'), findsOneWidget);
    await tester.ensureVisible(find.text('Complete ride'));
    await tester.tap(find.text('Complete ride'));
    expect(advanced, 1);
    expect(tester.takeException(), isNull);
  });

  testWidgets('ride request chips and decisions fit a narrow 2x-text screen', (
    tester,
  ) async {
    var accepted = 0;
    await _pumpNarrowDashboard(
      tester,
      DriverOfferCard(
        offer: const PendingOfferModel(
          rideId: 44,
          rideTypeCode: 'PREMIUM_SUV',
          pickupLatitude: 12.97,
          pickupLongitude: 77.59,
          destinationLatitude: 12.93,
          destinationLongitude: 77.62,
          estimatedDistance: 15.8,
          estimatedDuration: 2100,
          distanceKm: 2.4,
          totalFare: 780,
          currency: 'INR',
        ),
        busy: false,
        loadingDetails: false,
        onAccept: () => accepted++,
        onReject: () {},
      ),
      brightness: Brightness.dark,
    );
    expect(tester.takeException(), isNull);
    expect(find.text('2.4 km away'), findsOneWidget);
    await tester.ensureVisible(find.text('Accept ride'));
    await tester.tap(find.text('Accept ride'));
    expect(accepted, 1);
    expect(tester.takeException(), isNull);
  });

  testWidgets('large-text lifetime totals stack with matching left alignment', (
    tester,
  ) async {
    await _pumpNarrowDashboard(
      tester,
      DriverLifetimeSummary(
        statistics: const DriverStatisticsModel(
          completedTrips: 128,
          cancelledTrips: 4,
          lifetimeDistanceKm: 2000,
          lifetimeEarning: 78450,
          averageRating: 4.8,
        ),
        loading: false,
        error: null,
        onRetry: () {},
        onSeeTrips: () {},
      ),
    );
    expect(tester.takeException(), isNull);
    expect(
      tester.getTopLeft(find.text('128')).dx,
      tester.getTopLeft(find.text('₹78450')).dx,
    );
    expect(
      tester.getTopLeft(find.text('₹78450')).dy,
      greaterThan(tester.getBottomLeft(find.text('Completed trips')).dy),
    );
    expect(tester.takeException(), isNull);
  });

  testWidgets('availability switch remains usable with narrow large text', (
    tester,
  ) async {
    bool? changed;
    await _pumpNarrowDashboard(
      tester,
      DriverAvailabilityCard(
        online: false,
        busyWithRide: false,
        loading: false,
        onChanged: (value) => changed = value,
      ),
    );
    expect(tester.takeException(), isNull);
    // The explicit switch keeps its action despite wrapped labels.
    final toggle = find.byType(CupertinoSwitch);
    expect(toggle, findsOneWidget);
    await tester.tap(toggle);
    expect(changed, isTrue);
    expect(tester.takeException(), isNull);
  });
}
