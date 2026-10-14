import 'package:swift_app/constants/enums/ride_type_enum.dart';
import 'package:swift_app/features/rider/home/data/model/ride_type_model.dart';
import 'package:swift_app/features/rider/ride/data/model/ride_request_model.dart';
import 'package:swift_app/features/rider/ride/domain/entity/route.dart'
    as ride_models;
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('ride type values from the backend are preserved', () {
    final ride = RideTypeModel.fromJson({
      'id': 7,
      'code': 'LUXURY',
      'name': 'Luxury',
      'rideType': 'luxury',
      'active': true,
      'sortOrder': 2,
      'maxPassengers': 4,
      'scheduledSupported': false,
      'cashSupported': true,
      'onlinePaymentSupported': true,
    });

    expect(ride.rideType, RideTypeEnum.LUXURY);
  });

  test('booking payload includes the route contract used by riderservice', () {
    final request = RideRequestModel(
      authUserId: 42,
      riderId: 9,
      rideType: 'SEDAN',
      route: const ride_models.Route(
        pickupLatitude: 12.9716,
        pickupLongitude: 77.5946,
        destinationLatitude: 12.9352,
        destinationLongitude: 77.6245,
      ),
    );

    expect(request.toJson(), {
      'authUserId': 42,
      'offerId': null,
      'riderId': 9,
      'rideType': 'SEDAN',
      'route': {
        'offerId': null,
        'pickupLatitude': 12.9716,
        'pickupLongitude': 77.5946,
        'destinationLatitude': 12.9352,
        'destinationLongitude': 77.6245,
        'rideType': 'SEDAN',
      },
    });
  });
}
