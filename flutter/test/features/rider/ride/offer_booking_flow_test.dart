import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_maps_flutter_platform_interface/google_maps_flutter_platform_interface.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/common/fare/data/model/fare_estimate_request_model.dart';
import 'package:swift_app/features/common/fare/domain/entity/fare.dart';
import 'package:swift_app/features/common/fare/domain/entity/fare_estimate_response.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_bloc.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_event.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_state.dart';
import 'package:swift_app/features/rider/home/data/model/ride_type_model.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_request.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_response.dart';
import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_state.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_state.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_state.dart';
import 'package:swift_app/features/rider/home/presentation/models/location_selection.dart';
import 'package:swift_app/features/rider/home/presentation/pages/trip_route_map_page.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/offer_card_widget.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/offer_section_widget.dart';
import 'package:swift_app/features/rider/ride/presentation/blocs/ride_bloc.dart';
import 'package:swift_app/features/rider/ride/presentation/blocs/ride_event.dart';
import 'package:swift_app/features/rider/ride/presentation/blocs/ride_state.dart';
import 'package:swift_app/features/rider/ride/presentation/pages/ride_options_page.dart';

void main() {
  late GoogleMapsFlutterPlatform originalMaps;
  setUp(() {
    originalMaps = GoogleMapsFlutterPlatform.instance;
    GoogleMapsFlutterPlatform.instance = _Maps();
  });
  tearDown(() => GoogleMapsFlutterPlatform.instance = originalMaps);

  testWidgets(
    'map flow opens the offers review and preserves selected vehicle',
    (tester) async {
      final fixture = await _setup(tester);
      await tester.pumpWidget(
        fixture.app(
          TripRouteMapPage(trip: _trip, preferredRideTypeCode: 'AUTO_RICKSHAW'),
        ),
      );
      await _frames(tester);
      expect(find.text('Review fare & offers'), findsOneWidget);
      await tester.tap(find.text('Review fare & offers'));
      await _frames(tester);

      expect(find.byType(RideOptionsPage), findsOneWidget);
      expect(fixture.fare.requests.single.rideType, 'AUTO_RICKSHAW');
      expect(fixture.offers.state.lastRequest!.rideTypeCode, 'AUTO_RICKSHAW');
      expect(fixture.ride.events, isEmpty);
      Navigator.of(tester.element(find.byType(RideOptionsPage))).pop();
      await tester.pumpAndSettle();
      expect(find.byType(TripRouteMapPage), findsOneWidget);
      expect(find.byType(RideOptionsPage), findsNothing);
      expect(fixture.ride.events, isEmpty);
    },
  );

  testWidgets(
    'promotion must have a matching confirmed discount before booking',
    (tester) async {
      final fixture = await _setup(tester);
      await tester.pumpWidget(fixture.app(RideOptionsPage(trip: _trip)));
      await _frames(tester);
      final originalQuote = fixture.fare.requests.single;
      fixture.fare.complete(originalQuote);
      await _frames(tester);
      await tester.scrollUntilVisible(
        find.text('Save on your ride'),
        150,
        scrollable: find
            .descendant(
              of: find.byType(CustomScrollView),
              matching: find.byType(Scrollable),
            )
            .first,
      );
      await _frames(tester);
      await tester.tap(find.text('Save on your ride'));
      await _frames(tester);
      final promotedQuote = fixture.fare.requests.last;
      expect(promotedQuote.offerId, 20);
      expect(find.text('Calculating fare…'), findsOneWidget);
      expect(find.text('SWIFT20 applied'), findsNothing);

      // A stale success must not enable booking before the offer quote arrives.
      fixture.fare.complete(originalQuote);
      await _frames(tester);
      expect(find.text('Calculating fare…'), findsOneWidget);
      expect(fixture.ride.events, isEmpty);

      fixture.fare.complete(promotedQuote, discount: 20);
      await _frames(tester);
      expect(find.text('SWIFT20 applied'), findsOneWidget);
      expect(find.text('You save ₹20.00'), findsOneWidget);
      await tester.tap(find.text('Request Bike'));
      await tester.pump();
      final request = fixture.ride.events.single as BookRide;
      expect(request.request.offerId, 20);
      expect(request.request.route!.offerId, 20);
      expect(request.request.rideType, 'BIKE');
    },
  );

  testWidgets(
    'changing vehicle clears its promotion and requires a new matching quote',
    (tester) async {
      final fixture = await _setup(tester);
      await tester.pumpWidget(fixture.app(RideOptionsPage(trip: _trip)));
      await _frames(tester);
      fixture.fare.complete(fixture.fare.requests.single);
      await _frames(tester);
      await tester.scrollUntilVisible(
        find.text('Save on your ride'),
        150,
        scrollable: find
            .descendant(
              of: find.byType(CustomScrollView),
              matching: find.byType(Scrollable),
            )
            .first,
      );
      await _frames(tester);
      await tester.tap(find.text('Save on your ride'));
      await _frames(tester);
      final promotedBike = fixture.fare.requests.last;
      fixture.fare.complete(promotedBike, discount: 20);
      await _frames(tester);
      await tester.ensureVisible(find.text('Auto Rickshaw'));
      await _frames(tester);
      await tester.tap(find.text('Auto Rickshaw'));
      await _frames(tester);
      final autoQuote = fixture.fare.requests.last;
      expect(autoQuote.rideType, 'AUTO_RICKSHAW');
      expect(autoQuote.offerId, isNull);
      expect(find.text('SWIFT20 applied'), findsNothing);
      fixture.fare.complete(promotedBike, discount: 20);
      await _frames(tester);
      expect(find.text('Calculating fare…'), findsOneWidget);
      expect(fixture.ride.events, isEmpty);
      fixture.fare.complete(autoQuote);
      await _frames(tester);
      await tester.tap(find.text('Request Auto Rickshaw'));
      await tester.pump();
      final request = fixture.ride.events.single as BookRide;
      expect(request.request.rideType, 'AUTO_RICKSHAW');
      expect(request.request.offerId, isNull);
    },
  );

  testWidgets(
    'unavailable promotion can be removed and base fare is re-quoted',
    (tester) async {
      final fixture = await _setup(tester);
      await tester.pumpWidget(fixture.app(RideOptionsPage(trip: _trip)));
      await _frames(tester);
      fixture.fare.complete(fixture.fare.requests.single);
      await _frames(tester);
      await tester.scrollUntilVisible(
        find.text('Save on your ride'),
        150,
        scrollable: find
            .descendant(
              of: find.byType(CustomScrollView),
              matching: find.byType(Scrollable),
            )
            .first,
      );
      await _frames(tester);
      await tester.tap(find.text('Save on your ride'));
      await _frames(tester);
      fixture.fare.emit(
        FareState(
          status: FareStatus.failure,
          lastRequest: fixture.fare.requests.last,
          message: 'Minimum fare not met',
        ),
      );
      await _frames(tester);
      await tester.ensureVisible(find.text('Remove'));
      expect(
        find.text(
          'This offer could not be applied. Remove it or choose another.',
        ),
        findsOneWidget,
      );
      await tester.tap(find.text('Remove'));
      await _frames(tester);
      expect(fixture.fare.requests.last.offerId, isNull);
      expect(find.text('SWIFT20 selected'), findsNothing);
      fixture.fare.complete(fixture.fare.requests.last);
      await _frames(tester);
      await tester.tap(find.text('Request Bike'));
      await tester.pump();
      final request = fixture.ride.events.single as BookRide;
      expect(request.request.offerId, isNull);
      expect(request.request.route!.offerId, isNull);
    },
  );

  testWidgets('zero-discount offer response cannot silently charge full fare', (
    tester,
  ) async {
    final fixture = await _setup(tester);
    await tester.pumpWidget(fixture.app(RideOptionsPage(trip: _trip)));
    await _frames(tester);
    fixture.fare.complete(fixture.fare.requests.single);
    await _frames(tester);
    await tester.scrollUntilVisible(
      find.text('Save on your ride'),
      150,
      scrollable: find
          .descendant(
            of: find.byType(CustomScrollView),
            matching: find.byType(Scrollable),
          )
          .first,
    );
    await _frames(tester);
    await tester.tap(find.text('Save on your ride'));
    await _frames(tester);
    fixture.fare.complete(fixture.fare.requests.last);
    await _frames(tester);
    expect(find.text('Remove unavailable offer'), findsOneWidget);
    expect(find.text('SWIFT20 applied'), findsNothing);
    final button = tester.widget<FilledButton>(
      find.widgetWithText(FilledButton, 'Remove unavailable offer'),
    );
    expect(button.onPressed, isNull);
    expect(fixture.ride.events, isEmpty);
  });

  testWidgets(
    'offers list hides future, expired, exhausted and other vehicle offers',
    (tester) async {
      final now = DateTime.now().toUtc();
      final offers = _Offers()
        ..emit(
          OfferState(
            status: OfferStatus.success,
            lastRequest: OfferRequest(rideTypeCode: 'BIKE'),
            offers: [
              OfferResponse(id: 1, title: 'Generic offer', active: true),
              OfferResponse(
                id: 2,
                title: 'Bike offer',
                rideTypeCode: 'BIKE',
                active: true,
              ),
              OfferResponse(
                id: 3,
                title: 'Future offer',
                validFrom: now.add(const Duration(days: 1)),
              ),
              OfferResponse(
                id: 4,
                title: 'Expired offer',
                validUntil: now.subtract(const Duration(days: 1)),
              ),
              OfferResponse(
                id: 5,
                title: 'Used offer',
                usageLimit: 3,
                usedCount: 3,
              ),
              OfferResponse(
                id: 6,
                title: 'Other vehicle',
                rideTypeCode: 'HATCHBACK',
              ),
              OfferResponse(id: 7, title: 'Inactive offer', active: false),
            ],
          ),
        );
      addTearDown(offers.close);
      await tester.pumpWidget(
        BlocProvider<OfferBloc>.value(
          value: offers,
          child: const MaterialApp(
            home: Scaffold(
              body: SingleChildScrollView(child: OfferSection(expanded: true)),
            ),
          ),
        ),
      );
      expect(find.byType(OfferCard), findsNWidgets(2));
      expect(find.text('Generic offer'), findsOneWidget);
      expect(find.text('Bike offer'), findsOneWidget);
      expect(find.text('Future offer'), findsNothing);
      expect(find.text('Expired offer'), findsNothing);
    },
  );
}

