import 'package:swift_app/constants/enums/address_type_enum.dart';

class AddressCreate {
  final AddressTypeEnum addressType;
  final String label;
  final String fullAddress;
  final String landmark;
  final String city;
  final String state;
  final String country;
  final String pinCode;
  final double latitude;
  final double longitude;
  final bool defaultAddress;

  AddressCreate({
    required this.addressType,
    required this.label,
    required this.fullAddress,
    required this.landmark,
    required this.city,
    required this.state,
    required this.country,
    required this.pinCode,
    required this.latitude,
    required this.longitude,
    required this.defaultAddress,
  });
}
