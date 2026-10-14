import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:geolocator/geolocator.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:google_maps_flutter_platform_interface/google_maps_flutter_platform_interface.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/core/notifications/notification_inbox_datasource.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_bloc.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_event.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_state.dart';
import 'package:swift_app/features/driver/data/datasource/driver_location_publisher.dart';
import 'package:swift_app/features/driver/data/datasource/driver_realtime_service.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/driver/data/model/driver_document_model.dart';
import 'package:swift_app/features/driver/data/model/driver_journey_model.dart';
import 'package:swift_app/features/driver/data/model/driver_profile_model.dart';
import 'package:swift_app/features/driver/data/model/driver_rating_model.dart';
import 'package:swift_app/features/driver/data/model/driver_statistics_model.dart';
import 'package:swift_app/features/driver/data/model/pending_offer_model.dart';
import 'package:swift_app/features/driver/data/model/profile_completeness_model.dart';
import 'package:swift_app/features/driver/data/model/realtime_ride_event.dart';
import 'package:swift_app/features/driver/presentation/pages/driver_home_page.dart';
import 'package:swift_app/features/common/shared/widgets/swift_bottom_navigation_bar.dart';
import 'package:swift_app/features/common/shared/widgets/app_loading_page.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/google_map_widget.dart';
import 'package:swift_app/features/common/shared/widgets/vehicle_photo.dart';

ApiResponseFormat<T> _ok<T>(T data) => ApiResponseFormat.success(
  data: data,
  statusCode: 200,
  message: 'OK',
  endpoint: 'fixture',
);

const _driverProfile = DriverProfileModel(
  driverId: 7,
  driverName: 'Test Driver',
  driverStatus: 'ONLINE',
  vehicles: [DriverVehicleModel(id: 3, vehicleType: 'SEDAN', active: true)],
);

const _statistics = DriverStatisticsModel(
  completedTrips: 0,
  cancelledTrips: 0,
  lifetimeDistanceKm: 0,
  lifetimeEarning: 0,
  averageRating: 0,
);

class _OfferDatasource extends DriverRemoteDatasource {
  _OfferDatasource({
    required super.dioClient,
    required super.endpoints,
    required super.authLocalDatasource,
  });

  List<PendingOfferModel> pendingOffers = [];
  int documentRequests = 0;
  int journeyRequests = 0;
  int inboxRequests = 0;
  int activeRequests = 0;
  Completer<ApiResponseFormat<DriverProfileModel>>? pendingProfile;
  Completer<ApiResponseFormat<DriverStatisticsModel>>? pendingStatistics;
  Completer<ApiResponseFormat<List<PendingOfferModel>>>? pendingInbox;
  Completer<ApiResponseFormat<int?>>? pendingActiveRide;

  @override
  Future<ApiResponseFormat<DriverProfileModel>> getProfile() async =>
      pendingProfile == null ? _ok(_driverProfile) : pendingProfile!.future;

  @override
  Future<ApiResponseFormat<List<DriverVehicleModel>>> getVehicles() async =>
      _ok(const [
        DriverVehicleModel(
          id: 3,
          vehicleType: 'SEDAN',
          registrationNumber: 'KA 01 AB 1234',
          active: true,
        ),
      ]);

  @override
  Future<ApiResponseFormat<ProfileCompletenessModel>>
  getProfileCompleteness() async => _ok(
    const ProfileCompletenessModel(
      complete: true,
      personal: CompletenessSection(complete: true, missing: []),
      documents: CompletenessSection(complete: true, missing: []),
      vehicle: CompletenessSection(complete: true, missing: []),
      missing: [],
    ),
  );

  @override
  Future<ApiResponseFormat<DriverStatisticsModel>> getStatistics(
    int driverId,
  ) async =>
      pendingStatistics == null ? _ok(_statistics) : pendingStatistics!.future;

  @override
  Future<ApiResponseFormat<List<PendingOfferModel>>> getPendingOffers() async {
    inboxRequests++;
    return pendingInbox == null ? _ok(pendingOffers) : pendingInbox!.future;
  }

  @override
  Future<ApiResponseFormat<int?>> getActiveRideId() async {
    activeRequests++;
    return pendingActiveRide == null
        ? _ok<int?>(null)
        : pendingActiveRide!.future;
  }

  @override
  Future<ApiResponseFormat<List<DriverDocumentModel>>> getDocuments() async {
    documentRequests++;
    return _ok(const []);
  }

