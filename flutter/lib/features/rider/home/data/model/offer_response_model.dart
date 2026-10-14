import 'package:swift_app/features/rider/home/domain/entity/offer_response.dart';

class OfferResponseModel extends OfferResponse {
  // Offer timestamps are emitted as UTC LocalDateTime by the backend. Dart
  // otherwise interprets a timestamp without an offset as device-local time.
  static DateTime? _utcDate(dynamic value) {
    if (value is! String) return null;
    final date = DateTime.tryParse(value);
    if (date == null) return null;
    if (date.isUtc) return date;
    return DateTime.utc(
      date.year,
      date.month,
      date.day,
      date.hour,
      date.minute,
      date.second,
      date.millisecond,
      date.microsecond,
    );
  }

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
      validFrom: _utcDate(json['validFrom']),
      validUntil: _utcDate(json['validUntil']),
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
