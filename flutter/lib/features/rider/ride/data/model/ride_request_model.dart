import 'package:swift_app/features/rider/ride/domain/entity/ride_request.dart';

class RideRequestModel extends RideRequest {
  const RideRequestModel({
    super.authUserId,
    super.offerId,
    super.riderId,
    super.rideType,
    super.route,
  });

  factory RideRequestModel.fromEntity(RideRequest entity) {
    return RideRequestModel(
      authUserId: entity.authUserId,
      offerId: entity.offerId,
      riderId: entity.riderId,
      rideType: entity.rideType,
      route: entity.route,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'authUserId': authUserId,
      'offerId': offerId,
      'riderId': riderId,
      'rideType': rideType,
      'route': route == null
          ? null
          : {
              'offerId': route!.offerId,
              'pickupLatitude': route!.pickupLatitude,
              'pickupLongitude': route!.pickupLongitude,
              'destinationLatitude': route!.destinationLatitude,
              'destinationLongitude': route!.destinationLongitude,
              'rideType': route!.rideType ?? rideType,
            },
    };
  }
}
