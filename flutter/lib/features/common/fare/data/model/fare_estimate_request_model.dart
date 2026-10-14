import 'package:swift_app/features/common/fare/domain/entity/fare_estimate_request.dart';

class FareEstimateRequestModel extends FareEstimateRequest {
  const FareEstimateRequestModel({
    required super.offerId,
    required super.pickupLatitude,
    required super.pickupLongitude,
    required super.destinationLatitude,
    required super.destinationLongitude,
    super.rideType,
  });

  factory FareEstimateRequestModel.fromJson(Map<String, dynamic> json) {
    return FareEstimateRequestModel(
      offerId: (json['offerId'] as num?)?.toInt(),
      pickupLatitude: (json['pickupLatitude'] as num).toDouble(),
      pickupLongitude: (json['pickupLongitude'] as num).toDouble(),
      destinationLatitude: (json['destinationLatitude'] as num).toDouble(),
      destinationLongitude: (json['destinationLongitude'] as num).toDouble(),
      rideType: json['rideType']?.toString(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      if (offerId != null) 'offerId': offerId,
      'pickupLatitude': pickupLatitude,
      'pickupLongitude': pickupLongitude,
      'destinationLatitude': destinationLatitude,
      'destinationLongitude': destinationLongitude,
      if (rideType != null) 'rideType': rideType,
    };
  }
}
