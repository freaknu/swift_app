import 'package:swift_app/features/rider/home/domain/entity/offer_response.dart';

class OfferResponseModel extends OfferResponse {
  OfferResponseModel({
    super.id,
    super.offerCode,
    super.title,
    super.description,
    super.rideTypeCode,
    super.discountType,
    super.discountValue,
    super.maximumDiscount,
    super.minimumFare,
    super.validFrom,
    super.validUntil,
    super.active,
    super.usageLimit,
    super.perUserLimit,
    super.usedCount,
    super.newUserOnly,
    super.stackable,
  });

  factory OfferResponseModel.fromJson(Map<String, dynamic> json) {
    return OfferResponseModel(
      id: json['id'] as int?,
      offerCode: json['offerCode'] as String?,
      title: json['title'] as String?,
      description: json['description'] as String?,
      rideTypeCode: json['rideTypeCode'] as String?,
      discountType: json['discountType'] as String?,
      discountValue: (json['discountValue'] as num?)?.toDouble(),
      maximumDiscount: (json['maximumDiscount'] as num?)?.toDouble(),
      minimumFare: (json['minimumFare'] as num?)?.toDouble(),
      validFrom: json['validFrom'] != null
          ? DateTime.tryParse(json['validFrom'] as String)
          : null,
      validUntil: json['validUntil'] != null
          ? DateTime.tryParse(json['validUntil'] as String)
          : null,
      active: json['active'] as bool?,
      usageLimit: json['usageLimit'] as int?,
      perUserLimit: json['perUserLimit'] as int?,
      usedCount: json['usedCount'] as int?,
      newUserOnly: json['newUserOnly'] as bool?,
      stackable: json['stackable'] as bool?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'offerCode': offerCode,
      'title': title,
      'description': description,
      'rideTypeCode': rideTypeCode,
      'discountType': discountType,
      'discountValue': discountValue,
      'maximumDiscount': maximumDiscount,
      'minimumFare': minimumFare,
      'validFrom': validFrom?.toUtc().toIso8601String(),
      'validUntil': validUntil?.toUtc().toIso8601String(),
      'active': active,
      'usageLimit': usageLimit,
      'perUserLimit': perUserLimit,
      'usedCount': usedCount,
      'newUserOnly': newUserOnly,
      'stackable': stackable,
    };
  }
}
