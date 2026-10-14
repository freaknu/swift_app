import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'dart:async';
import 'dart:math' as math;

import 'package:swift_app/constants/widgets/primary_button_widget.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';
import 'package:swift_app/features/rider/ride/domain/usecases/ride_lifecycle_usecase.dart';
import 'package:swift_app/features/rider/ride/data/datasource/rider_realtime_service.dart';
import 'package:swift_app/features/rider/ride/data/datasource/rider_location_datasource.dart';
import 'package:swift_app/features/rider/ride/data/datasource/rider_ride_extras_datasource.dart';
import 'package:swift_app/features/rider/ride/data/datasource/rider_route_datasource.dart';
import 'package:swift_app/features/rider/ride/domain/entity/driver_position_fix.dart';
import 'package:swift_app/features/rider/ride/data/repository/ride_repository_impl.dart';
import 'package:swift_app/features/common/chat/data/datasource/chat_realtime_service.dart';
import 'package:swift_app/features/common/chat/data/datasource/chat_remote_datasource.dart';
import 'package:swift_app/features/common/chat/domain/chat_availability.dart';
import 'package:swift_app/features/common/chat/presentation/pages/chat_page.dart';
import 'package:swift_app/features/common/payment/data/payment_remote_datasource.dart';
import 'package:swift_app/features/common/payment/data/razorpay_checkout.dart';
import 'package:swift_app/features/common/payment/presentation/ride_payment_card.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:geocoding/geocoding.dart' as geocoding;
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/google_map_style.dart';

/// Shows the rider's active ride and keeps it fresh while matching and
/// dispatch are happening. The backend currently exposes polling endpoints,
/// so this is deliberately resilient without requiring a second socket flow.
class RideStatusPage extends StatefulWidget {
  final RideResponse ride;
  final int riderId;
  final String? pickupLabel;
  final String? destinationLabel;
  final RideLifecycleUsecase? lifecycle;
  final Stream<Map<String, dynamic>>? realtimeEvents;
  final RiderLocationDatasource? locationDatasource;
  final RiderRideExtrasDatasource? extrasDatasource;
  final RiderRouteDatasource? routeDatasource;
  final Widget? mapOverride;
  final ChatRemoteService? chatDatasource;
  final ChatRealtimeTransport? chatRealtime;
  final PaymentRemoteService? paymentDatasource;
  final PaymentCheckout? paymentCheckout;

  const RideStatusPage({
    super.key,
    required this.ride,
    required this.riderId,
    this.pickupLabel,
    this.destinationLabel,
    this.lifecycle,
    this.realtimeEvents,
    this.locationDatasource,
    this.extrasDatasource,
    this.routeDatasource,
    this.mapOverride,
    this.chatDatasource,
    this.chatRealtime,
    this.paymentDatasource,
    this.paymentCheckout,
  });

  @override
  State<RideStatusPage> createState() => _RideStatusPageState();
}

