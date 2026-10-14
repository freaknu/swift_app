class OfferResponse {
  final int? id;
  final String? offerCode;
  final String? title;
  final String? description;
  final String? rideTypeCode;
  final String? discountType;
  final double? discountValue;
  final double? maximumDiscount;
  final double? minimumFare;
  final DateTime? validFrom;
  final DateTime? validUntil;
  final bool? active;
  final int? usageLimit;
  final int? perUserLimit;
  final int? usedCount;
  final bool? newUserOnly;
  final bool? stackable;

  OfferResponse({
    this.id,
    this.offerCode,
    this.title,
    this.description,
    this.rideTypeCode,
    this.discountType,
    this.discountValue,
    this.maximumDiscount,
    this.minimumFare,
    this.validFrom,
    this.validUntil,
    this.active,
    this.usageLimit,
    this.perUserLimit,
    this.usedCount,
    this.newUserOnly,
    this.stackable,
  });
}
