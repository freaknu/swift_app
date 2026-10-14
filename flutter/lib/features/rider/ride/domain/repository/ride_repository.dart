import 'package:swift_app/features/rider/ride/domain/entity/ride_request.dart';
import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';

abstract class RideRepository {
  Future<RideResponse> bookRide(RideRequest data);

  Future<RideResponse> getRide(int rideId);

  Future<RideResponse> getActiveRide(int riderId);

  Future<RideResponse> cancelRide({
    required int rideId,
    required int riderId,
    required String reason,
  });
}
