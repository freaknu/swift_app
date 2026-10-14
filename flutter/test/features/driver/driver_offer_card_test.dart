import 'package:swift_app/features/driver/data/model/pending_offer_model.dart';
import 'package:swift_app/features/driver/presentation/pages/driver_home_page.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('countdown repaints its badge without rebuilding trip details', (
    tester,
  ) async {
    final offer = PendingOfferModel(
      rideId: 42,
      totalFare: 425,
      currency: 'INR',
      expiresAt: DateTime.now().add(const Duration(minutes: 1)),
      serverTime: DateTime.now(),
    );
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: DriverOfferCard(
            offer: offer,
            busy: false,
            loadingDetails: false,
            onAccept: () {},
            onReject: () {},
          ),
        ),
      ),
    );
    final fareBefore = tester.widget<Text>(find.text('₹ 425'));
    final badgeBefore = tester.widget<CircularProgressIndicator>(
      find.byType(CircularProgressIndicator),
    );
    await tester.pump(const Duration(seconds: 1));
    expect(
      identical(fareBefore, tester.widget<Text>(find.text('₹ 425'))),
      isTrue,
    );
    expect(
      identical(
        badgeBefore,
        tester.widget<CircularProgressIndicator>(
          find.byType(CircularProgressIndicator),
        ),
      ),
      isFalse,
    );
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox.shrink());
  });

  test(
    'offer deadline follows server time when the device clock is behind',
    () {
      final receivedAt = DateTime.utc(2026, 10, 8, 12);
      final offer = PendingOfferModel.fromJson({
        'rideId': 42,
        'serverTime': '2026-10-08T12:00:16Z',
        'expiresAt': '2026-10-08T12:00:31Z',
      }, receivedAt: receivedAt);

      expect(offer.expiresAt, DateTime.utc(2026, 10, 8, 12, 0, 31));
      expect(offer.localDeadline, receivedAt.add(const Duration(seconds: 15)));
      expect(offer.secondsRemainingAt(receivedAt), 15);
      expect(
        offer.secondsRemainingAt(receivedAt.add(const Duration(seconds: 14))),
        1,
      );
      expect(
        offer.isExpiredAt(receivedAt.add(const Duration(seconds: 15))),
        isTrue,
      );

      final sparseUpdate = PendingOfferModel.fromJson({
        'rideId': 42,
        'distanceKm': 1.2,
      });
      final merged = offer.merge(sparseUpdate);
      expect(merged.localDeadline, offer.localDeadline);
      expect(merged.secondsRemainingAt(receivedAt), 15);
    },
  );

  test(
    'parses enriched matching offer and preserves details on a sparse update',
    () {
      final offer = PendingOfferModel.fromJson({
        'rideId': 42,
        'driverId': 7,
        'pickupLatitude': 12.9716,
        'pickupLongitude': 77.5946,
        'destinationLatitude': 12.9352,
        'destinationLongitude': 77.6245,
        'estimatedDistance': 6.8,
        'estimatedDuration': 1020,
        'totalFare': 425.5,
        'currency': 'INR',
        'distanceKm': 1.4,
        'expiresAt': DateTime.now()
            .add(const Duration(seconds: 15))
            .toUtc()
            .toIso8601String(),
      });

      final merged = offer.merge(
        PendingOfferModel.fromJson({'rideId': '42', 'distanceKm': 1.2}),
      );

      expect(merged.hasTripDetails, isTrue);
      expect(merged.distanceKm, 1.2);
      expect(merged.estimatedDistance, 6.8);
      expect(merged.totalFare, 425.5);
      expect(merged.secondsRemaining, inInclusiveRange(1, 15));
    },
  );

  testWidgets('offer card fits a narrow phone and exposes both decisions', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(320, 740);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });

    var accepted = 0;
    var declined = 0;
    final offer = PendingOfferModel.fromJson({
      'rideId': 42,
      'pickupLatitude': 12.9716,
      'pickupLongitude': 77.5946,
      'destinationLatitude': 12.9352,
      'destinationLongitude': 77.6245,
      'estimatedDistance': 6.8,
      'estimatedDuration': 1020,
      'totalFare': 425.5,
      'currency': 'INR',
      'distanceKm': 1.4,
      'expiresAt': DateTime.now()
          .add(const Duration(seconds: 15))
          .toUtc()
          .toIso8601String(),
    });

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SafeArea(
            child: MediaQuery(
              data: const MediaQueryData(textScaler: TextScaler.linear(1.45)),
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(12),
                child: DriverOfferCard(
                  offer: offer,
                  busy: false,
                  loadingDetails: false,
                  onAccept: () => accepted++,
                  onReject: () => declined++,
                ),
              ),
            ),
          ),
        ),
      ),
    );

    expect(tester.takeException(), isNull);
    expect(find.text('Ride #42'), findsOneWidget);
    expect(find.text('6.8 km'), findsOneWidget);
    expect(find.text('17 min'), findsOneWidget);
    expect(find.text('₹ 426'), findsOneWidget);
    expect(find.text('12.97160, 77.59460'), findsOneWidget);
    expect(find.text('12.93520, 77.62450'), findsOneWidget);

    await tester.ensureVisible(find.text('Accept ride'));
    await tester.tap(find.text('Accept ride'));
    await tester.tap(find.text('Decline'));
    expect(accepted, 1);
    expect(declined, 1);
    expect(tester.takeException(), isNull);
  });

  testWidgets('offer card has a clear fallback while details are unavailable', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(
            child: DriverOfferCard(
              offer: const PendingOfferModel(rideId: 43),
              busy: false,
              loadingDetails: false,
              onAccept: () {},
              onReject: () {},
            ),
          ),
        ),
      ),
    );

    expect(find.text('Pickup details unavailable'), findsOneWidget);
    expect(find.text('Destination details unavailable'), findsOneWidget);
    expect(
      tester.widget<FilledButton>(find.byType(FilledButton)).onPressed,
      isNull,
    );
    expect(tester.takeException(), isNull);
  });
}