class _RideStatusPageState extends State<RideStatusPage>
    with WidgetsBindingObserver, SingleTickerProviderStateMixin {
  late final RideLifecycleUsecase _lifecycle;
  Timer? _pollTimer;
  Timer? _positionTimer;
  late final AnimationController _driverAnimation;
  final ValueNotifier<LatLng?> _markerPosition = ValueNotifier(null);
  DateTime? _lastMarkerFrame;
  LatLng? _animationFrom;
  LatLng? _animationTo;
  bool _animateDriver = true;
  bool _foreground = true;
  DriverPositionFix? _latestDriverFix;
  RiderLocationDatasource get _locationDatasource =>
      widget.locationDatasource ?? RiderLocationDatasource(sl<DioClient>().dio);
  RiderRideExtrasDatasource? get _extrasDatasource =>
      widget.extrasDatasource ??
      (sl.isRegistered<DioClient>()
          ? RiderRideExtrasDatasource(sl<DioClient>().dio)
          : null);
  RiderRouteDatasource? get _routeDatasource =>
      widget.routeDatasource ??
      (widget.mapOverride == null && sl.isRegistered<DioClient>()
          ? RiderRouteDatasource(sl<DioClient>().dio)
          : null);
  bool _positionLoading = false;
  PaymentRemoteService? get _paymentDatasource =>
      widget.paymentDatasource ??
      (sl.isRegistered<DioClient>()
          ? PaymentRemoteDatasource(sl<DioClient>().dio)
          : null);
  bool _routeLoading = false;
  String? _routeLeg;
  DateTime? _lastRouteAttempt;
  LatLng? _lastRouteOrigin;
  List<LatLng> _roadRoute = const [];
  AssignedDriverSummary? _driverSummary;
  bool _summaryLoading = false;
  DateTime? _lastSummaryAttempt;
  MyRideRating? _myRating;
  bool _ratingLoading = false;
  bool _ratingLoaded = false;
  bool _ratingSubmitting = false;
  int _selectedRating = 0;
  String? _ratingError;
  late RideResponse _ride;
  bool _refreshing = false;
  bool _cancelling = false;
  String? _message;
  StreamSubscription<Map<String, dynamic>>? _realtimeSub;
  RiderRealtimeService? _realtimeService;
  String? _realtimeStatus;
  String? _matchFailureMessage;
  int? _searchAttempt;
  double? _searchRadiusKm;
  int? _driversNotified;
  GoogleMapController? _mapController;
  LatLng? _driverPosition;
  double? _driverDistanceToPickupMeters;
  int? _driverEtaMinutes;
  double? _driverDistanceToDestinationMeters;
  int? _driverDestinationEtaMinutes;
  bool _hasCenteredOnDriver = false;
  String? _pickupLabel;
  String? _destinationLabel;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _lifecycle = widget.lifecycle ?? sl<RideLifecycleUsecase>();
    _driverAnimation = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1100),
    )..addListener(_advanceDriverMarker);
    _ride = widget.ride;
    _pickupLabel = widget.pickupLabel;
    _destinationLabel = widget.destinationLabel;
    unawaited(_resolveAddressLabels());
    if (_ride.driverId != null) unawaited(_fetchDriverSummary());
    if (_status == 'COMPLETED') unawaited(_fetchMyRating());
    if (_status == 'COMPLETED') unawaited(_fetchRoadRoute());
    if (!_finished) {
      _pollTimer = Timer.periodic(
        const Duration(seconds: 5),
        (_) => _refreshRide(),
      );
      _positionTimer = Timer.periodic(
        const Duration(seconds: 15),
        (_) => unawaited(_fetchDriverPosition()),
      );
      unawaited(_fetchDriverPosition());
    }
    // Live matching progress over STOMP: the search radius the backend is
    // currently broadcasting to. The REST poll stays as the fallback.
    final rideId = _ride.id;
    if (rideId != null && (!_finished || _status == 'COMPLETED')) {
      final eventStream = widget.realtimeEvents;
      if (eventStream != null) {
        _realtimeSub = eventStream.listen(_onRealtimeEvent);
      } else if (sl.isRegistered<RiderRealtimeService>()) {
        _realtimeService = sl<RiderRealtimeService>();
        _realtimeService!.connect(rideId: rideId, riderId: widget.riderId);
        _realtimeSub = _realtimeService!.events.listen(_onRealtimeEvent);
      }
    }
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _animateDriver =
        !MediaQuery.disableAnimationsOf(context) &&
        TickerMode.valuesOf(context).enabled;
    if (!_animateDriver && _animationTo != null) {
      _driverAnimation.stop();
      _driverPosition = _animationTo;
      _markerPosition.value = _driverPosition;
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _realtimeSub?.cancel();
    _pollTimer?.cancel();
    _positionTimer?.cancel();
    _driverAnimation.dispose();
    _markerPosition.dispose();
    _realtimeService?.disconnect();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    _foreground = state == AppLifecycleState.resumed;
    if (!_foreground) {
      _driverAnimation.stop();
      return;
    }
    if (_finished && _status != 'COMPLETED') return;
    final rideId = _ride.id;
    if (rideId != null) {
      _realtimeService?.connect(rideId: rideId, riderId: widget.riderId);
    }
    if (_status == 'COMPLETED') return;
    unawaited(_refreshRide());
    unawaited(_fetchDriverPosition());
    unawaited(_fetchRoadRoute());
  }

  Future<void> _fetchDriverPosition() async {
    final rideId = _ride.id;
    if (_positionLoading ||
        rideId == null ||
        _finished ||
        !_foreground ||
        !mounted ||
        _ride.driverId == null) {
      return;
    }
    _positionLoading = true;
    try {
      final fix = await _locationDatasource.latestDriverPosition(rideId);
      if (mounted && fix != null) _applyDriverFix(fix);
    } catch (_) {
      // STOMP continues while a snapshot is unavailable; the stale indicator
      // below tells the rider when neither source has a recent position.
    } finally {
      _positionLoading = false;
      if (mounted && _latestDriverFix != null && !_positionFresh) {
        setState(() {});
      }
    }
  }

  Future<void> _fetchRoadRoute() async {
    final datasource = _routeDatasource;
    final rideId = _ride.id;
    final driverId = _ride.driverId;
    if (!mounted ||
        _routeLoading ||
        !_hasTripCoordinates ||
        datasource == null ||
        rideId == null ||
        driverId == null) {
      return;
    }
    final completed = _status == 'COMPLETED';
    final inTrip = _status == 'STARTED';
    if (!completed && !_positionFresh) return;
    final leg = completed
        ? 'receipt'
        : inTrip
        ? 'to-destination'
        : 'to-pickup';
    final origin = completed
        ? _point(_ride.pickupLatitude, _ride.pickupLongitude)
        : _latestDriverFix!.point;
    final destination = !completed && !inTrip
        ? _point(_ride.pickupLatitude, _ride.pickupLongitude)
        : _point(_ride.destinationLatitude, _ride.destinationLongitude);
    final now = DateTime.now();
    final sameLeg = _routeLeg == leg;
    if (sameLeg &&
        _lastRouteAttempt != null &&
        now.difference(_lastRouteAttempt!) < const Duration(seconds: 60) &&
        (completed ||
            (_lastRouteOrigin != null &&
                _metersBetween(_lastRouteOrigin!, origin) < 150))) {
      return;
    }
    _routeLoading = true;
    _routeLeg = leg;
    _lastRouteAttempt = now;
    _lastRouteOrigin = origin;
    if (!sameLeg) setState(() => _roadRoute = const []);
    try {
      final route = await datasource.fetch(
        rideId: rideId,
        driverId: driverId,
        origin: origin,
        destination: destination,
        rideTypeCode: _ride.rideTypeCode,
      );
      if (!mounted ||
          _ride.id != rideId ||
          _routeLeg != leg ||
          (completed != (_status == 'COMPLETED'))) {
        return;
      }
      setState(() => _roadRoute = route?.points ?? const []);
    } catch (_) {
      // An unavailable road route leaves the verified stops and live marker
      // visible. It must not be replaced with an invented straight line.
    } finally {
      _routeLoading = false;
      if (mounted &&
          (_status == 'COMPLETED' || _status == 'STARTED') &&
          _routeLeg !=
              (_status == 'COMPLETED' ? 'receipt' : 'to-destination')) {
        unawaited(_fetchRoadRoute());
      }
    }
  }

  double _metersBetween(LatLng a, LatLng b) {
    const earthRadiusMeters = 6371000.0;
    final latDelta = (b.latitude - a.latitude) * math.pi / 180;
    final lngDelta = (b.longitude - a.longitude) * math.pi / 180;
    final latA = a.latitude * math.pi / 180;
    final latB = b.latitude * math.pi / 180;
    final h =
        math.pow(math.sin(latDelta / 2), 2) +
        math.cos(latA) * math.cos(latB) * math.pow(math.sin(lngDelta / 2), 2);
    return 2 * earthRadiusMeters * math.asin(math.min(1, math.sqrt(h)));
  }

  Future<void> _fetchDriverSummary() async {
    final rideId = _ride.id;
    final datasource = _extrasDatasource;
    if (rideId == null ||
        _ride.driverId == null ||
        datasource == null ||
        _summaryLoading ||
        _driverSummary?.driverId == _ride.driverId) {
      return;
    }
    final now = DateTime.now();
    if (_lastSummaryAttempt != null &&
        now.difference(_lastSummaryAttempt!) < const Duration(seconds: 10)) {
      return;
    }
    _summaryLoading = true;
    _lastSummaryAttempt = now;
    try {
      final summary = await datasource.driverSummary(rideId);
      if (!mounted || summary == null || summary.driverId != _ride.driverId) {
        return;
      }
      setState(() => _driverSummary = summary);
    } catch (_) {
      // Keep ride controls usable when supplementary driver details lag.
    } finally {
      _summaryLoading = false;
    }
  }

  Future<void> _fetchMyRating() async {
    final rideId = _ride.id;
    final datasource = _extrasDatasource;
    if (rideId == null ||
        datasource == null ||
        _ratingLoading ||
        _status != 'COMPLETED') {
      return;
    }
    _ratingLoading = true;
    try {
      final rating = await datasource.myRating(rideId);
      if (!mounted) return;
      setState(() {
        _myRating = rating;
        _selectedRating = rating?.rate ?? _selectedRating;
        _ratingLoaded = true;
        _ratingError = null;
      });
    } catch (_) {
      if (mounted) setState(() => _ratingError = 'Rating unavailable. Retry.');
    } finally {
      _ratingLoading = false;
    }
  }

  Future<void> _submitRating() async {
    final rideId = _ride.id;
    final driverId = _ride.driverId;
    final datasource = _extrasDatasource;
    if (_ratingSubmitting ||
        _myRating != null ||
        _selectedRating == 0 ||
        rideId == null ||
        driverId == null ||
        datasource == null ||
        _status != 'COMPLETED') {
      return;
    }
    setState(() {
      _ratingSubmitting = true;
      _ratingError = null;
    });
    try {
      final rating = await datasource.rateDriver(
        rideId: rideId,
        driverId: driverId,
        rate: _selectedRating,
      );
      if (!mounted) return;
      setState(() {
        _myRating = rating;
        _ratingLoaded = rating != null;
        if (rating == null) _ratingError = 'Rating saved. Checking status…';
      });
    } catch (_) {
      if (mounted) {
        setState(() => _ratingError = 'Could not save rating. Retry.');
      }
    } finally {
      if (mounted) setState(() => _ratingSubmitting = false);
    }
  }

  bool get _positionFresh =>
      _latestDriverFix?.isFreshAt(DateTime.now()) ?? false;

  Future<void> _resolveAddressLabels() async {
    Future<String?> resolve(double? latitude, double? longitude) async {
      if (latitude == null ||
          longitude == null ||
          !latitude.isFinite ||
          !longitude.isFinite) {
        return null;
      }
      try {
        final geocoder = geocoding.Geocoding(locale: const Locale('en', 'IN'));
        final places = await geocoder
            .placemarkFromCoordinates(latitude, longitude)
            .timeout(const Duration(seconds: 8));
        if (places.isEmpty) return null;
        final place = places.first;
        final parts = [place.name, place.subLocality, place.locality]
            .whereType<String>()
            .map((part) => part.trim())
            .where((part) => part.isNotEmpty)
            .toSet();
        return parts.isEmpty ? null : parts.join(', ');
      } catch (_) {
        return null;
      }
    }

    if (_pickupLabel == null) {
      final label = await resolve(_ride.pickupLatitude, _ride.pickupLongitude);
      if (mounted && label != null) setState(() => _pickupLabel = label);
    }
    if (_destinationLabel == null) {
      final label = await resolve(
        _ride.destinationLatitude,
        _ride.destinationLongitude,
      );
      if (mounted && label != null) setState(() => _destinationLabel = label);
    }
  }

  String _displayLocation(
    String? label,
    double? latitude,
    double? longitude,
    String fallback,
  ) {
    final value = label?.trim();
    if (value != null && value.isNotEmpty) return value;
    if (latitude != null && longitude != null) {
      return '${latitude.toStringAsFixed(5)}, ${longitude.toStringAsFixed(5)}';
    }
    return fallback;
  }

  void _applyDriverFix(DriverPositionFix fix) {
    if (!mounted ||
        _finished ||
        _ride.id == null ||
        !fix.belongsTo(rideId: _ride.id!, driverId: _ride.driverId) ||
        !fix.isFreshAt(DateTime.now()) ||
        !fix.isNewerThan(_latestDriverFix)) {
      return;
    }
    _latestDriverFix = fix;
    _animationFrom = _driverPosition ?? fix.point;
    _animationTo = fix.point;
    if (!_animateDriver || !_foreground) {
      _driverAnimation.stop();
      _driverPosition = fix.point;
      _markerPosition.value = fix.point;
    } else {
      _driverAnimation.forward(from: 0);
    }
    setState(() {
      _driverPosition ??= fix.point;
      _driverDistanceToPickupMeters = fix.distanceToPickupMeters;
      _driverEtaMinutes = fix.etaMinutes;
      _driverDistanceToDestinationMeters = fix.distanceToDestinationMeters;
      _driverDestinationEtaMinutes = fix.destinationEtaMinutes;
      if (fix.driverArrived && _statusRank(_status) < 3) {
        _realtimeStatus = 'DRIVER_ARRIVED';
      }
    });
    if (!_hasCenteredOnDriver) unawaited(_centerOnLiveDriver());
    unawaited(_fetchRoadRoute());
  }

  void _advanceDriverMarker() {
    if (!mounted || _animationFrom == null || _animationTo == null) return;
    final now = DateTime.now();
    if (_driverAnimation.isAnimating &&
        _lastMarkerFrame != null &&
        now.difference(_lastMarkerFrame!) < const Duration(milliseconds: 50)) {
      return;
    }
    _lastMarkerFrame = now;
    final start = _animationFrom!;
    final end = _animationTo!;
    final t = Curves.easeOutCubic.transform(_driverAnimation.value);
    // Interpolation changes only the map marker. Keep the status sheet,
    // toolbar and their text layout out of these platform-view updates.
    _driverPosition = LatLng(
      start.latitude + (end.latitude - start.latitude) * t,
      start.longitude + (end.longitude - start.longitude) * t,
    );
    _markerPosition.value = _driverPosition;
  }

  void _onRealtimeEvent(Map<String, dynamic> event) {
    if (!mounted) return;
    final eventRideId = int.tryParse(event['rideId']?.toString() ?? '');
    if (eventRideId == null || eventRideId != _ride.id) return;
    final status = event['rideStatus']?.toString().toUpperCase();
    final fix = DriverPositionFix.tryParse(event);
    if (fix != null) _applyDriverFix(fix);
    if (_finished) return;
    if (status == 'SEARCHING') {
      setState(() {
        _searchAttempt = (event['attempt'] as num?)?.toInt();
        _searchRadiusKm = (event['radiusKm'] as num?)?.toDouble();
        _driversNotified = (event['driverCount'] as num?)?.toInt();
      });
    } else if (const {
      'DRIVER_ASSIGNED',
      'DRIVER_ARRIVED',
      'STARTED',
      'COMPLETED',
      'CANCELLED',
    }.contains(status)) {
      if (_statusRank(status!) < _statusRank(_status)) return;
      setState(() => _realtimeStatus = status);
      if (_finished) _stopLiveUpdates();
      if (status == 'STARTED' || status == 'COMPLETED') {
        unawaited(_fetchRoadRoute());
      }
      unawaited(_refreshRide());
    } else if (status == 'MATCH_FAILED') {
      final message = event['message']?.toString().trim();
      setState(() {
        _realtimeStatus = 'MATCH_FAILED';
        _matchFailureMessage = message != null && message.isNotEmpty
            ? message
            : null;
        _searchAttempt = null;
        _searchRadiusKm = null;
        _driversNotified = null;
        _message = null;
      });
      _stopLiveUpdates();
      unawaited(_refreshRide());
    }
  }

  void _stopLiveUpdates() {
    _pollTimer?.cancel();
    _positionTimer?.cancel();
    _driverAnimation.stop();
    // The completed receipt still needs PAYMENT_* events on this ride topic.
    if (_status == 'COMPLETED') return;
    _realtimeSub?.cancel();
    _realtimeSub = null;
    _realtimeService?.disconnect();
  }

  bool get _isSearching => _status == 'REQUESTED' || _status == 'MATCHING';

  String get _status =>
      (_realtimeStatus ?? _ride.currentStatus ?? 'MATCHING').toUpperCase();

  bool get _canCancel => const {
    'REQUESTED',
    'MATCHING',
    'DRIVER_ASSIGNED',
    'DRIVER_ARRIVED',
  }.contains(_status);

  bool get _canChat => ChatAvailability.isAvailable(_status);

  bool get _finished =>
      const {'COMPLETED', 'CANCELLED', 'MATCH_FAILED'}.contains(_status);

  Future<void> _refreshRide() async {
    final rideId = _ride.id;
    if (_refreshing || rideId == null || !mounted || !_foreground) return;
    _refreshing = true;
    try {
      final hadDriver = _ride.driverId != null;
      final updated = await _lifecycle.fetchRide(rideId);
      if (!mounted) return;
      setState(() {
        _ride = updated;
        final refreshedStatus = updated.currentStatus?.toUpperCase();
        if (refreshedStatus != null &&
            (_realtimeStatus == null ||
                (!_isTerminal(_realtimeStatus!) &&
                    _statusRank(refreshedStatus) >=
                        _statusRank(_realtimeStatus!)) ||
                (_isTerminal(_realtimeStatus!) &&
                    _isTerminal(refreshedStatus)))) {
          _realtimeStatus = refreshedStatus;
        }
        _message = null;
      });
      if (_finished) _stopLiveUpdates();
      if (!hadDriver && updated.driverId != null) {
        unawaited(_fetchDriverPosition());
      }
      if (updated.driverId != null) unawaited(_fetchDriverSummary());
      if (_status == 'COMPLETED' && !_ratingLoaded) {
        unawaited(_fetchMyRating());
      }
      if (_status == 'STARTED' || _status == 'COMPLETED') {
        unawaited(_fetchRoadRoute());
      }
    } catch (error) {
      if (!mounted) return;
      if (_finished) return;
      // A temporary gateway/network error should not throw the rider out of
      // the active journey. Keep the last known status visible instead, but
      // surface the backend's message when one was returned.
      final backendMessage = error is RideRepositoryException
          ? error.message
          : null;
      setState(
        () => _message = (backendMessage == null || backendMessage.isEmpty)
            ? 'Status will update when you are back online.'
            : backendMessage,
      );
    } finally {
      _refreshing = false;
    }
  }

  Future<void> _cancelRide() async {
    if (_cancelling || !_canCancel || _ride.id == null) return;
    final confirmed = await showCupertinoDialog<bool>(
      context: context,
      builder: (context) => CupertinoAlertDialog(
        title: const Text('Cancel this ride?'),
        content: const Text(
          'Your driver search will stop. You can request another ride later.',
        ),
        actions: [
          CupertinoDialogAction(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Keep ride'),
          ),
          CupertinoDialogAction(
            isDestructiveAction: true,
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Cancel ride'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    setState(() {
      _cancelling = true;
      _message = null;
    });
    try {
      final updated = await _lifecycle.cancelRide(
        rideId: _ride.id!,
        riderId: widget.riderId,
        reason: 'Cancelled by rider',
      );
      if (!mounted) return;
      setState(() => _ride = updated);
      _realtimeStatus = updated.currentStatus?.toUpperCase();
      _stopLiveUpdates();
    } catch (error) {
      if (mounted) setState(() => _message = error.toString());
    } finally {
      if (mounted) setState(() => _cancelling = false);
    }
  }

  String get _title => switch (_status) {
    'REQUESTED' || 'MATCHING' => 'Finding your driver',
    'DRIVER_ASSIGNED' => 'Driver is on the way',
    'DRIVER_ARRIVED' => 'Your driver has arrived',
    'STARTED' => 'Enjoy your ride',
    'COMPLETED' => 'Ride completed',
    'CANCELLED' => 'Ride cancelled',
    'MATCH_FAILED' => 'No drivers available',
    _ => 'Ride status',
  };

  String get _description => switch (_status) {
    'REQUESTED' || 'MATCHING' =>
      'We are checking nearby drivers. This usually takes a moment.',
    'DRIVER_ASSIGNED' =>
      'Your driver accepted the ride and is heading to pickup.',
    'DRIVER_ARRIVED' => 'Meet your driver at the pickup point.',
    'STARTED' => 'You are on the way to your destination.',
    'COMPLETED' => 'Thanks for riding with Swift.',
    'CANCELLED' => 'This ride is no longer active.',
    'MATCH_FAILED' =>
      _matchFailureMessage ??
          'No drivers are available right now. Please try again.',
    _ => 'We will keep this ride status up to date.',
  };

  Future<void> _openChat() async {
    final driverId = _ride.driverId;
    final rideId = _ride.id;
    if (!_canChat || driverId == null || rideId == null) return;
    await Navigator.of(context).push(
      CupertinoPageRoute(
        builder: (_) => ChatPage(
          rideId: rideId,
          currentUserId: widget.riderId,
          role: 'RIDER',
          rideStatus: _status,
          riderId: widget.riderId,
          driverId: driverId,
          datasource: widget.chatDatasource ?? sl<ChatRemoteDatasource>(),
          realtime: widget.chatRealtime ?? sl<ChatRealtimeService>(),
          rideStatusReader: () async {
            final updated = await _lifecycle.fetchRide(rideId);
            return updated.currentStatus;
          },
        ),
      ),
    );
  }

  int get _progress => switch (_status) {
    'REQUESTED' || 'MATCHING' => 1,
    'DRIVER_ASSIGNED' => 2,
    'DRIVER_ARRIVED' => 3,
    'STARTED' => 4,
    'COMPLETED' => 5,
    _ => 0,
  };

  int _statusRank(String status) => switch (status) {
    'REQUESTED' => 1,
    'MATCHING' => 1,
    'DRIVER_ASSIGNED' => 2,
    'DRIVER_ARRIVED' => 3,
    'STARTED' => 4,
    'COMPLETED' => 5,
    'CANCELLED' => 5,
    'MATCH_FAILED' => 5,
    _ => 0,
  };

  bool _isTerminal(String status) =>
      const {'COMPLETED', 'CANCELLED', 'MATCH_FAILED'}.contains(status);

  bool get _hasTripCoordinates {
    final values = [
      _ride.pickupLatitude,
      _ride.pickupLongitude,
      _ride.destinationLatitude,
      _ride.destinationLongitude,
    ];
    return values.every((value) => value != null && value.isFinite) &&
        _ride.pickupLatitude!.abs() <= 90 &&
        _ride.destinationLatitude!.abs() <= 90 &&
        _ride.pickupLongitude!.abs() <= 180 &&
        _ride.destinationLongitude!.abs() <= 180;
  }

  @override
  Widget build(BuildContext context) {
    if (_status == 'COMPLETED') return _receiptPage(context);
    final theme = Theme.of(context);
    final pickup = _point(_ride.pickupLatitude, _ride.pickupLongitude);
    final destination = _point(
      _ride.destinationLatitude,
      _ride.destinationLongitude,
    );
    final center = LatLng(
      (pickup.latitude + destination.latitude) / 2,
      (pickup.longitude + destination.longitude) / 2,
    );

    return Scaffold(
      backgroundColor: Colors.transparent,
      body: AppGlassBackground(
        child: SafeArea(
          bottom: false,
          child: Column(
            children: [
              Expanded(
                child: Stack(
                  children: [
                    Positioned.fill(
                      child:
                          widget.mapOverride ??
                          (_hasTripCoordinates
                              ? ValueListenableBuilder<LatLng?>(
                                  valueListenable: _markerPosition,
                                  builder: (context, _, _) => GoogleMap(
                                    initialCameraPosition: CameraPosition(
                                      target: center,
                                      zoom: 12,
                                    ),
                                    style: AppGoogleMapStyle.forBrightness(
                                      theme.brightness,
                                    ),
                                    onMapCreated: (controller) {
                                      _mapController = controller;
                                      if (_driverPosition != null) {
                                        unawaited(_centerOnLiveDriver());
                                      }
                                    },
                                    markers: _mapMarkers(pickup, destination),
                                    polylines: _roadPolylines(theme),
                                    zoomControlsEnabled: false,
                                    myLocationButtonEnabled: false,
                                    mapToolbarEnabled: false,
                                    compassEnabled: false,
                                  ),
                                )
                              : ColoredBox(
                                  color: theme.scaffoldBackgroundColor,
                                  child: const Center(
                                    child: Text('Route map is unavailable'),
                                  ),
                                )),
                    ),
                    Padding(
                      padding: const EdgeInsets.fromLTRB(12, 10, 12, 0),
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
                          onPressed: () => Navigator.of(context).maybePop(),
                          tooltip: 'Back',
                        ),
                        title: const Text('Ride status'),
                        actions: [
                          SwiftAppBarAction(
                            icon: SwiftIcons.refreshCw,
                            onPressed: _refreshing ? null : _refreshRide,
                            tooltip: 'Refresh status',
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              _statusSheet(theme),
            ],
          ),
        ),
      ),
    );
  }

  Widget _receiptPage(BuildContext context) {
    final theme = Theme.of(context);
    final fare = _ride.fareDetail?.totalFare;
    final currency = _ride.fareDetail?.currency?.toUpperCase();
    final fareText = fare == null
        ? '—'
        : currency == null || currency == 'INR'
        ? '₹${fare.toStringAsFixed(0)}'
        : '$currency ${fare.toStringAsFixed(2)}';
    final distance = _ride.actualDistance;
    final durationSeconds = _ride.actualDuration;
    final durationText = durationSeconds == null
        ? '—'
        : durationSeconds < 60
        ? '<1 min'
        : '${(durationSeconds / 60).round()} min';
    final pickup = _point(_ride.pickupLatitude, _ride.pickupLongitude);
    final destination = _point(
      _ride.destinationLatitude,
      _ride.destinationLongitude,
    );
    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      appBar: SwiftAppBar(
        toolbarHeight: SwiftAppBar.toolbarHeightFor(context),
        title: const Text('Trip receipt'),
        automaticallyImplyLeading: false,
        leading: SwiftAppBarAction(
          icon: SwiftIcons.chevronLeft,
          onPressed: () => Navigator.of(context).maybePop(),
          tooltip: 'Back',
        ),
      ),
      body: SafeArea(
        top: false,
        child: Column(
          children: [
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 18),
                children: [
                  const SizedBox(height: 10),
                  CircleAvatar(
                    radius: 28,
                    backgroundColor: theme.colorScheme.primary.withValues(
                      alpha: 0.12,
                    ),
                    child: AppIcon(
                      SwiftIcons.check,
                      color: theme.colorScheme.primary,
                      size: 24,
                    ),
                  ),
                  const SizedBox(height: 15),
                  Center(
                    child: Text(
                      'Ride completed',
                      style: theme.textTheme.headlineSmall?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ),
                  const SizedBox(height: 4),
                  Center(
                    child: Text(
                      'Thanks for riding with Swift.',
                      style: theme.textTheme.bodyMedium,
                    ),
                  ),
                  const SizedBox(height: 22),
                  Container(
                    decoration: BoxDecoration(
                      color: theme.colorScheme.surface,
                      border: Border.all(color: theme.dividerColor),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Column(
                      children: [
                        if (_hasTripCoordinates)
                          ClipRRect(
                            borderRadius: const BorderRadius.vertical(
                              top: Radius.circular(20),
                            ),
                            child: SizedBox(
                              height: 180,
                              child:
                                  widget.mapOverride ??
                                  GoogleMap(
                                    initialCameraPosition: CameraPosition(
                                      target: LatLng(
                                        (pickup.latitude +
                                                destination.latitude) /
                                            2,
                                        (pickup.longitude +
                                                destination.longitude) /
                                            2,
                                      ),
                                      zoom: 12,
                                    ),
                                    style: AppGoogleMapStyle.forBrightness(
                                      theme.brightness,
                                    ),
                                    markers: _mapMarkers(pickup, destination),
                                    polylines: _roadPolylines(theme),
                                    zoomControlsEnabled: false,
                                    myLocationButtonEnabled: false,
                                    mapToolbarEnabled: false,
                                  ),
                            ),
                          ),
                        Padding(
                          padding: const EdgeInsets.all(14),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              _routeSummary(theme),
                              const SizedBox(height: 16),
                              Row(
                                children: [
                                  Expanded(
                                    child: _ReceiptMetric(
                                      label: 'Fare',
                                      value: fareText,
                                    ),
                                  ),
                                  Expanded(
                                    child: _ReceiptMetric(
                                      label: 'Distance',
                                      value: distance == null
                                          ? '—'
                                          : '${distance.toStringAsFixed(1)} km',
                                    ),
                                  ),
                                  Expanded(
                                    child: _ReceiptMetric(
                                      label: 'Time',
                                      value: durationText,
                                    ),
                                  ),
                                ],
                              ),
                              if (_ride.driverId != null) ...[
                                const SizedBox(height: 16),
                                _AssignedDriverCard(
                                  driverId: _ride.driverId!,
                                  summary: _driverSummary,
                                ),
                              ],
                              const SizedBox(height: 8),
                              Text(
                                'Ride #${_ride.id ?? '—'}',
                                style: theme.textTheme.bodySmall,
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (_ride.id != null && _paymentDatasource != null) ...[
                    const SizedBox(height: 16),
                    RidePaymentCard(
                      key: ValueKey('payment-${_ride.id}'),
                      rideId: _ride.id!,
                      riderId: widget.riderId,
                      datasource: _paymentDatasource!,
                      checkout: widget.paymentCheckout,
                      events: widget.realtimeEvents ?? _realtimeService?.events,
                      connected: _realtimeService?.connected,
                    ),
                  ],
                  if (_ride.driverId != null && _extrasDatasource != null) ...[
                    const SizedBox(height: 16),
                    _ratingCard(theme),
                  ],
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
              child: AppPrimaryButton(
                text: 'Done',
                onPressed: () => Navigator.of(context).pop(false),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _ratingCard(ThemeData theme) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: theme.dividerColor),
      ),
      child: Column(
        children: [
          Text(
            _myRating == null ? 'Rate your driver' : 'Your driver rating',
            style: theme.textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 10),
          if (_ratingLoading && !_ratingLoaded)
            const CircularProgressIndicator()
          else
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: List.generate(
                5,
                (index) => IconButton(
                  tooltip: '${index + 1} stars',
                  onPressed: _myRating != null || !_ratingLoaded
                      ? null
                      : () => setState(() => _selectedRating = index + 1),
                  icon: AppIcon(
                    SwiftIcons.star,
                    color: index < _selectedRating
                        ? const Color(0xFFE9A615)
                        : theme.colorScheme.onSurfaceVariant,
                  ),
                ),
              ),
            ),
          if (_ratingError != null) ...[
            const SizedBox(height: 8),
            Text(
              _ratingError!,
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.error,
              ),
            ),
            if (!_ratingLoaded)
              TextButton(onPressed: _fetchMyRating, child: const Text('Retry')),
          ],
          if (_myRating == null && _ratingLoaded && _selectedRating > 0) ...[
            const SizedBox(height: 8),
            OutlinedButton(
              onPressed: _ratingSubmitting ? null : _submitRating,
              child: Text(_ratingSubmitting ? 'Saving…' : 'Submit rating'),
            ),
          ],
        ],
      ),
    );
  }

  Widget _statusSheet(ThemeData theme) {
    final scheme = theme.colorScheme;
    final viewport = MediaQuery.sizeOf(context);
    final insets = MediaQuery.paddingOf(context);
    // Keep the overlaid app bar clear even in landscape with enlarged text.
    // The sheet's content scrolls when the remaining space is smaller.
    final availableSheetHeight =
        viewport.height -
        insets.top -
        math.max(insets.bottom, 10) -
        10 -
        28 -
        (SwiftAppBar.toolbarHeightFor(context) + 20);
    return SafeArea(
      top: false,
      minimum: const EdgeInsets.fromLTRB(12, 10, 12, 10),
      child: GlassSurface(
        borderRadius: AppRadii.sheetRadius,
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
        child: ConstrainedBox(
          constraints: BoxConstraints(
            maxHeight: math.max(
              0,
              math.min(viewport.height * 0.60, availableSheetHeight),
            ),
          ),
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  children: [
                    Container(
                      width: 44,
                      height: 44,
                      decoration: BoxDecoration(
                        color: scheme.primary.withValues(alpha: 0.12),
                        shape: BoxShape.circle,
                      ),
                      child:
                          _isSearching &&
                              !MediaQuery.disableAnimationsOf(context)
                          ? const Padding(
                              padding: EdgeInsets.all(10),
                              child: CircularProgressIndicator(strokeWidth: 3),
                            )
                          : AppIcon(
                              _status == 'MATCH_FAILED'
                                  ? SwiftIcons.circleAlert
                                  : _finished
                                  ? SwiftIcons.circleCheck
                                  : SwiftIcons.car,
                              color: _status == 'MATCH_FAILED'
                                  ? scheme.error
                                  : scheme.primary,
                            ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            _title,
                            style: theme.textTheme.headlineSmall?.copyWith(
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                          const SizedBox(height: 3),
                          Text(_description, style: theme.textTheme.bodySmall),
                        ],
                      ),
                    ),
                  ],
                ),
                if (_isSearching && _searchRadiusKm != null) ...[
                  const SizedBox(height: 12),
                  _SearchProgress(
                    radiusKm: _searchRadiusKm!,
                    attempt: _searchAttempt,
                    driversNotified: _driversNotified,
                  ),
                ],
                if (_ride.driverId != null && !_finished) ...[
                  const SizedBox(height: 12),
                  _AssignedDriverCard(
                    driverId: _ride.driverId!,
                    summary: _driverSummary,
                  ),
                ],
                if (_driverPosition != null &&
                    _positionFresh &&
                    !_finished) ...[
                  const SizedBox(height: 12),
                  _LiveDriverCard(
                    etaMinutes: _liveEtaMinutes,
                    distanceMeters: _liveDistanceMeters,
                    arrived: _status == 'DRIVER_ARRIVED',
                    onTrip: _status == 'STARTED',
                  ),
                ],
                if (_ride.driverId != null &&
                    !_positionFresh &&
                    !_finished) ...[
                  const SizedBox(height: 12),
                  Text(
                    'Updating your driver’s location…',
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
                const SizedBox(height: 16),
                _routeSummary(theme),
                if (!_isSearching && _status != 'MATCH_FAILED') ...[
                  const SizedBox(height: 14),
                  _ProgressLine(progress: _progress),
                ],
                if (_message != null) ...[
                  const SizedBox(height: 10),
                  Text(
                    _message!,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: scheme.error,
                    ),
                  ),
                ],
                const SizedBox(height: 14),
                AppPrimaryButton(
                  text: _status == 'MATCH_FAILED'
                      ? 'Try another ride'
                      : _finished
                      ? 'Back to Swift'
                      : _isSearching
                      ? 'Cancel request'
                      : _canChat && _ride.driverId != null
                      ? 'Chat with driver'
                      : 'Refresh status',
                  onPressed: _finished
                      ? () =>
                            Navigator.of(context).pop(_status == 'MATCH_FAILED')
                      : _isSearching
                      ? _cancelling
                            ? null
                            : _cancelRide
                      : _canChat && _ride.driverId != null
                      ? _openChat
                      : _refreshRide,
                  loading: _refreshing || _cancelling,
                ),
                if (_status == 'MATCH_FAILED')
                  TextButton(
                    onPressed: () => Navigator.of(context).pop(false),
                    child: const Text('Back to Swift'),
                  ),
                if (_canCancel && !_isSearching) ...[
                  const SizedBox(height: 4),
                  TextButton(
                    onPressed: _cancelling ? null : _cancelRide,
                    child: Text(_cancelling ? 'Cancelling…' : 'Cancel ride'),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _routeSummary(ThemeData theme) {
    final fare = _ride.fareDetail?.totalFare;
    final currency = _ride.fareDetail?.currency?.toUpperCase();
    final fareLabel = fare == null
        ? null
        : currency == null || currency == 'INR'
        ? '₹${fare.toStringAsFixed(0)}'
        : '$currency ${fare.toStringAsFixed(2)}';
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 13, 14, 12),
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: theme.dividerColor),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _RouteStop(
            icon: SwiftIcons.circle,
            color: theme.colorScheme.primary,
            label: 'Pickup',
            value: _displayLocation(
              _pickupLabel,
              _ride.pickupLatitude,
              _ride.pickupLongitude,
              'Pickup location unavailable',
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(5, 5, 0, 5),
            child: Container(
              width: 2,
              height: 12,
              alignment: Alignment.centerLeft,
              color: theme.dividerColor,
            ),
          ),
          _RouteStop(
            icon: SwiftIcons.mapPin,
            color: theme.colorScheme.error,
            label: 'Destination',
            value: _displayLocation(
              _destinationLabel,
              _ride.destinationLatitude,
              _ride.destinationLongitude,
              'Destination unavailable',
            ),
          ),
          if (fareLabel != null || _ride.rideTypeCode != null) ...[
            Divider(height: 22, color: theme.dividerColor),
            Row(
              children: [
                if (_ride.rideTypeCode != null)
                  Expanded(
                    child: Text(
                      _ride.rideTypeCode!.replaceAll('_', ' '),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                if (fareLabel != null)
                  Text(
                    fareLabel,
                    style: theme.textTheme.titleMedium?.copyWith(
                      color: theme.colorScheme.primary,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  LatLng _point(double? latitude, double? longitude) {
    return LatLng(latitude ?? 0, longitude ?? 0);
  }

  Set<Marker> _mapMarkers(LatLng pickup, LatLng destination) {
    final markers = <Marker>{
      Marker(
        markerId: const MarkerId('status-pickup'),
        position: pickup,
        icon: BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueGreen),
      ),
      Marker(
        markerId: const MarkerId('status-destination'),
        position: destination,
        icon: BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueRed),
      ),
    };
    final driver = _positionFresh ? _driverPosition : null;
    if (driver != null) {
      markers.add(
        Marker(
          markerId: const MarkerId('live-driver'),
          position: driver,
          icon: BitmapDescriptor.defaultMarkerWithHue(
            BitmapDescriptor.hueAzure,
          ),
          infoWindow: InfoWindow(
            title: _status == 'DRIVER_ARRIVED'
                ? 'Driver has arrived'
                : 'Your driver',
            snippet: _liveEtaLabel,
          ),
        ),
      );
    }
    return markers;
  }

  Set<Polyline> _roadPolylines(ThemeData theme) {
    final expectedLeg = _status == 'COMPLETED'
        ? 'receipt'
        : _status == 'STARTED'
        ? 'to-destination'
        : 'to-pickup';
    if ((_finished && _status != 'COMPLETED') ||
        _roadRoute.length < 2 ||
        _routeLeg != expectedLeg) {
      return const {};
    }
    return {
      Polyline(
        polylineId: const PolylineId('verified-road-route'),
        points: _roadRoute,
        color: theme.colorScheme.primary,
        width: 5,
      ),
    };
  }

  double? get _liveDistanceMeters {
    if (!_positionFresh) return null;
    if (_status == 'STARTED' && _driverDistanceToDestinationMeters != null) {
      return _driverDistanceToDestinationMeters;
    }
    if (_driverDistanceToPickupMeters != null) {
      return _driverDistanceToPickupMeters;
    }
    return null;
  }

  int? get _liveEtaMinutes {
    if (!_positionFresh) return null;
    if (_status == 'DRIVER_ARRIVED') return 0;
    if (_status == 'STARTED' && _driverDestinationEtaMinutes != null) {
      return _driverDestinationEtaMinutes;
    }
    if (_status != 'STARTED' && _driverEtaMinutes != null) {
      return _driverEtaMinutes;
    }
    return null;
  }

  String get _liveEtaLabel {
    final eta = _liveEtaMinutes;
    if (_status == 'DRIVER_ARRIVED' || eta == 0) return 'At pickup';
    if (eta == null) return 'Calculating arrival time';
    return '$eta min away';
  }

  Future<void> _centerOnLiveDriver() async {
    final controller = _mapController;
    final driver = _driverPosition;
    if (controller == null ||
        driver == null ||
        _hasCenteredOnDriver ||
        !_hasTripCoordinates) {
      return;
    }
    final pickup = _point(_ride.pickupLatitude, _ride.pickupLongitude);
    final south = math.min(driver.latitude, pickup.latitude);
    final north = math.max(driver.latitude, pickup.latitude);
    final west = math.min(driver.longitude, pickup.longitude);
    final east = math.max(driver.longitude, pickup.longitude);
    try {
      await controller.animateCamera(
        CameraUpdate.newLatLngBounds(
          LatLngBounds(
            southwest: LatLng(
              south - (south == north ? 0.002 : 0),
              west - (west == east ? 0.002 : 0),
            ),
            northeast: LatLng(
              north + (south == north ? 0.002 : 0),
              east + (west == east ? 0.002 : 0),
            ),
          ),
          70,
        ),
      );
      _hasCenteredOnDriver = true;
    } catch (_) {
      // The map may not have laid out yet; the marker will still render and
      // the next location fix can try again.
    }
  }
}

class _RouteStop extends StatelessWidget {
  final SwiftIcons icon;
  final Color color;
  final String label;
  final String value;

  const _RouteStop({
    required this.icon,
    required this.color,
    required this.label,
    required this.value,
  });

  @override
  Widget build(BuildContext context) => Row(
    children: [
      AppIcon(icon, color: color, size: 16),
      const SizedBox(width: 12),
      Expanded(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label, style: Theme.of(context).textTheme.labelSmall),
            Text(
              value,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(
                context,
              ).textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w700),
            ),
          ],
        ),
      ),
    ],
  );
}

class _ReceiptMetric extends StatelessWidget {
  final String label;
  final String value;

  const _ReceiptMetric({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: theme.textTheme.bodySmall),
        const SizedBox(height: 4),
        Text(
          value,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: theme.textTheme.titleSmall?.copyWith(
            fontWeight: FontWeight.w800,
          ),
        ),
      ],
    );
  }
}

class _LiveDriverCard extends StatelessWidget {
  final int? etaMinutes;
  final double? distanceMeters;
  final bool arrived;
  final bool onTrip;

  const _LiveDriverCard({
    required this.etaMinutes,
    required this.distanceMeters,
    required this.arrived,
    required this.onTrip,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final distance = distanceMeters == null
        ? '—'
        : distanceMeters! >= 1000
        ? '${(distanceMeters! / 1000).toStringAsFixed(1)} km'
        : '${distanceMeters!.round()} m';
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: scheme.primary.withValues(alpha: 0.09),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: scheme.primary.withValues(alpha: 0.18)),
      ),
      child: Row(
        children: [
          AppIcon(
            arrived
                ? SwiftIcons.circleCheck
                : onTrip
                ? SwiftIcons.circleArrowRight
                : SwiftIcons.mapPin,
            color: scheme.primary,
            size: 21,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  arrived
                      ? 'Driver at pickup'
                      : onTrip
                      ? 'You are on the way'
                      : 'Driver is approaching',
                  style: Theme.of(context).textTheme.labelLarge,
                ),
                const SizedBox(height: 2),
                Text(
                  arrived
                      ? 'Ready when you are'
                      : onTrip
                      ? '$distance from destination'
                      : '$distance from pickup',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          ),
          Text(
            arrived
                ? 'Here'
                : etaMinutes == null
                ? '…'
                : '$etaMinutes min',
            style: Theme.of(context).textTheme.titleSmall?.copyWith(
              color: scheme.primary,
              fontWeight: FontWeight.w800,
            ),
          ),
        ],
      ),
    );
  }
}

class _AssignedDriverCard extends StatelessWidget {
  final int driverId;
  final AssignedDriverSummary? summary;

  const _AssignedDriverCard({required this.driverId, required this.summary});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final detail = summary;
    final name = detail?.driverName ?? 'Driver #$driverId';
    final vehicle = [
      detail?.brand,
      detail?.model,
    ].whereType<String>().where((value) => value.isNotEmpty).join(' ');
    final subtitle = [
      if (vehicle.isNotEmpty) vehicle else detail?.vehicleType,
      detail?.registrationNumber,
    ].whereType<String>().where((value) => value.isNotEmpty).join(' · ');
    final imageUri = Uri.tryParse(detail?.profileImage ?? '');
    final imageUrl =
        imageUri != null &&
            (imageUri.scheme == 'https' || imageUri.scheme == 'http')
        ? imageUri.toString()
        : null;
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: theme.dividerColor),
      ),
      child: Row(
        children: [
          CircleAvatar(
            radius: 22,
            backgroundImage: imageUrl == null ? null : NetworkImage(imageUrl),
            child: imageUrl == null
                ? const AppIcon(SwiftIcons.userRound)
                : null,
          ),
          const SizedBox(width: 11),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  name,
                  style: theme.textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w800,
                  ),
                ),
                Text(
                  subtitle.isEmpty ? 'Vehicle details updating…' : subtitle,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.bodySmall,
                ),
              ],
            ),
          ),
          const AppIcon(SwiftIcons.car, size: 20),
        ],
      ),
    );
  }
}

/// Live "finding your driver" indicator: the search radius the matching
/// service is currently broadcasting to, widening until a driver accepts.
class _SearchProgress extends StatefulWidget {
  final double radiusKm;
  final int? attempt;
  final int? driversNotified;

  const _SearchProgress({
    required this.radiusKm,
    this.attempt,
    this.driversNotified,
  });

  @override
  State<_SearchProgress> createState() => _SearchProgressState();
}

class _SearchProgressState extends State<_SearchProgress>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1600),
    );
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (MediaQuery.disableAnimationsOf(context)) {
      _controller.stop();
    } else if (!_controller.isAnimating) {
      _controller.repeat();
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final label = StringBuffer(
      'Searching within ${widget.radiusKm.toStringAsFixed(0)} km',
    );
    if (widget.driversNotified != null) {
      label.write(' \u00b7 ${widget.driversNotified} drivers notified');
    }
    return Row(
      children: [
        SizedBox(
          width: 34,
          height: 34,
          child: RepaintBoundary(
            child: CustomPaint(
              painter: _PulsePainter(
                animation: _controller,
                reducedMotion: MediaQuery.disableAnimationsOf(context),
                color: scheme.primary,
              ),
            ),
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label.toString(), style: theme.textTheme.bodySmall),
              if (widget.attempt != null && widget.attempt! > 1)
                Text(
                  'Widening search \u00b7 attempt ${widget.attempt}',
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: scheme.onSurfaceVariant,
                  ),
                ),
            ],
          ),
        ),
      ],
    );
  }
}

