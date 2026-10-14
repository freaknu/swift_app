import 'package:swift_app/features/rider/ride/data/model/ride_response_model.dart';

/// The matching service offers one ride to this driver at a time. Both the
/// driver-service inbox and the targeted WebSocket event use this shape.
class PendingOfferModel {
  final int? rideId;
  final int? riderId;
  final int? driverId;
  final double? distanceKm;
  final DateTime? expiresAt;
  final DateTime? serverTime;

  /// Expiry translated to the device clock when the offer was received.
  final DateTime? localDeadline;
  final double? pickupLatitude;
  final double? pickupLongitude;
  final double? destinationLatitude;
  final double? destinationLongitude;
  final double? estimatedDistance;
  final int? estimatedDuration;
  final double? totalFare;
  final String? currency;
  final String? rideTypeCode;

  const PendingOfferModel({
    this.rideId,
    this.riderId,
    this.driverId,
    this.distanceKm,
    this.expiresAt,
    this.serverTime,
    this.localDeadline,
    this.pickupLatitude,
    this.pickupLongitude,
    this.destinationLatitude,
    this.destinationLongitude,
    this.estimatedDistance,
    this.estimatedDuration,
    this.totalFare,
    this.currency,
    this.rideTypeCode,
  });

  factory PendingOfferModel.fromJson(
    Map<String, dynamic> json, {
    DateTime? receivedAt,
  }) {
    final fare = json['fareDetail'];
    final fareMap = fare is Map<String, dynamic>
        ? fare
        : const <String, dynamic>{};
    final expiresAt = DateTime.tryParse(json['expiresAt']?.toString() ?? '');
    final serverTime = DateTime.tryParse(json['serverTime']?.toString() ?? '');
    return PendingOfferModel(
      rideId: _int(json['rideId'] ?? json['id']),
      riderId: _int(json['riderId']),
      driverId: _int(json['driverId']),
      distanceKm: _double(json['distanceKm']),
      expiresAt: expiresAt,
      serverTime: serverTime,
      localDeadline: expiresAt == null || serverTime == null
          ? null
          : (receivedAt ?? DateTime.now()).add(
              expiresAt.difference(serverTime),
            ),
      pickupLatitude: _double(json['pickupLatitude']),
      pickupLongitude: _double(json['pickupLongitude']),
      destinationLatitude: _double(json['destinationLatitude']),
      destinationLongitude: _double(json['destinationLongitude']),
      estimatedDistance: _double(json['estimatedDistance']),
      estimatedDuration: _int(json['estimatedDuration']),
      totalFare: _double(json['totalFare'] ?? fareMap['totalFare']),
      currency: (json['currency'] ?? fareMap['currency'])?.toString(),
      rideTypeCode: json['rideTypeCode']?.toString(),
    );
  }

  PendingOfferModel merge(PendingOfferModel newer) => PendingOfferModel(
    rideId: newer.rideId ?? rideId,
    riderId: newer.riderId ?? riderId,
    driverId: newer.driverId ?? driverId,
    distanceKm: newer.distanceKm ?? distanceKm,
    expiresAt: newer.expiresAt ?? expiresAt,
    serverTime: newer.serverTime ?? serverTime,
    localDeadline: newer.localDeadline ?? localDeadline,
    pickupLatitude: newer.pickupLatitude ?? pickupLatitude,
    pickupLongitude: newer.pickupLongitude ?? pickupLongitude,
    destinationLatitude: newer.destinationLatitude ?? destinationLatitude,
    destinationLongitude: newer.destinationLongitude ?? destinationLongitude,
    estimatedDistance: newer.estimatedDistance ?? estimatedDistance,
    estimatedDuration: newer.estimatedDuration ?? estimatedDuration,
    totalFare: newer.totalFare ?? totalFare,
    currency: newer.currency ?? currency,
    rideTypeCode: newer.rideTypeCode ?? rideTypeCode,
  );

  PendingOfferModel withRideDetail(RideResponseModel detail) => merge(
    PendingOfferModel(
      pickupLatitude: detail.pickupLatitude,
      pickupLongitude: detail.pickupLongitude,
      destinationLatitude: detail.destinationLatitude,
      destinationLongitude: detail.destinationLongitude,
      estimatedDistance: detail.estimatedDistance,
      estimatedDuration: detail.estimatedDuration,
      totalFare: detail.fareDetail?.totalFare,
      currency: detail.fareDetail?.currency,
      rideTypeCode: detail.rideTypeCode,
    ),
  );

  bool get hasTripDetails =>
      pickupLatitude != null &&
      pickupLongitude != null &&
      destinationLatitude != null &&
      destinationLongitude != null;

  bool get isExpired => isExpiredAt(DateTime.now());

  bool isExpiredAt(DateTime now) {
    final expiry = localDeadline ?? expiresAt;
    return expiry != null && !expiry.isAfter(now);
  }

  int? get secondsRemaining => secondsRemainingAt(DateTime.now());

  int? secondsRemainingAt(DateTime now) {
    final expiry = localDeadline ?? expiresAt;
    if (expiry == null) return null;
    final milliseconds = expiry.difference(now).inMilliseconds;
    return milliseconds <= 0 ? 0 : (milliseconds / 1000).ceil();
  }

  static int? _int(dynamic value) {
    if (value is num) return value.toInt();
    return int.tryParse(value?.toString() ?? '');
  }

  static double? _double(dynamic value) {
    if (value is num) return value.toDouble();
    return double.tryParse(value?.toString() ?? '');
  }
}
