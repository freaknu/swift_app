class RideResponse {
  final int? id;
  final int? riderId;
  final int? driverId;
  final int? vehicleId;
  final String? rideTypeCode;
  final String? currentStatus;

  final double? pickupLatitude;
  final double? pickupLongitude;
  final double? destinationLatitude;
  final double? destinationLongitude;

  final double? estimatedDistance;
  final double? actualDistance;

  final bool? isCompleted;

  final int? estimatedDuration;
  final int? actualDuration;

  final String? requestedAt;
  final String? acceptedAt;
  final String? driverArrivedAt;
  final String? startedAt;
  final String? completedAt;

  final bool? isRideCancelled;
  final String? cancelledAt;
  final int? cancelledBy;
  final String? cancellationReason;

  final FareDetail? fareDetail;
  final List<RideHistory> rideHistory;

  final String? createdAt;
  final String? updatedAt;

  const RideResponse({
    this.id,
    this.riderId,
    this.driverId,
    this.vehicleId,
    this.rideTypeCode,
    this.currentStatus,
    this.pickupLatitude,
    this.pickupLongitude,
    this.destinationLatitude,
    this.destinationLongitude,
    this.estimatedDistance,
    this.actualDistance,
    this.isCompleted,
    this.estimatedDuration,
    this.actualDuration,
    this.requestedAt,
    this.acceptedAt,
    this.driverArrivedAt,
    this.startedAt,
    this.completedAt,
    this.isRideCancelled,
    this.cancelledAt,
    this.cancelledBy,
    this.cancellationReason,
    this.fareDetail,
    this.rideHistory = const [],
    this.createdAt,
    this.updatedAt,
  });
}

class FareDetail {
  final int? id;
  final double? baseFare;
  final double? distanceFare;
  final double? timeFare;
  final double? bookingFee;
  final double? tax;
  final double? surgeMultiplier;
  final double? discount;
  final double? totalFare;
  final String? currency;
  final String? paymentStatus;
  final String? createdAt;
  final String? updatedAt;

  const FareDetail({
    this.id,
    this.baseFare,
    this.distanceFare,
    this.timeFare,
    this.bookingFee,
    this.tax,
    this.surgeMultiplier,
    this.discount,
    this.totalFare,
    this.currency,
    this.paymentStatus,
    this.createdAt,
    this.updatedAt,
  });
}

class RideHistory {
  final int? id;
  final String? rideStatus;
  final int? changedById;
  final String? changedByType;
  final String? remarks;
  final String? createdAt;

  const RideHistory({
    this.id,
    this.rideStatus,
    this.changedById,
    this.changedByType,
    this.remarks,
    this.createdAt,
  });
}
