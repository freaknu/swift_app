import 'package:swift_app/features/rider/home/domain/entity/offer_request.dart';

class OfferRequestModel extends OfferRequest {
  OfferRequestModel({super.rideTypeCode, super.startFrom});

  factory OfferRequestModel.fromJson(Map<String, dynamic> json) {
    return OfferRequestModel(
      rideTypeCode: json['rideTypeCode'] as String?,
      startFrom: json['startFrom'] != null
          ? DateTime.tryParse(json['startFrom'] as String)
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'rideTypeCode': rideTypeCode,
      'startFrom': startFrom?.toUtc().toIso8601String(),
    };
  }
}
