import 'dart:async';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/app_loading_page.dart';

import 'package:swift_app/features/common/shared/widgets/app_card.dart';

import 'package:swift_app/features/rider/home/presentation/widgets/google_map_style.dart';

import 'package:swift_app/features/rider/home/data/datasource/nearby_driver_datasource.dart';

import 'package:swift_app/API/dio_client.dart';

import 'package:swift_app/core/di/injection_container.dart';

import 'package:flutter/cupertino.dart';

import 'package:flutter/material.dart';

import 'package:geolocator/geolocator.dart';

import 'package:google_maps_flutter/google_maps_flutter.dart';

class GoogleMapWidget extends StatefulWidget {
  final bool active;

  final ValueChanged<Position>? onLocationChanged;

  final bool showNearbyDrivers;

  final NearbyDriverDatasource? nearbyDatasource;

  final bool showLoadingIndicator;

  final VoidCallback? onInitialLoadSettled;

  const GoogleMapWidget({
    super.key,

    this.active = true,

    this.onLocationChanged,

    this.showNearbyDrivers = false,

    this.nearbyDatasource,

    this.showLoadingIndicator = true,

    this.onInitialLoadSettled,
  });

  @override
  State<GoogleMapWidget> createState() => _GoogleMapWidgetState();
}

class _GoogleMapWidgetState extends State<GoogleMapWidget>
    with WidgetsBindingObserver {
  GoogleMapController? _mapController;

  StreamSubscription<Position>? _locationSubscription;

  Timer? _nearbyTimer;

  List<NearbyDriverPosition> _nearbyDrivers = const [];

  bool _nearbyLoading = false;

  bool _initialNearbyLoadSettled = false;

  DateTime? _lastNearbyRequest;

  LatLng? _lastNearbyCenter;

  LatLng? _currentLocation;

  bool _isLoading = true;

  bool _followUser = true;

  bool _initializing = false;

  bool _initialLoadSettled = false;

  bool _initialLoadNotified = false;

  bool _initialNotificationScheduled = false;

  bool _tickerEnabled = true;

  bool _foreground = true;

  DateTime? _lastCameraUpdate;

  bool get _active => widget.active && _tickerEnabled && _foreground;

  String? _locationMessage;

  bool _needsAppSettings = false;

  bool _needsLocationSettings = false;

  @override
  void initState() {
    super.initState();

    WidgetsBinding.instance.addObserver(this);

    _initializeLocation();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    _tickerEnabled = TickerMode.valuesOf(context).enabled;

    _syncLocationUpdates();
  }

  @override
  void didUpdateWidget(GoogleMapWidget oldWidget) {
    super.didUpdateWidget(oldWidget);

    if (oldWidget.onInitialLoadSettled != widget.onInitialLoadSettled) {
      _notifyInitialLoadSettled();
    }

    if (oldWidget.active != widget.active ||
        oldWidget.showNearbyDrivers != widget.showNearbyDrivers) {
      _syncLocationUpdates();
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    setState(() => _foreground = state == AppLifecycleState.resumed);

    _syncLocationUpdates();

    if (_active && (_currentLocation == null || _locationMessage != null)) {
      _initializeLocation();
    }
  }

  void _syncLocationUpdates() {
    if (!_active) {
      _locationSubscription?.cancel();

      _locationSubscription = null;

      _nearbyTimer?.cancel();

      _nearbyTimer = null;
    } else if (_currentLocation != null && _locationMessage == null) {
      _startLocationUpdates();

      if (widget.showNearbyDrivers) {
        _nearbyTimer ??= Timer.periodic(
          const Duration(seconds: 25),

          (_) => unawaited(_refreshNearby(force: true)),
        );

        unawaited(_refreshNearby());
      }
    }

    _settleInitialLoad();
  }

  Future<void> _initializeLocation() async {
    if (_initializing) {
      return;
    }

    _initializing = true;

    if (mounted) {
      setState(() {
        _isLoading = true;

        _locationMessage = null;

        _needsAppSettings = false;

        _needsLocationSettings = false;
      });
    }

    try {
      if (!await Geolocator.isLocationServiceEnabled()) {
        _setLocationError('Turn on Location Services.', locationSettings: true);

        return;
      }

      LocationPermission permission = await Geolocator.checkPermission();

      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }

      if (permission == LocationPermission.deniedForever) {
        _setLocationError(
          'Allow location access in Settings.',

          appSettings: true,
        );

        return;
      }

      if (permission == LocationPermission.denied) {
        _setLocationError('Location access is off.');

        return;
      }

      final Position position = await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.high,

          timeLimit: Duration(seconds: 20),
        ),
      );

      if (!mounted) return;

      setState(() {
        _currentLocation = LatLng(position.latitude, position.longitude);

        _isLoading = false;
      });

      widget.onLocationChanged?.call(position);

      _syncLocationUpdates();

      await _moveCamera();
    } catch (_) {
      _setLocationError('Couldn’t find your location.');
    } finally {
      _initializing = false;
    }
  }

  void _setLocationError(
    String message, {

    bool appSettings = false,

    bool locationSettings = false,
  }) {
    if (!mounted) return;

    setState(() {
      _isLoading = false;

      _locationMessage = message;

      _needsAppSettings = appSettings;

      _needsLocationSettings = locationSettings;
    });

    _settleInitialLoad(locationFailed: true);
  }

  void _settleInitialLoad({bool locationFailed = false}) {
    if (_initialLoadSettled) return;
    if (!locationFailed &&
        (_mapController == null ||
            (widget.showNearbyDrivers &&
                _active &&
                !_initialNearbyLoadSettled))) {
      return;
    }
    _initialLoadSettled = true;
    _notifyInitialLoadSettled();
  }

  void _notifyInitialLoadSettled() {
    if (!_initialLoadSettled ||
        _initialLoadNotified ||
        _initialNotificationScheduled ||
        widget.onInitialLoadSettled == null) {
      return;
    }
    _initialNotificationScheduled = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _initialNotificationScheduled = false;
      if (!mounted || _initialLoadNotified) return;
      final callback = widget.onInitialLoadSettled;
      if (callback == null) return;
      _initialLoadNotified = true;
      callback();
    });
    WidgetsBinding.instance.ensureVisualUpdate();
  }

  Future<void> _retryLocation() async {
    if (_needsAppSettings) {
      await Geolocator.openAppSettings();
    } else if (_needsLocationSettings) {
      await Geolocator.openLocationSettings();
    } else {
      await _initializeLocation();
    }
  }

  void _startLocationUpdates() {
    if (!_active || _locationSubscription != null) return;

    _locationSubscription =
        Geolocator.getPositionStream(
          locationSettings: const LocationSettings(
            accuracy: LocationAccuracy.high,

            distanceFilter: 10,
          ),
        ).listen(
          (Position position) {
            if (!mounted || !_active) return;

            // The native blue dot owns position rendering. Coordinates alone

            // must not rebuild the platform view on every GPS update.

            _currentLocation = LatLng(position.latitude, position.longitude);

            widget.onLocationChanged?.call(position);

            if (widget.showNearbyDrivers) unawaited(_refreshNearby());

            if (_locationMessage != null) {
              setState(() => _locationMessage = null);
            }

            if (_followUser) _moveCamera();
          },

          onError: (Object error) {
            _locationSubscription?.cancel();

            _locationSubscription = null;

            if (_active) _setLocationError('Location paused. Tap to retry.');
          },
        );
  }

  Future<void> _refreshNearby({bool force = false}) async {
    final point = _currentLocation;

    if (!widget.showNearbyDrivers ||
        !_active ||
        point == null ||
        _nearbyLoading) {
      return;
    }

    final now = DateTime.now();

    final previous = _lastNearbyCenter;

    final moved =
        previous == null ||
        (point.latitude - previous.latitude).abs() > 0.001 ||
        (point.longitude - previous.longitude).abs() > 0.001;

    if (!force &&
        !moved &&
        _lastNearbyRequest != null &&
        now.difference(_lastNearbyRequest!) < const Duration(seconds: 25)) {
      return;
    }

    _nearbyLoading = true;

    _lastNearbyRequest = now;

    _lastNearbyCenter = point;

    try {
      final datasource =
          widget.nearbyDatasource ??
          NearbyDriverDatasource(sl<DioClient>().dio);

      final nearby = await datasource.fetch(
        latitude: point.latitude,

        longitude: point.longitude,
      );

      if (mounted && _active) setState(() => _nearbyDrivers = nearby);
    } catch (_) {
      if (mounted && _active) {
        setState(
          () => _nearbyDrivers = _nearbyDrivers
              .where((driver) => driver.isFreshAt(DateTime.now()))
              .toList(),
        );
      }
    } finally {
      _nearbyLoading = false;
      _initialNearbyLoadSettled = true;
      if (mounted) _settleInitialLoad();
    }
  }

  Future<void> _moveCamera({bool recenter = false}) async {
    final LatLng? location = _currentLocation;

    final GoogleMapController? controller = _mapController;

    if (!_active || controller == null || location == null) {
      return;
    }

    final now = DateTime.now();

    if (!recenter &&
        _lastCameraUpdate != null &&
        now.difference(_lastCameraUpdate!) < const Duration(seconds: 1)) {
      return;
    }

    _lastCameraUpdate = now;

    final update = recenter
        ? CameraUpdate.newLatLngZoom(location, 16)
        : CameraUpdate.newLatLng(location);

    try {
      if (MediaQuery.disableAnimationsOf(context)) {
        await controller.moveCamera(update);
      } else {
        await controller.animateCamera(update);
      }
    } catch (_) {
      // The platform view can be disposed while a camera command is in flight.
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);

    _locationSubscription?.cancel();

    _nearbyTimer?.cancel();

    _mapController?.dispose();

    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);

    final LatLng? location = _currentLocation;

    if (location == null) {
      if (_isLoading) {
        return ColoredBox(
          color: theme.scaffoldBackgroundColor,
          child: widget.showLoadingIndicator
              ? const Center(child: AppLoadingProgress())
              : const SizedBox.expand(),
        );
      }
      return ColoredBox(
        color: theme.scaffoldBackgroundColor,

        child: Align(
          alignment: Alignment.topCenter,

          child: SingleChildScrollView(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(12, 24, 12, 12),

              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 420),

                child: _LocationStateCard(
                  loading: _isLoading,

                  message: _locationMessage,

                  openSettings: _needsAppSettings || _needsLocationSettings,

                  onRetry: _retryLocation,
                ),
              ),
            ),
          ),
        ),
      );
    }

    return Stack(
      children: [
        Listener(
          onPointerDown: (_) {
            _followUser = false;
          },

          child: GoogleMap(
            initialCameraPosition: CameraPosition(target: location, zoom: 16),

            style: AppGoogleMapStyle.forBrightness(theme.brightness),

            markers: widget.showNearbyDrivers
                ? _nearbyDrivers
                      .where((driver) => driver.isFreshAt(DateTime.now()))
                      .map(
                        (driver) => Marker(
                          markerId: MarkerId(
                            'nearby-driver-${driver.driverId}',
                          ),

                          position: driver.point,

                          icon: BitmapDescriptor.defaultMarkerWithHue(
                            BitmapDescriptor.hueGreen,
                          ),

                          infoWindow: InfoWindow(
                            title: driver.vehicleType ?? 'Nearby driver',
                          ),
                        ),
                      )
                      .toSet()
                : const {},

            onMapCreated: (controller) {
              if (!mounted) {
                controller.dispose();
                return;
              }

              setState(() => _mapController = controller);

              _settleInitialLoad();

              _moveCamera();
            },

            myLocationEnabled: _active,

            myLocationButtonEnabled: false,

            zoomControlsEnabled: false,

            compassEnabled: false,

            mapToolbarEnabled: false,
          ),
        ),

        if (_mapController == null && widget.showLoadingIndicator)
          Positioned.fill(
            child: ColoredBox(
              color: theme.scaffoldBackgroundColor,
              child: const Center(child: AppLoadingProgress()),
            ),
          ),

        if (_mapController != null)
          Positioned(
            top: MediaQuery.paddingOf(context).top + 12,

            right: 12,

            child: _MapGlassButton(
              tooltip: 'Recenter on my location',

              icon: SwiftIcons.locateFixed,

              onTap: () {
                _followUser = true;

                _moveCamera(recenter: true);
              },
            ),
          ),

        if (_locationMessage != null)
          Positioned(
            top: MediaQuery.paddingOf(context).top + 12,

            left: 12,

            right: 68,

            child: _MapMessage(
              message: _locationMessage!,

              onTap: _retryLocation,
            ),
          ),
      ],
    );
  }
}