  @override
  Future<ApiResponseFormat<DriverJourneyPage>> getJourneys({
    int page = 0,
    int size = 20,
    String? status,
  }) async {
    journeyRequests++;
    return _ok(
      DriverJourneyPage(
        content: const [],
        page: page,
        totalPages: 0,
        totalElements: 0,
        last: true,
      ),
    );
  }

  @override
  Future<ApiResponseFormat<DriverRatingModel>> getRating(int driverId) async =>
      _ok(const DriverRatingModel(averageRating: 0, entries: []));
}

class _Realtime extends DriverRealtimeService {
  _Realtime({required super.authLocalDatasource});

  final StreamController<RealtimeRideEvent> events =
      StreamController<RealtimeRideEvent>.broadcast();

  @override
  Stream<RealtimeRideEvent> get rideEvents => events.stream;

  @override
  void connect({required int driverId}) {}

  @override
  void disconnect() {}
}

class _Location extends DriverLocationPublisher {
  _Location({required super.datasource, required super.realtimeService});

  @override
  bool get isRunning => true;

  @override
  Future<void> stop({bool publishFinalPosition = false}) async {}
}

class _Inbox extends NotificationInboxDatasource {
  const _Inbox(super.dio);

  @override
  Future<int> unreadCount(String role) async => 0;
}

class _AuthBloc extends Cubit<AuthState> implements AuthBloc {
  _AuthBloc() : super(const AuthAuthenticated());

  @override
  void add(AuthEvent event) {}

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

Future<_OfferDatasource> _pumpLoadingDriverHome(
  WidgetTester tester, {
  Future<bool> Function()? onHomeReady,
}) async {
  tester.view.physicalSize = const Size(411, 923);
  tester.view.devicePixelRatio = 1;
  tester.view.padding = FakeViewPadding(top: 47, bottom: 34);
  tester.view.viewPadding = FakeViewPadding(top: 47, bottom: 34);
  addTearDown(tester.view.resetPadding);
  addTearDown(tester.view.resetViewPadding);
  addTearDown(() {
    tester.view.resetPhysicalSize();
    tester.view.resetDevicePixelRatio();
  });
  SharedPreferences.setMockInitialValues({'auth.userId': 7});
  final preferences = await SharedPreferences.getInstance();
  final auth = AuthLocalDatasource(preferences: preferences);
  final datasource = _OfferDatasource(
    dioClient: DioClient(),
    endpoints: RiderApiEndpoints(),
    authLocalDatasource: auth,
  )..pendingProfile = Completer<ApiResponseFormat<DriverProfileModel>>();
  final realtime = _Realtime(authLocalDatasource: auth);
  final location = _Location(datasource: datasource, realtimeService: realtime);
  final authBloc = _AuthBloc();
  sl.registerSingleton<NotificationInboxDatasource>(_Inbox(Dio()));
  addTearDown(() async {
    await sl.unregister<NotificationInboxDatasource>();
    await realtime.events.close();
    await authBloc.close();
  });
  await tester.pumpWidget(
    BlocProvider<AuthBloc>.value(
      value: authBloc,
      child: MaterialApp(
        home: DriverHomePage(
          onHomeReady: onHomeReady,
          datasource: datasource,
          realtimeService: realtime,
          locationPublisher: location,
        ),
      ),
    ),
  );
  await tester.pump();
  return datasource;
}

Finder get _startupLoader => find.byType(AppLoadingPage);

Future<void> _pumpFrames(WidgetTester tester, [int count = 6]) async {
  for (var frame = 0; frame < count; frame++) {
    await tester.pump();
  }
}

void main() {
  late GeolocatorPlatform originalLocation;
  late GoogleMapsFlutterPlatform originalMaps;
  setUp(() {
    originalLocation = GeolocatorPlatform.instance;
    originalMaps = GoogleMapsFlutterPlatform.instance;
    GeolocatorPlatform.instance = _FakeLocation()..servicesEnabled = false;
  });
  tearDown(() {
    GeolocatorPlatform.instance = originalLocation;
    GoogleMapsFlutterPlatform.instance = originalMaps;
  });
  testWidgets('new offer reveals Drive card once while driver views Trips', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(411, 923);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });
    SharedPreferences.setMockInitialValues({'auth.userId': 7});
    final preferences = await SharedPreferences.getInstance();
    final auth = AuthLocalDatasource(preferences: preferences);
    final datasource = _OfferDatasource(
      dioClient: DioClient(),
      endpoints: RiderApiEndpoints(),
      authLocalDatasource: auth,
    );
    final realtime = _Realtime(authLocalDatasource: auth);
    final location = _Location(
      datasource: datasource,
      realtimeService: realtime,
    );
    final authBloc = _AuthBloc();
    sl.registerSingleton<NotificationInboxDatasource>(_Inbox(Dio()));
    addTearDown(() async {
      await sl.unregister<NotificationInboxDatasource>();
      await realtime.events.close();
      await authBloc.close();
    });

