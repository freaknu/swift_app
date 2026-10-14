import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/vehicle_photo.dart';
import 'dart:async';
import 'dart:math' as math;

import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_bloc.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/rider/home/domain/entity/ride_type.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_state.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/models/location_selection.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/google_map_style.dart';
import 'package:swift_app/features/rider/ride/presentation/blocs/ride_bloc.dart';
import 'package:swift_app/features/rider/ride/presentation/blocs/ride_state.dart';
import 'package:swift_app/features/rider/ride/presentation/pages/ride_options_page.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

/// Per-ride-type live quote: route + ETA from the location ETA API,
/// fare from the fare estimate API.
class _TypeQuote {
  List<LatLng>? routePoints;
  int? etaMinutes;
  double? distanceKm;
  double? fare;
  bool etaLoading = false;
  bool etaUnavailable = false;
  bool fareLoading = false;
  bool fareUnavailable = false;
}

/// Works with the catalog's concrete RideTypeModel list without requiring a
/// subtype-specific `firstWhere(orElse:)` callback at runtime.
RideType selectPreferredRideType(
  Iterable<RideType> types,
  String? preferredCode,
) {
  final first = types.first;
  for (final type in types) {
    if (type.code == preferredCode) return type;
  }
  return first;
}

class TripRouteMapPage extends StatefulWidget {
  final TripLocations trip;
  final String? preferredRideTypeCode;

  const TripRouteMapPage({
    super.key,
    required this.trip,
    this.preferredRideTypeCode,
  });

  @override
  State<TripRouteMapPage> createState() => _TripRouteMapPageState();
}

class _TripRouteMapPageState extends State<TripRouteMapPage> {
  static const double _worldScaleRouteMeters = 250000;

  GoogleMapController? _mapController;

  /// Route geometry changes only when its quote or vehicle choice changes.
  List<LatLng> _routePoints = [];

  /// Static route polylines, built once per route/theme and reused across
  /// rebuilds. Keeping this preview stable avoids continuous polyline
  /// serialization and native-map updates while the rider compares fares.
  Set<Polyline> _routePolylines = {};
  bool _routePolylinesDark = false;

