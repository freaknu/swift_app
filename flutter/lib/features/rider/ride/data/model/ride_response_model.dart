import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';

class RideResponseModel extends RideResponse {
  const RideResponseModel({
    super.id,
    super.riderId,
    super.driverId,
    super.vehicleId,
    super.rideTypeCode,
    super.currentStatus,
    super.pickupLatitude,
    super.pickupLongitude,
    super.destinationLatitude,
    super.destinationLongitude,
    super.estimatedDistance,
    super.actualDistance,
    super.isCompleted,
    super.estimatedDuration,
    super.actualDuration,
    super.requestedAt,
    super.acceptedAt,
    super.driverArrivedAt,
    super.startedAt,
    super.completedAt,
    super.isRideCancelled,
    super.cancelledAt,
    super.cancelledBy,
    super.cancellationReason,
    super.fareDetail,
    super.rideHistory,
    super.createdAt,
    super.updatedAt,
  });

  factory RideResponseModel.fromJson(Map<String, dynamic> json) {
    final fareJson = json['fareDetail'];
    final historyJson = json['rideHistory'];

    return RideResponseModel(
      id: (json['id'] as num?)?.toInt(),
      riderId: (json['riderId'] as num?)?.toInt(),
      driverId: (json['driverId'] as num?)?.toInt(),
      vehicleId: (json['vehicleId'] as num?)?.toInt(),
      rideTypeCode: json['rideTypeCode']?.toString(),

      currentStatus: json['currentStatus'] as String?,

      pickupLatitude: (json['pickupLatitude'] as num?)?.toDouble(),

      pickupLongitude: (json['pickupLongitude'] as num?)?.toDouble(),

      destinationLatitude: (json['destinationLatitude'] as num?)?.toDouble(),

      destinationLongitude: (json['destinationLongitude'] as num?)?.toDouble(),

      estimatedDistance: (json['estimatedDistance'] as num?)?.toDouble(),

      actualDistance: (json['actualDistance'] as num?)?.toDouble(),

      isCompleted: json['isCompleted'] as bool?,

      estimatedDuration: (json['estimatedDuration'] as num?)?.toInt(),

      actualDuration: (json['actualDuration'] as num?)?.toInt(),

      requestedAt: json['requestedAt'] as String?,
      acceptedAt: json['acceptedAt'] as String?,
      driverArrivedAt: json['driverArrivedAt'] as String?,
      startedAt: json['startedAt'] as String?,
      completedAt: json['completedAt'] as String?,

      isRideCancelled: json['isRideCancelled'] as bool?,

      cancelledAt: json['cancelledAt'] as String?,

      cancelledBy: (json['cancelledBy'] as num?)?.toInt(),

      cancellationReason: json['cancellationReason'] as String?,

      fareDetail: fareJson is Map<String, dynamic>
          ? FareDetailModel.fromJson(fareJson)
          : null,

      rideHistory: historyJson is List
          ? historyJson
                .whereType<Map<String, dynamic>>()
                .map(RideHistoryModel.fromJson)
                .toList()
          : const [],

      createdAt: json['createdAt'] as String?,
      updatedAt: json['updatedAt'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'riderId': riderId,
      'driverId': driverId,
      'vehicleId': vehicleId,
      'rideTypeCode': rideTypeCode,
      'currentStatus': currentStatus,
      'pickupLatitude': pickupLatitude,
      'pickupLongitude': pickupLongitude,
      'destinationLatitude': destinationLatitude,
      'destinationLongitude': destinationLongitude,
      'estimatedDistance': estimatedDistance,
      'actualDistance': actualDistance,
      'isCompleted': isCompleted,
      'estimatedDuration': estimatedDuration,
      'actualDuration': actualDuration,
      'requestedAt': requestedAt,
      'acceptedAt': acceptedAt,
      'driverArrivedAt': driverArrivedAt,
      'startedAt': startedAt,
      'completedAt': completedAt,
      'isRideCancelled': isRideCancelled,
      'cancelledAt': cancelledAt,
      'cancelledBy': cancelledBy,
      'cancellationReason': cancellationReason,

      'fareDetail': fareDetail == null
          ? null
          : {
              'id': fareDetail!.id,
              'baseFare': fareDetail!.baseFare,
              'distanceFare': fareDetail!.distanceFare,
              'timeFare': fareDetail!.timeFare,
              'bookingFee': fareDetail!.bookingFee,
              'tax': fareDetail!.tax,
              'surgeMultiplier': fareDetail!.surgeMultiplier,
              'discount': fareDetail!.discount,
              'totalFare': fareDetail!.totalFare,
              'currency': fareDetail!.currency,
              'paymentStatus': fareDetail!.paymentStatus,
              'createdAt': fareDetail!.createdAt,
              'updatedAt': fareDetail!.updatedAt,
            },

      'rideHistory': rideHistory
          .map(
            (history) => {
              'id': history.id,
              'rideStatus': history.rideStatus,
              'changedById': history.changedById,
              'changedByType': history.changedByType,
              'remarks': history.remarks,
              'createdAt': history.createdAt,
            },
          )
          .toList(),

      'createdAt': createdAt,
      'updatedAt': updatedAt,
    };
  }
}

class FareDetailModel extends FareDetail {
  const FareDetailModel({
    super.id,
    super.baseFare,
    super.distanceFare,
    super.timeFare,
    super.bookingFee,
    super.tax,
    super.surgeMultiplier,
    super.discount,
    super.totalFare,
    super.currency,
    super.paymentStatus,
    super.createdAt,
    super.updatedAt,
  });

  factory FareDetailModel.fromJson(Map<String, dynamic> json) {
    return FareDetailModel(
      id: (json['id'] as num?)?.toInt(),

      baseFare: (json['baseFare'] as num?)?.toDouble(),

      distanceFare: (json['distanceFare'] as num?)?.toDouble(),

      timeFare: (json['timeFare'] as num?)?.toDouble(),

      bookingFee: (json['bookingFee'] as num?)?.toDouble(),

      tax: (json['tax'] as num?)?.toDouble(),

      surgeMultiplier: (json['surgeMultiplier'] as num?)?.toDouble(),

      discount: (json['discount'] as num?)?.toDouble(),

      totalFare: (json['totalFare'] as num?)?.toDouble(),

      currency: json['currency'] as String?,

      paymentStatus: json['paymentStatus'] as String?,

      createdAt: json['createdAt'] as String?,

      updatedAt: json['updatedAt'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'baseFare': baseFare,
      'distanceFare': distanceFare,
      'timeFare': timeFare,
      'bookingFee': bookingFee,
      'tax': tax,
      'surgeMultiplier': surgeMultiplier,
      'discount': discount,
      'totalFare': totalFare,
      'currency': currency,
      'paymentStatus': paymentStatus,
      'createdAt': createdAt,
      'updatedAt': updatedAt,
    };
  }
}

class RideHistoryModel extends RideHistory {
  const RideHistoryModel({
    super.id,
    super.rideStatus,
    super.changedById,
    super.changedByType,
    super.remarks,
    super.createdAt,
  });

  factory RideHistoryModel.fromJson(Map<String, dynamic> json) {
    return RideHistoryModel(
      id: (json['id'] as num?)?.toInt(),

      rideStatus: json['rideStatus'] as String?,

      changedById: (json['changedById'] as num?)?.toInt(),

      changedByType: json['changedByType'] as String?,

      remarks: json['remarks'] as String?,

      createdAt: json['createdAt'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'rideStatus': rideStatus,
      'changedById': changedById,
      'changedByType': changedByType,
      'remarks': remarks,
      'createdAt': createdAt,
    };
  }
}
