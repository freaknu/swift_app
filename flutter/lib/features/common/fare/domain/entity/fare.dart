class Fare {
  final int? id;
  final double baseFare;
  final double distanceFare;
  final double timeFare;
  final double bookingFee;
  final double tax;
  final double surgeMultiplier;
  final double discount;
  final double totalFare;
  final String currency;
  final String paymentStatus;
  final String? createdAt;
  final String? updatedAt;

  const Fare({
    this.id,
    required this.baseFare,
    required this.distanceFare,
    required this.timeFare,
    required this.bookingFee,
    required this.tax,
    required this.surgeMultiplier,
    required this.discount,
    required this.totalFare,
    required this.currency,
    required this.paymentStatus,
    this.createdAt,
    this.updatedAt,
  });
}