    await tester.pumpWidget(
      BlocProvider<AuthBloc>.value(
        value: authBloc,
        child: MaterialApp(
          home: DriverHomePage(
            datasource: datasource,
            realtimeService: realtime,
            locationPublisher: location,
          ),
        ),
      ),
    );
    await _pumpFrames(tester);
    expect(datasource.journeyRequests, 0);
    expect(datasource.documentRequests, 0);
    expect(
      tester
          .widget<SwiftBottomNavigationBar>(
            find.byType(SwiftBottomNavigationBar),
          )
          .selectedIndex,
      0,
    );
    await tester.tap(find.text('Trips').last);
    await tester.pump();
    expect(datasource.journeyRequests, 1);
    expect(datasource.documentRequests, 0);
    expect(
      tester
          .widget<SwiftBottomNavigationBar>(
            find.byType(SwiftBottomNavigationBar),
          )
          .selectedIndex,
      1,
    );

    final raw = <String, dynamic>{
      'rideStatus': 'OFFER_SENT',
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
      'expiresAt': DateTime.now()
          .add(const Duration(minutes: 2))
          .toIso8601String(),
    };
    realtime.events.add(RealtimeRideEvent.fromJson(raw));
    await tester.pump();
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));
    await tester.pump(const Duration(milliseconds: 400));

    expect(
      tester
          .widget<SwiftBottomNavigationBar>(
            find.byType(SwiftBottomNavigationBar),
          )
          .selectedIndex,
      0,
    );
    expect(find.text('Ride #42'), findsOneWidget);
    final decline = tester.getBottomRight(find.text('Decline'));
    expect(
      decline.dy,
      lessThan(tester.getTopLeft(find.byType(SwiftBottomNavigationBar)).dy),
    );
    expect(tester.takeException(), isNull);

    await tester.tap(find.text('Trips').last);
    await tester.pump();
    realtime.events.add(RealtimeRideEvent.fromJson(raw));
    datasource.pendingOffers = [PendingOfferModel.fromJson(raw)];
    await tester.pump(const Duration(seconds: 5));
    await tester.pump();
    expect(
      tester
          .widget<SwiftBottomNavigationBar>(
            find.byType(SwiftBottomNavigationBar),
          )
          .selectedIndex,
      1,
    );
    expect(tester.takeException(), isNull);
  });
  testWidgets(
    'driver startup page covers the whole app until initial loading settles',
    (tester) async {
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      var acknowledgements = 0;
      final datasource = await _pumpLoadingDriverHome(
        tester,
        onHomeReady: () async {
          acknowledgements++;
          return true;
        },
      );
      expect(_startupLoader, findsOneWidget);
      expect(find.text('Loading your data and map…'), findsOneWidget);
      expect(tester.getSize(_startupLoader), const Size(411, 923));
      expect(find.text('You’re offline'), findsNothing);
      expect(tester.takeException(), isNull);
      expect(acknowledgements, 0);

      datasource.pendingProfile!.complete(_ok(_driverProfile));
      datasource.pendingProfile = null;
      await _pumpFrames(tester);

      expect(_startupLoader, findsNothing);
      expect(acknowledgements, 1);
      expect(find.text('You’re online'), findsOneWidget);
      expect(
        tester
            .widget<GoogleMapWidget>(
              find.byType(GoogleMapWidget, skipOffstage: false),
            )
            .showLoadingIndicator,
        isTrue,
      );
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox.shrink());
    },
  );

  testWidgets('driver vehicle photo fits a narrow dashboard with larger text', (
    tester,
  ) async {
    final datasource = await _pumpLoadingDriverHome(tester);
    tester.view.physicalSize = const Size(320, 760);
    tester.platformDispatcher.textScaleFactorTestValue = 2;
    addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
    datasource.pendingProfile!.complete(_ok(_driverProfile));
    datasource.pendingProfile = null;
    await _pumpFrames(tester);
    expect(_startupLoader, findsNothing);
    final dashboardController = tester
        .widget<CustomScrollView>(find.byType(CustomScrollView).first)
        .controller!;
    dashboardController.jumpTo(dashboardController.position.maxScrollExtent);
    await _pumpFrames(tester);
    expect(find.byType(VehiclePhoto), findsOneWidget);
    expect(find.text('KA 01 AB 1234'), findsWidgets);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox.shrink());
  });

  testWidgets(
    'driver backend failure exposes retry without reopening startup',
    (tester) async {
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      var acknowledgements = 0;
      final datasource = await _pumpLoadingDriverHome(
        tester,
        onHomeReady: () async {
          acknowledgements++;
          return true;
        },
      );
      expect(_startupLoader, findsOneWidget);

      datasource.pendingProfile!.complete(
        ApiResponseFormat<DriverProfileModel>.failure(
          statusCode: 503,
          message: 'The driver service is unavailable.',
          endpoint: 'fixture',
        ),
      );
      await _pumpFrames(tester);
      expect(_startupLoader, findsNothing);
      expect(find.text('Driver profile unavailable'), findsOneWidget);
      expect(find.text('The driver service is unavailable.'), findsOneWidget);
      expect(acknowledgements, 0);

      datasource.pendingProfile =
          Completer<ApiResponseFormat<DriverProfileModel>>();
      final retry = find.widgetWithText(OutlinedButton, 'Retry');
      await tester.ensureVisible(retry);
      await tester.tap(retry);
      await tester.pump();
      expect(_startupLoader, findsNothing);

      datasource.pendingProfile!.complete(_ok(_driverProfile));
      datasource.pendingProfile = null;
      await _pumpFrames(tester);
      expect(_startupLoader, findsNothing);
      expect(find.text('You’re online'), findsOneWidget);
      expect(acknowledgements, 1);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox.shrink());
    },
  );
  testWidgets(
    'startup reveals a queued driver offer with reduced motion and no polling',
    (tester) async {
      tester.platformDispatcher.accessibilityFeaturesTestValue =
          FakeAccessibilityFeatures(disableAnimations: true);
      addTearDown(
        tester.platformDispatcher.clearAccessibilityFeaturesTestValue,
      );
      final datasource = await _pumpLoadingDriverHome(tester);
      final raw = <String, dynamic>{
        'rideStatus': 'OFFER_SENT',
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
        'expiresAt': DateTime.now()
            .add(const Duration(minutes: 2))
            .toIso8601String(),
      };
      datasource.pendingOffers = [PendingOfferModel.fromJson(raw)];
      datasource.pendingProfile!.complete(_ok(_driverProfile));
      datasource.pendingProfile = null;
      for (var frame = 0; frame < 12; frame++) {
        await tester.pump();
        final decline = find.text('Decline', skipOffstage: false);
        final revealed =
            _startupLoader.evaluate().isEmpty &&
            decline.evaluate().isNotEmpty &&
            tester.getBottomRight(decline).dy <
                tester.getTopLeft(find.byType(SwiftBottomNavigationBar)).dy;
        if (revealed) break;
        expect(
          tester.binding.hasScheduledFrame,
          isTrue,
          reason:
              'Startup readiness and offer reveal must schedule their own next frame.',
        );
      }
      expect(_startupLoader, findsNothing);
      expect(find.text('Ride #42'), findsOneWidget);
      final decline = find.text('Decline');
      expect(
        tester.getBottomRight(decline).dy,
        lessThan(tester.getTopLeft(find.byType(SwiftBottomNavigationBar)).dy),
      );
      expect(datasource.inboxRequests, 1);
      expect(datasource.activeRequests, 1);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox.shrink());
    },
  );

  for (final mapFirst in [false, true]) {
    testWidgets(
      'driver startup waits for APIs and native map, mapFirst=$mapFirst',
      (tester) async {
        final location = _FakeLocation();
        final maps = _FakeMaps();
        GeolocatorPlatform.instance = location;
        GoogleMapsFlutterPlatform.instance = maps;
        final datasource = await _pumpLoadingDriverHome(tester);
        datasource.pendingStatistics =
            Completer<ApiResponseFormat<DriverStatisticsModel>>();
        datasource.pendingInbox =
            Completer<ApiResponseFormat<List<PendingOfferModel>>>();
        datasource.pendingActiveRide = Completer<ApiResponseFormat<int?>>();
        datasource.pendingProfile!.complete(_ok(_driverProfile));
        datasource.pendingProfile = null;
        await _pumpFrames(tester);
        expect(_startupLoader, findsOneWidget);
        expect(location.positionRequests, 1);
        expect(
          tester
              .widget<GoogleMapWidget>(find.byType(GoogleMapWidget))
              .showLoadingIndicator,
          isFalse,
        );

        Future<void> finishApis() async {
          datasource.pendingStatistics!.complete(_ok(_statistics));
          await _pumpFrames(tester);
          expect(_startupLoader, findsOneWidget);
          datasource.pendingInbox!.complete(_ok(const []));
          await _pumpFrames(tester);
          expect(_startupLoader, findsOneWidget);
          datasource.pendingActiveRide!.complete(_ok<int?>(null));
          await _pumpFrames(tester);
        }

        Future<void> finishMap() async {
          location.position.complete(_position());
          await _pumpFrames(tester);
          expect(find.byType(GoogleMap), findsOneWidget);
          expect(maps.createdMapIds, hasLength(1));
          expect(_startupLoader, findsOneWidget);
          maps.createNativeView();
          await _pumpFrames(tester);
          expect(maps.initializationRequests, 1);
          expect(_startupLoader, findsOneWidget);
          maps.initialized.complete();
          await _pumpFrames(tester);
        }

        if (mapFirst) {
          await finishMap();
          expect(_startupLoader, findsOneWidget);
          await finishApis();
        } else {
          await finishApis();
          expect(_startupLoader, findsOneWidget);
          await finishMap();
        }
        expect(_startupLoader, findsNothing);
        expect(find.text('You’re online'), findsOneWidget);
        expect(tester.takeException(), isNull);
        await tester.pumpWidget(const SizedBox.shrink());
        await tester.pump();
      },
    );
  }

  testWidgets(
    'driver map initializes behind startup on short screens and large text',
    (tester) async {
      final location = _FakeLocation()
        ..permission = LocationPermission.deniedForever;
      GeolocatorPlatform.instance = location;
      final datasource = await _pumpLoadingDriverHome(tester);
      datasource.pendingStatistics =
          Completer<ApiResponseFormat<DriverStatisticsModel>>();
      tester.view.physicalSize = const Size(320, 540);
      tester.platformDispatcher.textScaleFactorTestValue = 3;
      addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
      datasource.pendingProfile!.complete(_ok(_driverProfile));
      datasource.pendingProfile = null;
      await _pumpFrames(tester);
      expect(find.byType(GoogleMapWidget, skipOffstage: false), findsOneWidget);
      expect(location.serviceChecks, greaterThan(0));
      expect(_startupLoader, findsOneWidget);
      datasource.pendingStatistics!.complete(_ok(_statistics));
      await _pumpFrames(tester);
      expect(_startupLoader, findsNothing);
      await tester.scrollUntilVisible(
        find.text('Allow location access in Settings.'),
        300,
        scrollable: find
            .descendant(
              of: find.byType(CustomScrollView).first,
              matching: find.byType(Scrollable),
            )
            .first,
      );
      expect(find.text('Open settings'), findsOneWidget);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox.shrink());
    },
  );

  testWidgets('driver cached refresh leaves the dashboard visible', (
    tester,
  ) async {
    final datasource = await _pumpLoadingDriverHome(tester);
    datasource.pendingProfile!.complete(_ok(_driverProfile));
    datasource.pendingProfile = null;
    await _pumpFrames(tester);
    expect(_startupLoader, findsNothing);
    datasource.pendingProfile =
        Completer<ApiResponseFormat<DriverProfileModel>>();
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();
    expect(_startupLoader, findsNothing);
    expect(find.text('You’re online'), findsOneWidget);
    datasource.pendingProfile!.complete(_ok(_driverProfile));
    datasource.pendingProfile = null;
    await _pumpFrames(tester);
    expect(_startupLoader, findsNothing);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox.shrink());
  });
}