class _PulsePainter extends CustomPainter {
  final Animation<double> animation;
  final bool reducedMotion;
  final Color color;

  _PulsePainter({
    required this.animation,
    required this.reducedMotion,
    required this.color,
  }) : super(repaint: reducedMotion ? null : animation);

  @override
  void paint(Canvas canvas, Size size) {
    final progress = reducedMotion ? 0.5 : animation.value;
    final center = Offset(size.width / 2, size.height / 2);
    final ringRadius = 4 + progress * (size.width / 2 - 4);
    canvas.drawCircle(
      center,
      ringRadius,
      Paint()
        ..color = color.withValues(alpha: (1 - progress) * 0.5)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2,
    );
    canvas.drawCircle(center, 4, Paint()..color = color);
  }

  @override
  bool shouldRepaint(covariant _PulsePainter oldDelegate) =>
      oldDelegate.animation != animation ||
      oldDelegate.reducedMotion != reducedMotion ||
      oldDelegate.color != color;
}

class _ProgressLine extends StatelessWidget {
  final int progress;

  const _ProgressLine({required this.progress});

  @override
  Widget build(BuildContext context) {
    final color = Theme.of(context).colorScheme.primary;
    const labels = ['Requested', 'Matched', 'Arrived', 'On trip', 'Done'];
    return Row(
      children: [
        for (var index = 0; index < labels.length; index++) ...[
          Expanded(
            child: Column(
              children: [
                Container(
                  width: 12,
                  height: 12,
                  decoration: BoxDecoration(
                    color: index < progress
                        ? color
                        : color.withValues(alpha: 0.15),
                    shape: BoxShape.circle,
                  ),
                ),
                const SizedBox(height: 5),
                Text(
                  labels[index],
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.labelSmall,
                ),
              ],
            ),
          ),
          if (index < labels.length - 1)
            Expanded(
              child: Container(
                height: 2,
                margin: const EdgeInsets.only(bottom: 18),
                color: index + 1 < progress
                    ? color
                    : color.withValues(alpha: 0.12),
              ),
            ),
        ],
      ],
    );
  }
}
