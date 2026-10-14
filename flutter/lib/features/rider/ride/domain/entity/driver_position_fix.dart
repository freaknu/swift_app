import 'package:google_maps_flutter/google_maps_flutter.dart';

/// A server-timestamped position for the driver assigned to a ride.
/// Both the ride WebSocket event and the HTTP recovery snapshot use this shape.
class DriverPositionFix {
  final int rideId;
  final int driverId;
  final String eventId;
  final DateTime eventTime;
  final LatLng point;
  final double? distanceToPickupMeters;
  final int? etaMinutes;
  final double? distanceToDestinationMeters;
  final int? destinationEtaMinutes;
  final bool driverArrived;

  const DriverPositionFix({
    required this.rideId,
    required this.driverId,
    required this.eventId,
    required this.eventTime,
    required this.point,
    this.distanceToPickupMeters,
    this.etaMinutes,
    this.distanceToDestinationMeters,
    this.destinationEtaMinutes,
    this.driverArrived = false,
  });

  static DriverPositionFix? tryParse(Map<dynamic, dynamic> json) {
    final rideId = int.tryParse('${json['rideId'] ?? ''}');
    final driverId = int.tryParse('${json['driverId'] ?? ''}');
    final latitude = (json['latitude'] as num?)?.toDouble();
    final longitude = (json['longitude'] as num?)?.toDouble();
    final eventId = json['eventId']?.toString().trim() ?? '';
    final eventTime = DateTime.tryParse(json['eventTime']?.toString() ?? '');
    if (rideId == null ||
        driverId == null ||
        eventId.isEmpty ||
        eventTime == null ||
        latitude == null ||
        longitude == null ||
        !latitude.isFinite ||
        !longitude.isFinite ||
        latitude.abs() > 90 ||
        longitude.abs() > 180) {
      return null;
    }
    double? finiteDistance(Object? value) {
      final distance = (value as num?)?.toDouble();
      return distance != null && distance.isFinite && distance >= 0
          ? distance
          : null;
    }

    int? nonnegativeMinutes(Object? value) {
      final minutes = (value as num?)?.toInt();
      return minutes != null && minutes >= 0 ? minutes : null;
    }

    return DriverPositionFix(
      rideId: rideId,
      driverId: driverId,
      eventId: eventId,
      eventTime: eventTime.toUtc(),
      point: LatLng(latitude, longitude),
      distanceToPickupMeters: finiteDistance(json['distanceToPickupMeters']),
      etaMinutes: nonnegativeMinutes(json['etaMinutes']),
      distanceToDestinationMeters: finiteDistance(
        json['distanceToDestinationMeters'],
      ),
      destinationEtaMinutes: nonnegativeMinutes(json['destinationEtaMinutes']),
      driverArrived: json['driverArrived'] == true,
    );
  }

  bool belongsTo({required int rideId, int? driverId}) =>
      this.rideId == rideId && (driverId == null || this.driverId == driverId);

  bool isNewerThan(DriverPositionFix? previous) =>
      previous == null ||
      (eventId != previous.eventId && eventTime.isAfter(previous.eventTime));

  bool isFreshAt(
    DateTime now, {
    Duration maxAge = const Duration(seconds: 65),
  }) {
    final age = now.toUtc().difference(eventTime);
    // Android emulator and device clocks can differ from the server slightly.
    return age >= const Duration(seconds: -30) && age <= maxAge;
  }
}
