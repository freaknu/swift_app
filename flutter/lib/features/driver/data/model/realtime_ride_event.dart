import 'package:swift_app/features/rider/ride/data/model/ride_response_model.dart';

class RealtimeRideEvent {
  final int? rideId;
  final int? riderId;
  final int? driverId;
  final int? vehicleId;
  final String rideStatus;
  final DateTime? eventTime;
  final double? pickupLatitude;
  final double? pickupLongitude;
  final double? destinationLatitude;
  final double? destinationLongitude;
  final double? estimatedDistance;
  final int? estimatedDuration;
  final double? totalFare;
  final String? currency;
  final Map<String, dynamic> raw;

  const RealtimeRideEvent({
    this.rideId,
    this.riderId,
    this.driverId,
    this.vehicleId,
    this.rideStatus = '',
    this.eventTime,
    this.pickupLatitude,
    this.pickupLongitude,
    this.destinationLatitude,
    this.destinationLongitude,
    this.estimatedDistance,
    this.estimatedDuration,
    this.totalFare,
    this.currency,
    this.raw = const {},
  });

  bool get isRequest => rideStatus == 'REQUESTED' || rideStatus == 'MATCHING';
  bool get isFinished =>
      rideStatus == 'COMPLETED' ||
      rideStatus == 'CANCELLED' ||
      rideStatus == 'MATCH_FAILED';

  /// A targeted ride offer from the matching broadcast. Offer cards live in
  /// the offer inbox, not the lifecycle feed.
  bool get isOffer => rideStatus == 'OFFER_SENT';

  /// The offer stopped being actionable (taken by another driver / expired).
  bool get isOfferWithdrawn => rideStatus == 'OFFER_WITHDRAWN';

  bool get isDriverAssigned => rideStatus == 'DRIVER_ASSIGNED';

  /// 1-based matching attempt; the search radius grows with the attempt.
  int? get attempt => _asInt(raw['attempt']);

  /// Search radius (km) used for this broadcast.
  double? get radiusKm => _asDouble(raw['radiusKm']);

  factory RealtimeRideEvent.fromJson(Map<String, dynamic> json) {
    return RealtimeRideEvent(
      rideId: _asInt(json['rideId']),
      riderId: _asInt(json['riderId']),
      driverId: _asInt(json['driverId']),
      vehicleId: _asInt(json['vehicleId']),
      rideStatus: json['rideStatus']?.toString() ?? '',
      eventTime: DateTime.tryParse(json['eventTime']?.toString() ?? ''),
      pickupLatitude: _asDouble(json['pickupLatitude']),
      pickupLongitude: _asDouble(json['pickupLongitude']),
      destinationLatitude: _asDouble(json['destinationLatitude']),
      destinationLongitude: _asDouble(json['destinationLongitude']),
      estimatedDistance: _asDouble(json['estimatedDistance']),
      estimatedDuration: _asInt(json['estimatedDuration']),
      totalFare: _asDouble(json['totalFare']),
      currency: json['currency']?.toString(),
      raw: Map<String, dynamic>.from(json),
    );
  }

  factory RealtimeRideEvent.fromRideDetail(RideResponseModel detail) {
    return RealtimeRideEvent(
      rideId: detail.id,
      riderId: detail.riderId,
      driverId: detail.driverId,
      vehicleId: detail.vehicleId,
      rideStatus: detail.currentStatus ?? '',
      pickupLatitude: detail.pickupLatitude,
      pickupLongitude: detail.pickupLongitude,
      destinationLatitude: detail.destinationLatitude,
      destinationLongitude: detail.destinationLongitude,
      estimatedDistance: detail.estimatedDistance,
      estimatedDuration: detail.estimatedDuration,
      totalFare: detail.fareDetail?.totalFare,
      currency: detail.fareDetail?.currency,
    );
  }

  RealtimeRideEvent merge(RealtimeRideEvent newer) {
    return RealtimeRideEvent(
      rideId: newer.rideId ?? rideId,
      riderId: newer.riderId ?? riderId,
      driverId: newer.driverId ?? driverId,
      vehicleId: newer.vehicleId ?? vehicleId,
      rideStatus: newer.rideStatus.isNotEmpty ? newer.rideStatus : rideStatus,
      eventTime: newer.eventTime ?? eventTime,
      pickupLatitude: newer.pickupLatitude ?? pickupLatitude,
      pickupLongitude: newer.pickupLongitude ?? pickupLongitude,
      destinationLatitude: newer.destinationLatitude ?? destinationLatitude,
      destinationLongitude: newer.destinationLongitude ?? destinationLongitude,
      estimatedDistance: newer.estimatedDistance ?? estimatedDistance,
      estimatedDuration: newer.estimatedDuration ?? estimatedDuration,
      totalFare: newer.totalFare ?? totalFare,
      currency: newer.currency ?? currency,
      raw: {...raw, ...newer.raw},
    );
  }

  static int? _asInt(dynamic value) {
    if (value is int) return value;
    return int.tryParse(value?.toString() ?? '');
  }

  static double? _asDouble(dynamic value) {
    if (value is num) return value.toDouble();
    return double.tryParse(value?.toString() ?? '');
  }
}