  // ---- Rapido-style ride type selection ----
  final Map<String, _TypeQuote> _quotes = {};
  RideType? _selectedType;
  bool _quotesKickStarted = false;
  bool _openingOptions = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _hideKeyboard());
  }

  Future<void> _closeTextInput() async {
    await SystemChannels.textInput.invokeMethod<void>('TextInput.hide');
    try {
      await SystemChannels.textInput.invokeMethod<void>(
        'TextInput.clearClient',
      );
    } catch (_) {
      // Older Flutter engines may not expose clearClient to framework calls.
    }
  }

  Future<void> _hideKeyboard() async {
    FocusManager.instance.primaryFocus?.unfocus(
      disposition: UnfocusDisposition.scope,
    );
    await _closeTextInput();
    await Future<void>.delayed(const Duration(milliseconds: 180));
    await _closeTextInput();
  }

  LatLng get _pickup => LatLng(
    widget.trip.pickup.position.latitude,
    widget.trip.pickup.position.longitude,
  );

  LatLng get _destination => LatLng(
    widget.trip.destination.position.latitude,
    widget.trip.destination.position.longitude,
  );

  /// Points used for camera framing when no backend road route is available.
  List<LatLng> get _displayPoints =>
      _routePoints.length > 1 ? _routePoints : [_pickup, _destination];

  bool get _hasRoute => _routePoints.length > 1;

  double get _routeDistanceMeters => _distanceBetween(_pickup, _destination);

  bool get _usesDestinationFocus =>
      !_samePoint(_pickup, _destination) &&
      _routeDistanceMeters > _worldScaleRouteMeters;

  List<RideType> _activeTypes(RideTypeState state) {
    return List<RideType>.of(state.rideTypes.where((t) => t.active))
      ..sort((a, b) => a.sortOrder.compareTo(b.sortOrder));
  }

  @override
  void dispose() {
    _mapController?.dispose();
    super.dispose();
  }

  /// Kicks off per-type ETA + fare quotes once ride types are available.
  void _ensureQuotesStarted(List<RideType> types) {
    if (_quotesKickStarted) return;
    _quotesKickStarted = true;
    if (_samePoint(_pickup, _destination)) return;
    if (types.isEmpty) {
      _loadGenericRoute();
      return;
    }
    final first = selectPreferredRideType(types, widget.preferredRideTypeCode);
    _selectedType = first;
    context.read<RideTypeBloc>().add(SelectRideType(rideTypeId: first.id));
    // The selected route is the only map route needed. Fare comparisons load
    // in small batches instead of sending 30 route/fare requests at once.
    unawaited(_fetchEtaQuote(first, applyToMap: true));
    unawaited(_fetchFareQuote(first));
    unawaited(_loadOtherFares(types, first.code));
  }

  Future<void> _loadOtherFares(
    List<RideType> types,
    String selectedCode,
  ) async {
    final others = types.where((type) => type.code != selectedCode).toList();
    for (var index = 0; index < others.length && mounted; index += 3) {
      await Future.wait(others.skip(index).take(3).map(_fetchFareQuote));
    }
  }

  /// Fallback when ride types fail to load: generic ETA without a type.
  Future<void> _loadGenericRoute() async {
    try {
      final response = await sl<DioClient>().dio.post(
        RiderApiEndpoints().routeEtaEndpoint,
        data: {
          'rideId': 'preview',
          'driverId': 'preview',
          'driverLatitude': _pickup.latitude,
          'driverLongitude': _pickup.longitude,
          'destinationLatitude': _destination.latitude,
          'destinationLongitude': _destination.longitude,
        },
      );
      if (response.statusCode != 200) throw StateError('ETA unavailable');
      final data = response.data;
      final payload = data is Map ? data['data'] : null;
      if (payload is Map) {
        final poly = payload['polyline']?.toString() ?? '';
        final points = poly.isNotEmpty ? _decodePolyline(poly) : <LatLng>[];
        if (!mounted || points.length < 2) return;
        _applyRouteToMap(points);
      }
    } catch (_) {
      // Keep the two confirmed stops visible without drawing a fake road.
    }
  }

  /// ETA + route for one ride type. The backend picks the right routing
  /// (e.g. TWO_WHEELER for bikes) and returns the matching route + time.
  Future<void> _fetchEtaQuote(RideType type, {bool applyToMap = false}) async {
    final code = type.code;
    final quote = _quotes.putIfAbsent(code, () => _TypeQuote());
    if (quote.etaLoading) return;
    if (mounted) {
      setState(() {
        quote.etaLoading = true;
        quote.etaUnavailable = false;
      });
    }
    try {
      final response = await sl<DioClient>().dio.post(
        RiderApiEndpoints().routeEtaEndpoint,
        data: {
          'rideId': 'preview',
          'driverId': 'preview',
          'driverLatitude': _pickup.latitude,
          'driverLongitude': _pickup.longitude,
          'destinationLatitude': _destination.latitude,
          'destinationLongitude': _destination.longitude,
          'rideType': code,
        },
      );
      if (response.statusCode != 200) throw StateError('ETA unavailable');
      final data = response.data;
      final payload = data is Map ? data['data'] : null;
      if (payload is Map) {
        final poly = payload['polyline']?.toString() ?? '';
        final points = poly.isNotEmpty ? _decodePolyline(poly) : <LatLng>[];
        if (!mounted) return;
        setState(() {
          quote.routePoints = points;
          quote.etaMinutes = (payload['etaMinutes'] as num?)?.toInt();
          quote.distanceKm = (payload['distanceKm'] as num?)?.toDouble();
          quote.etaLoading = false;
          quote.etaUnavailable = quote.etaMinutes == null;
        });
        if (applyToMap && points.length > 1 && _selectedType?.code == code) {
          _applyRouteToMap(points);
        }
      } else if (mounted) {
        setState(() {
          quote.etaLoading = false;
          quote.etaUnavailable = true;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          quote.etaLoading = false;
          quote.etaUnavailable = true;
        });
      }
    }
  }

  /// Fare for one ride type (backend uses per-type rates).
  Future<void> _fetchFareQuote(RideType type) async {
    final code = type.code;
    final quote = _quotes.putIfAbsent(code, () => _TypeQuote());
    if (quote.fareLoading || quote.fare != null) return;
    if (mounted) {
      setState(() {
        quote.fareLoading = true;
        quote.fareUnavailable = false;
      });
    }
    try {
      final response = await sl<DioClient>().dio.post(
        RiderApiEndpoints().calculateEstimate,
        data: {
          'pickupLatitude': _pickup.latitude,
          'pickupLongitude': _pickup.longitude,
          'destinationLatitude': _destination.latitude,
          'destinationLongitude': _destination.longitude,
          'rideType': code,
        },
      );
      if (response.statusCode != 200) throw StateError('Fare unavailable');
      final data = response.data;
      final payload = data is Map ? data['data'] : null;
      double? fare;
      if (payload is Map) {
        final fareMap = payload['fare'];
        if (fareMap is Map) fare = (fareMap['totalFare'] as num?)?.toDouble();
      }
      if (!mounted) return;
      setState(() {
        quote.fare = fare;
        quote.fareLoading = false;
        quote.fareUnavailable = fare == null;
      });
    } catch (_) {
      if (mounted) {
        setState(() {
          quote.fareLoading = false;
          quote.fareUnavailable = true;
        });
      }
    }
  }

  void _selectType(RideType type) {
    if (_selectedType?.code == type.code) return;
    setState(() {
      _selectedType = type;
      _routePoints = const [];
      _routePolylines = {};
    });
    context.read<RideTypeBloc>().add(SelectRideType(rideTypeId: type.id));
    final quote = _quotes[type.code];
    final points = quote?.routePoints;
    if (points != null && points.length > 1) {
      _applyRouteToMap(points);
    } else {
      _fetchEtaQuote(type, applyToMap: true);
    }
    _fetchFareQuote(type);
  }

  void _applyRouteToMap(List<LatLng> points) {
    if (!mounted) return;
    setState(() {
      _routePoints = points;
      _routePolylines = {};
    });
    _fitTrip();
  }

  Future<void> _reviewRide() async {
    final rideType = _selectedType;
    if (rideType == null ||
        rideType.code.isEmpty ||
        _openingOptions ||
        context.read<RideBloc>().state.isLoading) {
      return;
    }
    _openingOptions = true;
    final completed = await Navigator.of(context).push<bool>(
      CupertinoPageRoute(
        builder: (_) => MultiBlocProvider(
          providers: [
            BlocProvider<UserBloc>.value(value: context.read<UserBloc>()),
            BlocProvider<RideTypeBloc>.value(
              value: context.read<RideTypeBloc>(),
            ),
            BlocProvider<OfferBloc>.value(value: context.read<OfferBloc>()),
            BlocProvider<FareBloc>.value(value: context.read<FareBloc>()),
            BlocProvider<RideBloc>.value(value: context.read<RideBloc>()),
          ],
          child: RideOptionsPage(
            trip: widget.trip,
            preferredRideTypeCode: rideType.code,
          ),
        ),
      ),
    );
    if (!mounted) return;
    _openingOptions = false;
    if (completed == true) Navigator.of(context).pop();
  }

  /// Google encoded-polyline decoder (no extra dependency).
  List<LatLng> _decodePolyline(String encoded) {
    final points = <LatLng>[];
    var index = 0;
    var lat = 0;
    var lng = 0;
    while (index < encoded.length) {
      var shift = 0;
      var result = 0;
      int b;
      do {
        b = encoded.codeUnitAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      final dLat = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
      lat += dLat;

      shift = 0;
      result = 0;
      do {
        b = encoded.codeUnitAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      final dLng = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
      lng += dLng;

      points.add(LatLng(lat / 1e5, lng / 1e5));
    }
    return points;
  }

  void _refreshRoutePolylines(bool isDark) {
    _routePolylinesDark = isDark;
    if (!_hasRoute) {
      _routePolylines = {};
      return;
    }
    final routePoints = _displayPoints;
    _routePolylines = {
      Polyline(
        polylineId: const PolylineId('trip-line-shadow'),
        points: routePoints,
        color: isDark ? Colors.black.withValues(alpha: 0.74) : Colors.white,
        width: 10,
        zIndex: 1,
        geodesic: true,
        startCap: Cap.roundCap,
        endCap: Cap.roundCap,
      ),
      Polyline(
        polylineId: const PolylineId('trip-line'),
        points: routePoints,
        color: Theme.of(context).colorScheme.primary,
        width: 6,
        zIndex: 2,
        geodesic: true,
        startCap: Cap.roundCap,
        endCap: Cap.roundCap,
      ),
    };
  }

  Future<void> _fitTrip() async {
    final controller = _mapController;
    if (controller == null) return;

    if (_samePoint(_pickup, _destination)) {
      await controller.animateCamera(CameraUpdate.newLatLngZoom(_pickup, 15));
      return;
    }

    if (_usesDestinationFocus && !_hasRoute) {
      await controller.animateCamera(
        CameraUpdate.newCameraPosition(
          CameraPosition(target: _destination, zoom: 13),
        ),
      );
      return;
    }

    try {
      await controller.animateCamera(
        CameraUpdate.newLatLngBounds(_boundsForPoints(_displayPoints), 56),
      );
    } catch (_) {
      await controller.animateCamera(
        CameraUpdate.newLatLngZoom(_destination, 13),
      );
    }
  }

  bool _samePoint(LatLng a, LatLng b) {
    return (a.latitude - b.latitude).abs() < 0.000001 &&
        (a.longitude - b.longitude).abs() < 0.000001;
  }

  LatLng _midpoint(LatLng a, LatLng b) {
    return LatLng(
      (a.latitude + b.latitude) / 2,
      (a.longitude + b.longitude) / 2,
    );
  }

  double _distanceBetween(LatLng a, LatLng b) {
    const earthRadiusMeters = 6371000.0;
    final dLat = _toRadians(b.latitude - a.latitude);
    final dLng = _toRadians(b.longitude - a.longitude);
    final startLat = _toRadians(a.latitude);
    final endLat = _toRadians(b.latitude);

    final value =
        math.sin(dLat / 2) * math.sin(dLat / 2) +
        math.cos(startLat) *
            math.cos(endLat) *
            math.sin(dLng / 2) *
            math.sin(dLng / 2);

    return 2 *
        earthRadiusMeters *
        math.atan2(math.sqrt(value), math.sqrt(1 - value));
  }

  double _toRadians(double degrees) => degrees * math.pi / 180;

  LatLngBounds _boundsForPoints(List<LatLng> points) {
    var minLat = points.first.latitude;
    var maxLat = points.first.latitude;
    var minLng = points.first.longitude;
    var maxLng = points.first.longitude;
    for (final p in points) {
      minLat = math.min(minLat, p.latitude);
      maxLat = math.max(maxLat, p.latitude);
      minLng = math.min(minLng, p.longitude);
      maxLng = math.max(maxLng, p.longitude);
    }
    return LatLngBounds(
      southwest: LatLng(minLat, minLng),
      northeast: LatLng(maxLat, maxLng),
    );
  }

  String _labelFor(String? value, String fallback) {
    final label = value?.trim() ?? '';
    return label.isEmpty ? fallback : label;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final pickupLabel = _labelFor(widget.trip.pickup.label, 'Pickup');
    final destinationLabel = _labelFor(
      widget.trip.destination.label,
      'Destination',
    );
    final cameraTarget = _usesDestinationFocus
        ? _destination
        : _midpoint(_pickup, _destination);
    final initialZoom = _samePoint(_pickup, _destination)
        ? 15.0
        : _usesDestinationFocus
        ? 13.0
        : 12.0;

    if ((_routePolylines.isEmpty && _hasRoute) ||
        _routePolylinesDark != isDark) {
      _refreshRoutePolylines(isDark);
    }

    return MediaQuery.removeViewInsets(
      context: context,
      removeBottom: true,
      child: Scaffold(
        backgroundColor: Theme.of(context).scaffoldBackgroundColor,
        resizeToAvoidBottomInset: false,
        body: Column(
          children: [
            SizedBox(
              height: (MediaQuery.sizeOf(context).height * 0.34).clamp(
                205.0,
                320.0,
              ),
              child: Stack(
                children: [
                  Positioned.fill(
                    child: GoogleMap(
                      initialCameraPosition: CameraPosition(
                        target: cameraTarget,
                        zoom: initialZoom,
                      ),
                      style: AppGoogleMapStyle.forBrightness(theme.brightness),
                      onMapCreated: (controller) async {
                        _mapController = controller;
                        await Future<void>.delayed(
                          const Duration(milliseconds: 250),
                        );
                        if (mounted) await _fitTrip();
                      },
                      markers: {
                        Marker(
                          markerId: const MarkerId('pickup'),
                          position: _pickup,
                          infoWindow: InfoWindow(title: pickupLabel),
                          icon: BitmapDescriptor.defaultMarkerWithHue(
                            BitmapDescriptor.hueGreen,
                          ),
                        ),
                        Marker(
                          markerId: const MarkerId('destination'),
                          position: _destination,
                          infoWindow: InfoWindow(title: destinationLabel),
                          icon: BitmapDescriptor.defaultMarkerWithHue(
                            BitmapDescriptor.hueRed,
                          ),
                        ),
                      },
                      polylines: _routePolylines,
                      myLocationButtonEnabled: false,
                      zoomControlsEnabled: false,
                      mapToolbarEnabled: false,
                      compassEnabled: false,
                    ),
                  ),
                  SafeArea(
                    bottom: false,
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
                      child: SwiftAppBar(
                        toolbarHeight: SwiftAppBar.toolbarHeightFor(context),
                        primary: false,
                        backgroundColor: theme.colorScheme.surface.withValues(
                          alpha: 0.94,
                        ),
                        foregroundColor: theme.colorScheme.onSurface,
                        automaticallyImplyLeading: false,
                        leading: SwiftAppBarAction(
                          icon: SwiftIcons.chevronLeft,
                          onPressed: () => Navigator.of(context).pop(),
                          tooltip: 'Back',
                        ),
                        title: Text(
                          'Choose your ride',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: theme.textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        actions: [
                          SwiftAppBarAction(
                            icon: SwiftIcons.locateFixed,
                            tooltip: 'Fit route',
                            onPressed: _fitTrip,
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: _bookingSheet(context, pickupLabel, destinationLabel),
            ),
          ],
        ),
      ),
    );
  }

  Widget _bookingSheet(
    BuildContext context,
    String pickupLabel,
    String destinationLabel,
  ) {
    final theme = Theme.of(context);
    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 0, 16, 10),
        child: GlassSurface(
          borderRadius: AppRadii.sheetRadius,
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 10),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _RouteSummaryRow(
                icon: SwiftIcons.locateFixed,
                iconColor: Theme.of(context).colorScheme.primary,
                label: pickupLabel,
              ),
              const SizedBox(height: 8),
              _RouteSummaryRow(
                icon: SwiftIcons.flag,
                iconColor: const Color(0xFFE53935),
                label: destinationLabel,
              ),
              const SizedBox(height: 10),
              Divider(color: theme.dividerColor, height: 1),
              const SizedBox(height: 10),
              Text(
                'Choose your ride',
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(height: 8),
              Expanded(
                child: BlocBuilder<RideTypeBloc, RideTypeState>(
                  builder: (context, state) {
                    final types = _activeTypes(state);
                    if (types.isNotEmpty) {
                      WidgetsBinding.instance.addPostFrameCallback(
                        (_) => _ensureQuotesStarted(types),
                      );
                    }
                    if ((state.status == RideTypeStatus.loading ||
                            state.status == RideTypeStatus.initial) &&
                        types.isEmpty) {
                      return SingleChildScrollView(
                        child: _rideTypeLoadingRow(theme),
                      );
                    }
                    if (types.isEmpty) {
                      return SingleChildScrollView(
                        child: _rideTypeEmptyRow(theme, state),
                      );
                    }
                    return ListView.separated(
                      shrinkWrap: true,
                      padding: EdgeInsets.zero,
                      itemCount: types.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 6),
                      itemBuilder: (context, index) {
                        final type = types[index];
                        final quote = _quotes[type.code];
                        return _rideTypeRow(
                          context,
                          type,
                          quote,
                          selected: _selectedType?.code == type.code,
                        );
                      },
                    );
                  },
                ),
              ),
              Divider(color: theme.dividerColor, height: 14),
              if (_selectedType != null) ...[
                Text('Fare estimate', style: theme.textTheme.bodySmall),
                Text(
                  _quotes[_selectedType!.code]?.fare != null
                      ? '₹${_quotes[_selectedType!.code]!.fare!.toStringAsFixed(0)}'
                      : _quotes[_selectedType!.code]?.fareUnavailable == true
                      ? 'Unavailable'
                      : 'Calculating…',
                  style: theme.textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 8),
              ],
              _bookButton(context),
            ],
          ),
        ),
      ),
    );
  }

  Widget _rideTypeLoadingRow(ThemeData theme) {
    return Semantics(
      label: 'Loading ride types',
      child: AppShimmer(
        child: Column(
          children: List.generate(
            2,
            (index) => Padding(
              padding: EdgeInsets.only(bottom: index == 0 ? 6 : 0),
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 9,
                ),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(
                    color: theme.dividerColor.withValues(alpha: 0.35),
                  ),
                ),
                child: const Row(
                  children: [
                    ShimmerBlock(width: 44, height: 44, radius: 12),
                    SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          ShimmerBlock(width: 110, height: 14),
                          SizedBox(height: 7),
                          ShimmerBlock(width: 150, height: 11),
                        ],
                      ),
                    ),
                    ShimmerBlock(width: 48, height: 16),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _rideTypeEmptyRow(ThemeData theme, RideTypeState state) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(
        children: [
          AppIcon(
            SwiftIcons.circleAlert,
            size: 18,
            color: theme.colorScheme.error,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              state.status == RideTypeStatus.failure
                  ? 'Vehicle types unavailable. Check your connection.'
                  : 'The vehicle catalog is empty. Try again.',
              style: theme.textTheme.bodyMedium,
            ),
          ),
          TextButton(
            onPressed: () =>
                context.read<RideTypeBloc>().add(const FetchRideTypes()),
            child: const Text('Retry'),
          ),
        ],
      ),
    );
  }

  Widget _rideTypeRow(
    BuildContext context,
    RideType type,
    _TypeQuote? quote, {
    required bool selected,
  }) {
    final theme = Theme.of(context);
    final etaText = quote?.etaLoading == true
        ? 'Trip time loading…'
        : quote?.etaMinutes != null
        ? 'Trip about ${quote!.etaMinutes} min'
        : quote?.etaUnavailable == true
        ? 'Trip time unavailable'
        : 'Trip time on selection';
    final fareText = quote == null || quote.fareLoading
        ? '…'
        : quote.fare != null
        ? '₹${quote.fare!.toStringAsFixed(0)}'
        : 'Unavailable';

    return Material(
      color: Colors.transparent,
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: () => _selectType(type),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            color: selected
                ? Theme.of(context).colorScheme.primary.withValues(alpha: 0.10)
                : Colors.transparent,
            border: Border.all(
              color: selected
                  ? Theme.of(
                      context,
                    ).colorScheme.primary.withValues(alpha: 0.65)
                  : theme.dividerColor.withValues(alpha: 0.35),
              width: selected ? 1.4 : 1,
            ),
          ),
          child: Row(
            children: [
              VehiclePhoto(code: type.code, width: 58, height: 44),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      type.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      etaText,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Text(
                fareText,
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _bookButton(BuildContext context) {
    final theme = Theme.of(context);
    final type = _selectedType;
    final quote = type != null ? _quotes[type.code] : null;
    return BlocBuilder<RideBloc, RideState>(
      builder: (context, rideState) {
        final label = rideState.isLoading
            ? 'Requesting ride…'
            : type == null
            ? 'Select a ride'
            : quote?.fareUnavailable == true
            ? 'Fare unavailable'
            : quote?.fare == null
            ? 'Calculating fare…'
            : 'Review fare & offers';
        final canBook =
            type != null && quote?.fare != null && !rideState.isLoading;
        return Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            SizedBox(
              width: double.infinity,
              height: 52,
              child: FilledButton(
                onPressed: canBook ? _reviewRide : null,
                style: FilledButton.styleFrom(
                  backgroundColor: Theme.of(context).colorScheme.primary,
                  disabledBackgroundColor:
                      theme.colorScheme.surfaceContainerHighest,
                  elevation: 0,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                ),
                child: rideState.isLoading
                    ? SizedBox(
                        width: 22,
                        height: 22,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      )
                    : Text(
                        label,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: theme.textTheme.titleMedium?.copyWith(
                          color: canBook
                              ? theme.colorScheme.onPrimary
                              : theme.colorScheme.onSurfaceVariant,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
              ),
            ),
            if (type != null && quote?.fareUnavailable == true)
              TextButton.icon(
                onPressed: () => _fetchFareQuote(type),
                icon: const AppIcon(SwiftIcons.refreshCw, size: 18),
                label: const Text('Retry fare estimate'),
              ),
            if (rideState.hasError && rideState.errorMessage != null)
              Padding(
                padding: const EdgeInsets.only(top: 6),
                child: Text(
                  rideState.errorMessage!,
                  textAlign: TextAlign.center,
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: theme.colorScheme.error,
                  ),
                ),
              ),
          ],
        );
      },
    );
  }
}

class _RouteSummaryRow extends StatelessWidget {
  final SwiftIcons icon;
  final Color iconColor;
  final String label;

  const _RouteSummaryRow({
    required this.icon,
    required this.iconColor,
    required this.label,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width: 30,
          height: 30,
          decoration: BoxDecoration(
            color: iconColor.withValues(alpha: 0.10),
            shape: BoxShape.circle,
          ),
          child: AppIcon(icon, size: 16, color: iconColor),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(
              context,
            ).textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w600),
          ),
        ),
      ],
    );
  }
}
