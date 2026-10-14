import 'dart:async';

import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/driver/data/datasource/driver_realtime_service.dart';
import 'package:geolocator/geolocator.dart';

/// Shares the foreground driver's location while they are available for rides.
/// The first position is sent before [start] reports success, so going online
/// does not leave the driver invisible to matching until the next GPS update.
class DriverLocationPublisher {
  DriverLocationPublisher({
    required DriverRemoteDatasource datasource,
    required DriverRealtimeService realtimeService,
  }) : _datasource = datasource,
       _realtimeService = realtimeService;

  final DriverRemoteDatasource _datasource;
  final DriverRealtimeService _realtimeService;
  final StreamController<String> _errors = StreamController<String>.broadcast();

  StreamSubscription<Position>? _positionSubscription;
  Timer? _heartbeatTimer;
  Position? _lastPosition;
  DateTime? _lastSentAt;
  int? _driverId;
  int? _rideId;
  Future<void> _sendTail = Future<void>.value();
  String? _lastError;

  Stream<String> get errors => _errors.stream;
  bool get isRunning => _positionSubscription != null;

  Future<String?> start({required int driverId, int? rideId}) async {
    if (_driverId == driverId && _positionSubscription != null) {
      if (_rideId != rideId) await setRideId(rideId);
      return null;
    }

    await stop();
    _driverId = driverId;
    _rideId = rideId;

    try {
      if (!await Geolocator.isLocationServiceEnabled()) {
        await stop();
        return 'Turn on Location Services to receive nearby rides.';
      }

      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever) {
        await stop();
        return 'Allow location access to receive nearby rides.';
      }

      final firstPosition = await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.high,
          timeLimit: Duration(seconds: 15),
        ),
      );
      _lastPosition = firstPosition;
      final error = await _publish(firstPosition);
      if (error != null) {
        await stop();
        return error;
      }

      _positionSubscription =
          Geolocator.getPositionStream(
            locationSettings: const LocationSettings(
              accuracy: LocationAccuracy.high,
              distanceFilter: 15,
            ),
          ).listen(
            (position) {
              _lastPosition = position;
              final lastSentAt = _lastSentAt;
              if (lastSentAt == null ||
                  DateTime.now().difference(lastSentAt) >=
                      const Duration(seconds: 8)) {
                unawaited(_publish(position));
              }
            },
            onError: (Object error) => _reportError(
              'Location updates stopped. Check location permission and try again.',
            ),
          );
      // Matching considers a stationary driver's last fix stale after a
      // minute, so refresh presence while online even without movement.
      _heartbeatTimer = Timer.periodic(const Duration(seconds: 20), (_) {
        final position = _lastPosition;
        if (position != null) unawaited(_publish(position));
      });
      return null;
    } catch (_) {
      await stop();
      return 'Could not get your location. Check GPS and try again.';
    }
  }

  /// Marks tracking as part of a ride, removing this driver from the available
  /// location index. Passing null publishes them as available again.
  Future<void> setRideId(int? rideId) async {
    if (_rideId == rideId) return;
    _rideId = rideId;
    final position = _lastPosition;
    if (position != null && _driverId != null) {
      await _publish(position);
    }
  }

  Future<void> stop({bool publishFinalPosition = false}) async {
    final subscription = _positionSubscription;
    _positionSubscription = null;
    _heartbeatTimer?.cancel();
    _heartbeatTimer = null;
    await subscription?.cancel();
    final driverId = _driverId;
    final position = _lastPosition;
    if (publishFinalPosition && driverId != null && position != null) {
      // Call after the status endpoint changes to OFFLINE. The backend then
      // emits an INACTIVE location event so matching can remove this driver.
      await _publish(position);
    }
    await _sendTail;
    _driverId = null;
    _rideId = null;
    _lastPosition = null;
    _lastSentAt = null;
    _lastError = null;
  }

  Future<void> dispose() async {
    await stop();
    await _errors.close();
  }

  Future<String?> _publish(Position position) {
    final send = _sendTail.then((_) => _sendPosition(position));
    _sendTail = send.then<void>((_) {});
    return send;
  }

  Future<String?> _sendPosition(Position position) async {
    final driverId = _driverId;
    if (driverId == null) return null;
    try {
      final rideId = _rideId;
      if (rideId != null) {
        final sent = await _realtimeService.publishLocation(
          driverId: driverId,
          rideId: rideId,
          latitude: position.latitude,
          longitude: position.longitude,
        );
        if (!sent) {
          // Keep the ride moving for the rider when STOMP is reconnecting.
          // The authenticated location endpoint verifies ride ownership and
          // forwards the fix to the rider's realtime topic.
          final fallback = await _datasource.publishDriverLocation(
            driverId: driverId,
            rideId: rideId,
            latitude: position.latitude,
            longitude: position.longitude,
          );
          if (!fallback.success || fallback.data != true) {
            final message = fallback.message.isEmpty
                ? 'Could not share your ride location'
                : fallback.message;
            _reportError(message);
            return message;
          }
        }
        _lastSentAt = DateTime.now();
        _lastError = null;
        return null;
      }

      final response = await _datasource.publishDriverLocation(
        driverId: driverId,
        rideId: null,
        latitude: position.latitude,
        longitude: position.longitude,
      );
      if (!response.success || response.data != true) {
        final message = response.message.isEmpty
            ? 'Could not share your location'
            : response.message;
        _reportError(message);
        return message;
      }
      _lastSentAt = DateTime.now();
      _lastError = null;
      return null;
    } catch (_) {
      const message = 'Could not share your location';
      _reportError(message);
      return message;
    }
  }

  void _reportError(String message) {
    if (_lastError == message || _errors.isClosed) return;
    _lastError = message;
    _errors.add(message);
  }
}
