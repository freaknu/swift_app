import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:geolocator/geolocator.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:google_maps_flutter_platform_interface/google_maps_flutter_platform_interface.dart';
import 'package:swift_app/features/common/shared/widgets/app_loading_page.dart';
import 'package:swift_app/features/rider/home/data/datasource/nearby_driver_datasource.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/google_map_widget.dart';
import 'package:swift_app/themes/app_theme.dart';

void main() {
  late GeolocatorPlatform originalLocation;
  late GoogleMapsFlutterPlatform originalMaps;
  late _FakeLocation location;
  late _FakeMaps maps;

  setUp(() {
    originalLocation = GeolocatorPlatform.instance;
    originalMaps = GoogleMapsFlutterPlatform.instance;
    location = _FakeLocation();
    maps = _FakeMaps();
    GeolocatorPlatform.instance = location;
    GoogleMapsFlutterPlatform.instance = maps;
  });

  tearDown(() {
    GeolocatorPlatform.instance = originalLocation;
    GoogleMapsFlutterPlatform.instance = originalMaps;
  });

  testWidgets('pending GPS does not signal that native map is ready', (
    tester,
  ) async {
    var settled = 0;
    await tester.pumpWidget(_app(onInitialLoadSettled: () => settled++));
    await tester.pump();
    expect(find.byType(AppLoadingProgress), findsOneWidget);
    expect(find.byType(GoogleMap), findsNothing);
    expect(location.positionRequests, 1);
    expect(settled, 0);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox.shrink());
  });

  testWidgets('disabled Location Services settles once and exposes settings', (
    tester,
  ) async {
    var settled = 0;
    location.servicesEnabled = false;
    await tester.pumpWidget(_app(onInitialLoadSettled: () => settled++));
    await tester.pumpAndSettle();
    expect(find.byType(AppLoadingProgress), findsNothing);
    expect(find.text('Turn on Location Services.'), findsOneWidget);
    expect(settled, 1);
    await tester.tap(find.text('Open settings'));
    await tester.pump();
    expect(location.locationSettingsRequests, 1);
    expect(location.positionRequests, 0);
    expect(settled, 1);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'permanently denied permission settles and exposes app settings',
    (tester) async {
      var settled = 0;
      location.permission = LocationPermission.deniedForever;
      await tester.pumpWidget(_app(onInitialLoadSettled: () => settled++));
      await tester.pumpAndSettle();
      expect(find.byType(AppLoadingProgress), findsNothing);
      expect(find.text('Allow location access in Settings.'), findsOneWidget);
      expect(settled, 1);
      await tester.tap(find.text('Open settings'));
      await tester.pump();
      expect(location.appSettingsRequests, 1);
      expect(location.positionRequests, 0);
      expect(tester.takeException(), isNull);
    },
  );

  for (final openSettings in [true, false]) {
    testWidgets(
      'large text keeps ${openSettings ? 'settings' : 'retry'} reachable in a narrow 280px map',
      (tester) async {
        tester.view.physicalSize = const Size(320, 568);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);
        var settled = 0;
        if (openSettings) {
          location.servicesEnabled = false;
        } else {
          location.permission = LocationPermission.denied;
        }
        await tester.pumpWidget(
          _app(
            textScaler: TextScaler.linear(2),
            padding: const EdgeInsets.only(top: 44, bottom: 34),
            onInitialLoadSettled: () => settled++,
          ),
        );
        await tester.pumpAndSettle();
        expect(settled, 1);
        expect(tester.takeException(), isNull);
        final errorScroll = find.descendant(
          of: find.byType(GoogleMapWidget),
          matching: find.byType(SingleChildScrollView),
        );
        expect(errorScroll, findsOneWidget);
        final scrollable = tester.state<ScrollableState>(
          find.descendant(of: errorScroll, matching: find.byType(Scrollable)),
        );
        expect(scrollable.position.maxScrollExtent, greaterThan(0));
        final action = find.text(openSettings ? 'Open settings' : 'Try again');
        await tester.ensureVisible(action);
        await tester.pumpAndSettle();
        final actionRect = tester.getRect(action);
        final mapRect = tester.getRect(find.byType(GoogleMapWidget));
        expect(actionRect.top, greaterThanOrEqualTo(mapRect.top));
        expect(actionRect.bottom, lessThanOrEqualTo(mapRect.bottom));
        await tester.tap(action);
        await tester.pumpAndSettle();
        if (openSettings) {
          expect(location.locationSettingsRequests, 1);
        } else {
          expect(location.permissionRequests, 2);
        }
        expect(settled, 1);
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets('location retry does not repeat the startup settled callback', (
    tester,
  ) async {
    var settled = 0;
    location.permission = LocationPermission.denied;
    await tester.pumpWidget(_app(onInitialLoadSettled: () => settled++));
    await tester.pumpAndSettle();
    expect(find.text('Location access is off.'), findsOneWidget);
    expect(find.text('Try again'), findsOneWidget);
    expect(location.permissionRequests, 1);
    expect(settled, 1);

    location.permission = LocationPermission.whileInUse;
    await tester.tap(find.text('Try again'));
    await tester.pump();
    expect(find.byType(AppLoadingProgress), findsOneWidget);
    expect(location.positionRequests, 1);
    location.position.complete(_position());
    await tester.pumpAndSettle();
    maps.createNativeView();
    maps.initialized.complete();
    await tester.pumpAndSettle();
    expect(find.byType(AppLoadingProgress), findsNothing);
    expect(find.byType(GoogleMap), findsOneWidget);
    expect(settled, 1);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pump();
  });

  testWidgets(
    'GPS success waits for native controller and preserves map state',
    (tester) async {
      var settled = 0;
      Position? reportedPosition;
      await tester.pumpWidget(
        _app(
          onInitialLoadSettled: () => settled++,
          onLocationChanged: (position) => reportedPosition = position,
        ),
      );
      await tester.pump();
      final position = _position();
      location.position.complete(position);
      await tester.pumpAndSettle();
      expect(reportedPosition, position);
      expect(find.byType(GoogleMap), findsOneWidget);
      expect(find.byType(AppLoadingProgress), findsOneWidget);
      expect(settled, 0);
      expect(maps.createdMapIds, hasLength(1));
      final firstMapState = tester.state(find.byType(GoogleMap));

      maps.createNativeView();
      await tester.pump();
      expect(maps.initializationRequests, 1);
      expect(settled, 0);
      maps.initialized.complete();
      await tester.pumpAndSettle();
      expect(find.byType(AppLoadingProgress), findsNothing);
      expect(tester.state(find.byType(GoogleMap)), same(firstMapState));
      expect(maps.createdMapIds, hasLength(1));
      expect(location.streamRequests, 0);
      expect(settled, 1);
      expect(tester.takeException(), isNull);

      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pump();
      expect(maps.disposalRequests, greaterThan(0));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'hiding local progress preserves initialization and failure callback',
    (tester) async {
      var settled = 0;
      await tester.pumpWidget(
        _app(
          showLoadingIndicator: false,
          onInitialLoadSettled: () => settled++,
        ),
      );
      await tester.pump();
      expect(find.byType(AppLoadingProgress), findsNothing);
      expect(location.positionRequests, 1);
      expect(settled, 0);
      location.position.completeError(StateError('Location timed out'));
      await tester.pumpAndSettle();
      expect(find.text('Couldn’t find your location.'), findsOneWidget);
      expect(find.text('Try again'), findsOneWidget);
      expect(settled, 1);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'hidden native initialization calls a parent setter after the frame',
    (tester) async {
      var settled = 0;
      var parentReady = false;
      await tester.pumpWidget(
        MaterialApp(
          home: StatefulBuilder(
            builder: (context, setParentState) => MediaQuery(
              data: MediaQuery.of(context).copyWith(disableAnimations: true),
              child: AppStartupGate(
                ready: parentReady,
                child: Scaffold(
                  body: SizedBox(
                    height: 280,
                    child: GoogleMapWidget(
                      active: false,
                      showLoadingIndicator: false,
                      onInitialLoadSettled: () {
                        settled++;
                        setParentState(() => parentReady = true);
                      },
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      );
      await tester.pump();
      expect(find.byType(AppLoadingPage), findsOneWidget);
      location.position.complete(_position());
      await tester.pumpAndSettle();
      expect(find.byType(GoogleMap), findsOneWidget);
      expect(find.byType(AppLoadingPage), findsOneWidget);
      expect(settled, 0);
      maps.createNativeView();
      maps.initialized.complete();
      await tester.pumpAndSettle();
      expect(parentReady, isTrue);
      expect(settled, 1);
      expect(find.byType(AppLoadingPage), findsNothing);
      expect(maps.createdMapIds, hasLength(1));
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pump();
    },
  );

  for (final failed in [false, true]) {
    testWidgets(
      'native initialization waits for nearby ${failed ? 'failure' : 'data'} before opening startup',
      (tester) async {
        final nearby = _FakeNearby();
        var ready = false;
        var settled = 0;
        await tester.pumpWidget(
          MaterialApp(
            theme: AppTheme.light(),
            home: StatefulBuilder(
              builder: (context, setParentState) => MediaQuery(
                data: MediaQuery.of(context).copyWith(disableAnimations: true),
                child: AppStartupGate(
                  ready: ready,
                  child: Scaffold(
                    body: SizedBox(
                      height: 280,
                      child: GoogleMapWidget(
                        showNearbyDrivers: true,
                        nearbyDatasource: nearby,
                        showLoadingIndicator: false,
                        onInitialLoadSettled: () {
                          settled++;
                          setParentState(() => ready = true);
                        },
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        );
        await tester.pump();
        location.position.complete(_position());
        await tester.pumpAndSettle();
        expect(nearby.requests, 1);
        expect(find.byType(GoogleMap), findsOneWidget);
        final mapState = tester.state(find.byType(GoogleMap));
        maps.createNativeView();
        maps.initialized.complete();
        await tester.pumpAndSettle();
        expect(settled, 0);
        expect(ready, isFalse);
        expect(find.byType(AppLoadingPage), findsOneWidget);

        if (failed) {
          nearby.response.completeError(StateError('Nearby API unavailable'));
        } else {
          nearby.response.complete([
            NearbyDriverPosition(
              driverId: 7,
              vehicleType: 'AUTO',
              updatedAt: DateTime.now().toUtc(),
              point: const LatLng(12.9717, 77.5947),
            ),
          ]);
        }
        await tester.pumpAndSettle();
        expect(ready, isTrue);
        expect(settled, 1);
        expect(find.byType(AppLoadingPage), findsNothing);
        expect(tester.state(find.byType(GoogleMap)), same(mapState));
        expect(maps.createdMapIds, hasLength(1));
        expect(
          tester.widget<GoogleMap>(find.byType(GoogleMap)).markers,
          hasLength(failed ? 0 : 1),
        );
        await tester.pump();
        expect(settled, 1);
        expect(tester.takeException(), isNull);
        await tester.pumpWidget(const SizedBox.shrink());
        await tester.pump();
      },
    );
  }

  testWidgets(
    'inactive maps skip nearby data when native initialization settles',
    (tester) async {
      final nearby = _FakeNearby();
      var settled = 0;
      await tester.pumpWidget(
        _app(
          showNearbyDrivers: true,
          nearbyDatasource: nearby,
          onInitialLoadSettled: () => settled++,
        ),
      );
      await tester.pump();
      location.position.complete(_position());
      await tester.pumpAndSettle();
      maps.createNativeView();
      maps.initialized.complete();
      await tester.pumpAndSettle();
      expect(nearby.requests, 0);
      expect(settled, 1);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pump();
    },
  );

  testWidgets(
    'location failure does not wait for native or nearby initialization',
    (tester) async {
      final nearby = _FakeNearby();
      var settled = 0;
      location.permission = LocationPermission.deniedForever;
      await tester.pumpWidget(
        _app(
          active: true,
          showNearbyDrivers: true,
          nearbyDatasource: nearby,
          onInitialLoadSettled: () => settled++,
        ),
      );
      await tester.pumpAndSettle();
      expect(nearby.requests, 0);
      expect(maps.createdMapIds, isEmpty);
      expect(settled, 1);
      expect(find.text('Open settings'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'a disposed initial location request cannot notify its old parent',
    (tester) async {
      var settled = 0;
      await tester.pumpWidget(_app(onInitialLoadSettled: () => settled++));
      await tester.pump();
      await tester.pumpWidget(const SizedBox.shrink());
      location.position.completeError(StateError('Disposed GPS request'));
      await tester.pump();
      await tester.pump();
      expect(settled, 0);
      expect(tester.takeException(), isNull);
    },
  );
}

Widget _app({
  bool active = false,
  bool showNearbyDrivers = false,
  NearbyDriverDatasource? nearbyDatasource,
  bool showLoadingIndicator = true,
  VoidCallback? onInitialLoadSettled,
  ValueChanged<Position>? onLocationChanged,
  TextScaler textScaler = TextScaler.noScaling,
  EdgeInsets padding = EdgeInsets.zero,
}) => MaterialApp(
  theme: AppTheme.light(),
  home: Builder(
    builder: (context) => MediaQuery(
      data: MediaQuery.of(context).copyWith(
        disableAnimations: true,
        textScaler: textScaler,
        padding: padding,
      ),
      child: Scaffold(
        body: SizedBox(
          height: 280,
          child: GoogleMapWidget(
            active: active,
            showNearbyDrivers: showNearbyDrivers,
            nearbyDatasource: nearbyDatasource,
            showLoadingIndicator: showLoadingIndicator,
            onInitialLoadSettled: onInitialLoadSettled,
            onLocationChanged: onLocationChanged,
          ),
        ),
      ),
    ),
  ),
);

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
  LocationPermission permission = LocationPermission.whileInUse;
  Completer<Position>? _position;
  Completer<Position> get position => _position ??= Completer<Position>();
  var positionRequests = 0;
  var streamRequests = 0;
  var permissionRequests = 0;
  var appSettingsRequests = 0;
  var locationSettingsRequests = 0;

  @override
  Future<bool> isLocationServiceEnabled() async => servicesEnabled;

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

class _FakeNearby extends NearbyDriverDatasource {
  _FakeNearby() : super(Dio());

  Completer<List<NearbyDriverPosition>>? _response;
  Completer<List<NearbyDriverPosition>> get response =>
      _response ??= Completer<List<NearbyDriverPosition>>();
  var requests = 0;

  @override
  Future<List<NearbyDriverPosition>> fetch({
    required double latitude,
    required double longitude,
    double distanceKm = 5,
  }) {
    requests++;
    return response.future;
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
