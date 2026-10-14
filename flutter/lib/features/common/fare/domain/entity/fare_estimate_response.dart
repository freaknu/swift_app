import 'package:swift_app/features/common/fare/domain/entity/fare.dart';

class FareEstimateResponse {
  final double estimatedDistanceKm;
  final int estimatedDurationSeconds;
  final Fare fare;

  const FareEstimateResponse({
    required this.estimatedDistanceKm,
    required this.estimatedDurationSeconds,
    required this.fare,
  });
}
