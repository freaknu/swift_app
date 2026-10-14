import 'package:swift_app/constants/enums/address_type_enum.dart';

class UserReponse {
  final int id;
  final int authUserId;
  final String fullName;

  // Backend can return null
  final String? email;

  final String phoneNumber;

  // Backend can return null
  final String? profileImageUrl;

  final bool active;
  final bool deleted;
  final List<UserAddress> allAddresses;

  UserReponse({
    required this.id,
    required this.authUserId,
    required this.fullName,
    required this.email,
    required this.phoneNumber,
    required this.profileImageUrl,
    required this.active,
    required this.deleted,
    required this.allAddresses,
  });
}

class UserAddress {
  final int id;
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

  UserAddress({
    required this.id,
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
