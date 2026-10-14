import 'package:swift_app/features/rider/ride/domain/entity/route.dart';

class RideRequest {
  final int? authUserId;
  final int? offerId;
  final int? riderId;
  final String? rideType;
  final Route? route;

  const RideRequest({
    this.authUserId,
    this.offerId,
    this.riderId,
    this.rideType,
    this.route,
  });
}
