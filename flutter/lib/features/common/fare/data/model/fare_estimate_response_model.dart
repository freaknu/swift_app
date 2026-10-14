import 'package:swift_app/features/common/fare/data/model/fare_model.dart';
import 'package:swift_app/features/common/fare/domain/entity/fare_estimate_response.dart';

class FareEstimateResponseModel extends FareEstimateResponse {
  const FareEstimateResponseModel({
    required super.estimatedDistanceKm,
    required super.estimatedDurationSeconds,
    required super.fare,
  });

  factory FareEstimateResponseModel.fromJson(Map<String, dynamic> json) {
    final fareJson = json['fare'] as Map<String, dynamic>? ?? {};

    return FareEstimateResponseModel(
      estimatedDistanceKm:
          (json['estimatedDistanceKm'] as num?)?.toDouble() ?? 0.0,
      estimatedDurationSeconds:
          (json['estimatedDurationSeconds'] as num?)?.toInt() ?? 0,
      fare: FareModel.fromJson(fareJson),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'estimatedDistanceKm': estimatedDistanceKm,
      'estimatedDurationSeconds': estimatedDurationSeconds,
      'fare': (fare as FareModel).toJson(),
    };
  }
}
