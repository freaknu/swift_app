import 'package:swift_app/features/common/fare/domain/entity/fare.dart';

class FareModel extends Fare {
  const FareModel({
    super.id,
    required super.baseFare,
    required super.distanceFare,
    required super.timeFare,
    required super.bookingFee,
    required super.tax,
    required super.surgeMultiplier,
    required super.discount,
    required super.totalFare,
    required super.currency,
    required super.paymentStatus,
    super.createdAt,
    super.updatedAt,
  });

  factory FareModel.fromJson(Map<String, dynamic> json) {
    return FareModel(
      id: json['id'] as int?,
      baseFare: (json['baseFare'] as num?)?.toDouble() ?? 0.0,
      distanceFare: (json['distanceFare'] as num?)?.toDouble() ?? 0.0,
      timeFare: (json['timeFare'] as num?)?.toDouble() ?? 0.0,
      bookingFee: (json['bookingFee'] as num?)?.toDouble() ?? 0.0,
      tax: (json['tax'] as num?)?.toDouble() ?? 0.0,
      surgeMultiplier: (json['surgeMultiplier'] as num?)?.toDouble() ?? 1.0,
      discount: (json['discount'] as num?)?.toDouble() ?? 0.0,
      totalFare: (json['totalFare'] as num?)?.toDouble() ?? 0.0,
      currency: json['currency'] as String? ?? 'INR',
      paymentStatus: json['paymentStatus'] as String? ?? 'PENDING',
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