Position _position() => Position(
  longitude: 77.5946,
  latitude: 12.9716,
  timestamp: DateTime.utc(2026, 10, 14),
  accuracy: 5,
  altitude: 0,
  altitudeAccuracy: 0,
  heading: 0,
  headingAccuracy: 0,
  speed: 0,
  speedAccuracy: 0,
);

class _FakeLocation extends GeolocatorPlatform {
  bool servicesEnabled = true;
  int serviceChecks = 0;
  LocationPermission permission = LocationPermission.whileInUse;
  Completer<Position>? _position;
  Completer<Position> get position => _position ??= Completer<Position>();
  var positionRequests = 0;
  var streamRequests = 0;
  var permissionRequests = 0;
  var appSettingsRequests = 0;
  var locationSettingsRequests = 0;

  @override
  Future<bool> isLocationServiceEnabled() async {
    serviceChecks++;
    return servicesEnabled;
  }

  @override
  Future<LocationPermission> checkPermission() async => permission;

  @override
  Future<LocationPermission> requestPermission() async {
    permissionRequests++;
    return permission;
  }

  @override
  Future<Position> getCurrentPosition({LocationSettings? locationSettings}) {
    positionRequests++;
    return position.future;
  }

  @override
  Stream<Position> getPositionStream({LocationSettings? locationSettings}) {
    streamRequests++;
    return const Stream.empty();
  }