class _MapGlassButton extends StatelessWidget {
  final SwiftIcons icon;

  final String tooltip;

  final VoidCallback onTap;

  const _MapGlassButton({
    required this.icon,

    required this.tooltip,

    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);

    // Keep map controls opaque and avoid repainting a backdrop filter.

    return ClipRRect(
      borderRadius: BorderRadius.circular(16),

      child: CupertinoButton(
        padding: EdgeInsets.zero,

        minimumSize: Size.zero,

        onPressed: onTap,

        child: Container(
          width: 46,

          height: 46,

          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),

            color: theme.colorScheme.surface,

            border: Border.all(color: theme.colorScheme.outlineVariant),
          ),

          child: AppIcon(
            icon,

            color: Theme.of(context).colorScheme.primary,

            size: 20,

            semanticLabel: tooltip,
          ),
        ),
      ),
    );
  }
}

class _MapMessage extends StatelessWidget {
  final String message;

  final VoidCallback onTap;

  const _MapMessage({required this.message, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);

    // No backdrop blur here: the fill is nearly opaque, and a sigma-18

    // filter directly over the live map is the most expensive backdrop.

    return ClipRRect(
      borderRadius: BorderRadius.circular(16),

      child: Material(
        color: theme.colorScheme.surface.withValues(alpha: 0.92),

        child: InkWell(
          onTap: onTap,

          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),

            child: Row(
              children: [
                AppIcon(
                  SwiftIcons.mapPin,

                  color: Theme.of(context).colorScheme.primary,

                  size: 17,
                ),

                const SizedBox(width: 8),

                Expanded(
                  child: Text(
                    message,

                    maxLines: 1,

                    overflow: TextOverflow.ellipsis,

                    style: theme.textTheme.bodySmall?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _LocationStateCard extends StatelessWidget {
  final bool loading;

  final String? message;

  final bool openSettings;

  final VoidCallback onRetry;

  const _LocationStateCard({
    required this.loading,

    required this.message,

    required this.openSettings,

    required this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);

    return AppCard(
      width: double.infinity,

      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),

      child: Row(
        children: [
          Container(
            width: 42,

            height: 42,

            decoration: BoxDecoration(
              shape: BoxShape.circle,

              color: theme.colorScheme.primary.withValues(alpha: 0.10),
            ),

            child: Center(
              child: loading
                  ? const CupertinoActivityIndicator(radius: 11)
                  : AppIcon(
                      SwiftIcons.locateFixed,

                      color: Theme.of(context).colorScheme.primary,

                      size: 22,
                    ),
            ),
          ),

          const SizedBox(width: 12),

          Expanded(
            child: Column(
              mainAxisSize: MainAxisSize.min,

              crossAxisAlignment: CrossAxisAlignment.start,

              children: [
                Text(
                  loading
                      ? 'Finding your location'
                      : message ?? 'Choose a location',

                  style: theme.textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                ),

                if (!loading) ...[
                  const SizedBox(height: 4),

                  TextButton(
                    onPressed: onRetry,

                    style: TextButton.styleFrom(
                      padding: EdgeInsets.zero,

                      minimumSize: Size.zero,

                      tapTargetSize: MaterialTapTargetSize.shrinkWrap,

                      alignment: Alignment.centerLeft,
                    ),

                    child: Text(
                      openSettings ? 'Open settings' : 'Try again',

                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}
