import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';
import 'package:swift_app/features/rider/ride/domain/repository/ride_repository.dart';

class RideLifecycleUsecase {
  final RideRepository repository;

  const RideLifecycleUsecase(this.repository);

  Future<RideResponse> fetchRide(int rideId) {
    return repository.getRide(rideId);
  }

  Future<RideResponse> fetchActiveRide(int riderId) {
    return repository.getActiveRide(riderId);
  }

  Future<RideResponse> cancelRide({
    required int rideId,
    required int riderId,
    required String reason,
  }) {
    return repository.cancelRide(
      rideId: rideId,
      riderId: riderId,
      reason: reason,
    );
  }
}