final _trip = TripLocations(
  pickup: LocationSelection.fromCoordinates(
    latitude: 12.97,
    longitude: 77.59,
    label: 'Pickup',
  ),
  destination: LocationSelection.fromCoordinates(
    latitude: 12.98,
    longitude: 77.60,
    label: 'Destination',
  ),
);

Future<void> _frames(WidgetTester tester) async {
  for (var i = 0; i < 6; i++) {
    await tester.pump(const Duration(milliseconds: 100));
  }
}

Future<_Fixture> _setup(WidgetTester tester) async {
  tester.view.physicalSize = const Size(430, 1000);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
  SharedPreferences.setMockInitialValues({'auth.userId': 8});
  final preferences = await SharedPreferences.getInstance();
  sl.registerSingleton<AuthLocalDatasource>(
    AuthLocalDatasource(preferences: preferences),
  );
  final dio = DioClient();
  dio.dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (request, handler) {
        handler.resolve(
          Response(
            requestOptions: request,
            statusCode: 200,
            data: {
              'data': request.path.contains('fare')
                  ? {
                      'fare': {'totalFare': 100},
                    }
                  : {'etaMinutes': 5, 'distanceKm': 2, 'polyline': ''},
            },
          ),
        );
      },
    ),
  );
  sl.registerSingleton<DioClient>(dio);
  final fixture = _Fixture();
  addTearDown(() async {
    await fixture.fare.close();
    await fixture.offers.close();
    await fixture.ride.close();
    await fixture.types.close();
    await fixture.user.close();
    await sl.unregister<AuthLocalDatasource>();
    await sl.unregister<DioClient>();
  });
  return fixture;
}