  @override
  Future<bool> openAppSettings() async {
    appSettingsRequests++;
    return true;
  }

  @override
  Future<bool> openLocationSettings() async {
    locationSettingsRequests++;
    return true;
  }
}

/// Keeps platform creation pending while the real GoogleMap widget mounts.
/// Rendering tiles remains the native SDK's job; no method channel is needed.
class _FakeMaps extends GoogleMapsFlutterPlatform {
  Completer<void>? _initialized;
  Completer<void> get initialized => _initialized ??= Completer<void>();
  final createdMapIds = <int>[];
  PlatformViewCreatedCallback? _onCreated;
  var initializationRequests = 0;
  var disposalRequests = 0;

  void createNativeView() => _onCreated!(createdMapIds.single);

  @override
  Widget buildViewWithConfiguration(
    int creationId,
    PlatformViewCreatedCallback onPlatformViewCreated, {
    required MapWidgetConfiguration widgetConfiguration,
    MapObjects mapObjects = const MapObjects(),
    MapConfiguration mapConfiguration = const MapConfiguration(),
  }) {
    if (!createdMapIds.contains(creationId)) {
      createdMapIds.add(creationId);
      _onCreated = onPlatformViewCreated;
    }
    return const SizedBox.expand(key: ValueKey('fake-native-map'));
  }

