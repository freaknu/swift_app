import 'package:swift_app/constants/enums/ride_type_enum.dart';

class RideType {
  final int id;
  final String code;
  final String name;
  final String description;
  final RideTypeEnum rideType;
  final bool active;
  final int sortOrder;
  final int maxPassengers;
  final bool scheduledSupported;
  final bool cashSupported;
  final bool onlinePaymentSupported;

  RideType({
    required this.id,
    required this.code,
    required this.name,
    required this.description,
    required this.rideType,
    required this.active,
    required this.sortOrder,
    required this.maxPassengers,
    required this.scheduledSupported,
    required this.cashSupported,
    required this.onlinePaymentSupported,
  });
}
