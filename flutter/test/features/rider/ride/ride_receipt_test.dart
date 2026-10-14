import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/features/rider/ride/data/datasource/rider_ride_extras_datasource.dart';
import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';
import 'package:swift_app/features/rider/ride/domain/repository/ride_repository.dart';
import 'package:swift_app/features/rider/ride/domain/usecases/ride_lifecycle_usecase.dart';
import 'package:swift_app/features/rider/ride/presentation/pages/ride_status_page.dart';

class _CompletedRideRepository implements RideRepository {
  final RideResponse ride;

  _CompletedRideRepository(this.ride);

  @override
  Future<RideResponse> getRide(int rideId) async => ride;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeExtras extends RiderRideExtrasDatasource {
  _FakeExtras() : super(Dio());

  int? submittedRideId;
  int? submittedDriverId;
  int? submittedStars;
  MyRideRating? saved;

  @override
  Future<AssignedDriverSummary?> driverSummary(int rideId) async =>
      const AssignedDriverSummary(
        driverId: 5,
        driverName: 'Asha Driver',
        brand: 'Maruti',
        model: 'Dzire',
        registrationNumber: 'KA01AB1234',
      );

  @override
  Future<MyRideRating?> myRating(int rideId) async => saved;

  @override
  Future<MyRideRating?> rateDriver({
    required int rideId,
    required int driverId,
    required int rate,
  }) async {
    submittedRideId = rideId;
    submittedDriverId = driverId;
    submittedStars = rate;
    return saved = MyRideRating(id: 9, rate: rate);
  }
}

void main() {
  test('rating request uses assigned driver and owned ride lookup', () async {
    final requests = <RequestOptions>[];
    final dio = Dio(BaseOptions(baseUrl: 'http://localhost/'));
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (request, handler) {
          requests.add(request);
          final responseData = request.method == 'POST'
              ? {
                  'id': 5,
                  'allRatings': [
                    {'id': 9, 'rideId': 17, 'rate': 4},
                  ],
                }
              : {'id': 9, 'rideId': 17, 'rate': 4};
          handler.resolve(
            Response(
              requestOptions: request,
              statusCode: 200,
              data: {'data': responseData},
            ),
          );
        },
      ),
    );
    final datasource = RiderRideExtrasDatasource(dio);
    final rating = await datasource.rateDriver(
      rideId: 17,
      driverId: 5,
      rate: 4,
    );
    expect(rating?.rate, 4);
    expect((await datasource.myRating(17))?.id, 9);
    expect(requests.map((request) => request.path), [
      'api/v1/rating/createRating',
      'api/v1/rating/rides/17/mine',
    ]);
    expect(requests.first.data, {
      'rideId': 17,
      'driverId': 5,
      'userType': 'DRIVER',
      'rate': 4,
    });
  });

  testWidgets('completed ride shows real receipt and saves driver rating', (
    tester,
  ) async {
    const ride = RideResponse(
      id: 17,
      riderId: 3,
      driverId: 5,
      currentStatus: 'COMPLETED',
      actualDistance: 8.4,
      // RiderService stores actualDuration in seconds.
      actualDuration: 120,
      fareDetail: FareDetail(totalFare: 248, currency: 'INR'),
    );
    final extras = _FakeExtras();
    await tester.pumpWidget(
      MaterialApp(
        home: RideStatusPage(
          ride: ride,
          riderId: 3,
          lifecycle: RideLifecycleUsecase(_CompletedRideRepository(ride)),
          extrasDatasource: extras,
          mapOverride: const SizedBox.expand(),
        ),
      ),
    );
    await tester.pump();

    expect(find.text('Trip receipt'), findsOneWidget);
    expect(find.text('₹248'), findsWidgets);
    expect(find.text('8.4 km'), findsOneWidget);
    expect(find.text('2 min'), findsOneWidget);
    expect(find.text('Asha Driver'), findsOneWidget);
    await tester.scrollUntilVisible(
      find.text('Rate your driver'),
      180,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.tap(find.byTooltip('4 stars'));
    await tester.pump();
    await tester.ensureVisible(find.text('Submit rating'));
    await tester.tap(find.text('Submit rating'));
    await tester.pump();
    expect(extras.submittedRideId, 17);
    expect(extras.submittedDriverId, 5);
    expect(extras.submittedStars, 4);
    expect(find.text('Your driver rating'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