  @override
  Future<void> init(int mapId) {
    initializationRequests++;
    return initialized.future;
  }

  @override
  void dispose({required int mapId}) => disposalRequests++;

  @override
  Future<void> updateMapConfiguration(
    MapConfiguration configuration, {
    required int mapId,
  }) async {}

  @override
  Future<void> updateMarkers(
    MarkerUpdates updates, {
    required int mapId,
  }) async {}

  @override
  Future<void> updatePolygons(
    PolygonUpdates updates, {
    required int mapId,
  }) async {}

  @override
  Future<void> updatePolylines(
    PolylineUpdates updates, {
    required int mapId,
  }) async {}

  @override
  Future<void> updateCircles(
    CircleUpdates updates, {
    required int mapId,
  }) async {}

  @override
  Future<void> updateHeatmaps(
    HeatmapUpdates updates, {
    required int mapId,
  }) async {}

  @override
  Future<void> updateTileOverlays({
    required Set<TileOverlay> newTileOverlays,
    required int mapId,
  }) async {}

  @override
  Future<void> updateClusterManagers(
    ClusterManagerUpdates updates, {
    required int mapId,
  }) async {}

  @override
  Future<void> updateGroundOverlays(
    GroundOverlayUpdates updates, {
    required int mapId,
  }) async {}

  @override
  Stream<MarkerTapEvent> onMarkerTap({required int mapId}) =>
      const Stream.empty();

  @override
  Stream<InfoWindowTapEvent> onInfoWindowTap({required int mapId}) =>
      const Stream.empty();

  @override
  Stream<MarkerDragStartEvent> onMarkerDragStart({required int mapId}) =>
      const Stream.empty();

  @override
  Stream<MarkerDragEvent> onMarkerDrag({required int mapId}) =>
      const Stream.empty();

  @override
  Stream<MarkerDragEndEvent> onMarkerDragEnd({required int mapId}) =>
      const Stream.empty();

  @override
  Stream<PolylineTapEvent> onPolylineTap({required int mapId}) =>
      const Stream.empty();

  @override
  Stream<PolygonTapEvent> onPolygonTap({required int mapId}) =>
      const Stream.empty();

  @override
  Stream<CircleTapEvent> onCircleTap({required int mapId}) =>
      const Stream.empty();

  @override
  Stream<MapTapEvent> onTap({required int mapId}) => const Stream.empty();

  @override
  Stream<MapLongPressEvent> onLongPress({required int mapId}) =>
      const Stream.empty();

  @override
  Stream<ClusterTapEvent> onClusterTap({required int mapId}) =>
      const Stream.empty();
}