class _Fixture {
  final fare = _Fare();
  final offers = _Offers();
  final ride = _Ride();
  final types = _Types();
  final user = _User();
  Widget app(Widget page) => MultiBlocProvider(
    providers: [
      BlocProvider<FareBloc>.value(value: fare),
      BlocProvider<OfferBloc>.value(value: offers),
      BlocProvider<RideBloc>.value(value: ride),
      BlocProvider<RideTypeBloc>.value(value: types),
      BlocProvider<UserBloc>.value(value: user),
    ],
    child: MaterialApp(home: page),
  );
}

class _Recorder<S, E> extends Cubit<S> {
  _Recorder(super.initialState);
  final events = <E>[];
  void add(E event) => events.add(event);
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _Fare extends _Recorder<FareState, FareEvent> implements FareBloc {
  _Fare() : super(const FareState());
  final requests = <FareEstimateRequestModel>[];
  @override
  void add(FareEvent event) {
    super.add(event);
    if (event is EstimateFare) {
      requests.add(event.request);
      emit(FareState(status: FareStatus.loading, lastRequest: event.request));
    }
  }

  void complete(FareEstimateRequestModel request, {double discount = 0}) {
    emit(
      FareState(
        status: FareStatus.success,
        lastRequest: request,
        fareEstimate: FareEstimateResponse(
          estimatedDistanceKm: 2,
          estimatedDurationSeconds: 300,
          fare: Fare(
            baseFare: 30,
            distanceFare: 50,
            timeFare: 10,
            bookingFee: 5,
            tax: 5,
            surgeMultiplier: 1,
            discount: discount,
            totalFare: 100 - discount,
            currency: 'INR',
            paymentStatus: 'PENDING',
          ),
        ),
      ),
    );
  }
}

class _Offers extends _Recorder<OfferState, OfferEvent> implements OfferBloc {
  _Offers() : super(const OfferState());
  @override
  void add(OfferEvent event) {
    super.add(event);
    if (event is FetchOffers) {
      emit(
        OfferState(
          status: OfferStatus.success,
          lastRequest: event.request,
          offers: [
            OfferResponse(
              id: 20,
              offerCode: 'SWIFT20',
              title: 'Save on your ride',
              discountType: 'PERCENTAGE',
              discountValue: 20,
              active: true,
            ),
          ],
        ),
      );
    }
  }
}

class _Ride extends _Recorder<RideState, RideEvent> implements RideBloc {
  _Ride() : super(const RideState());
}

class _Types extends _Recorder<RideTypeState, RideTypeEvent>
    implements RideTypeBloc {
  _Types()
    : super(
        RideTypeState(
          status: RideTypeStatus.success,
          rideTypes: [
            RideTypeModel.fromVehicleTypeJson({
              'code': 'BIKE',
              'name': 'Bike',
            }, 0),
            RideTypeModel.fromVehicleTypeJson({
              'code': 'AUTO_RICKSHAW',
              'name': 'Auto Rickshaw',
            }, 1),
          ],
        ),
      );
  @override
  void add(RideTypeEvent event) {
    super.add(event);
    if (event is SelectRideType) {
      emit(state.copyWith(selectedRideTypeId: event.rideTypeId));
    }
  }
}

class _User extends _Recorder<UserState, UserEvent> implements UserBloc {
  _User()
    : super(
        UserState(
          status: UserStatus.success,
          user: UserReponse(
            id: 7,
            authUserId: 8,
            fullName: 'Fixture Rider',
            email: null,
            phoneNumber: '',
            profileImageUrl: null,
            active: true,
            deleted: false,
            allAddresses: [],
          ),
        ),
      );
}

class _Maps extends GoogleMapsFlutterPlatform {
  @override
  Widget buildViewWithConfiguration(
    int creationId,
    PlatformViewCreatedCallback onPlatformViewCreated, {
    required MapWidgetConfiguration widgetConfiguration,
    MapObjects mapObjects = const MapObjects(),
    MapConfiguration mapConfiguration = const MapConfiguration(),
  }) => const SizedBox.expand();
}
