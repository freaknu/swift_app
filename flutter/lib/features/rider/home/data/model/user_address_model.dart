import 'package:swift_app/constants/enums/address_type_enum.dart';
import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';

class UserAddressModel extends UserAddress {
  UserAddressModel({
    required super.id,
    required super.addressType,
    required super.label,
    required super.fullAddress,
    required super.landmark,
    required super.city,
    required super.state,
    required super.country,
    required super.pinCode,
    required super.latitude,
    required super.longitude,
    required super.defaultAddress,
  });

  factory UserAddressModel.fromJson(Map<String, dynamic> json) {
    final String addressTypeValue = json['addressType']?.toString() ?? '';

    final AddressTypeEnum addressType = AddressTypeEnum.values.firstWhere(
      (type) => type.name == addressTypeValue,
      orElse: () => AddressTypeEnum.OTHER,
    );

    return UserAddressModel(
      id: json['id'] is int
          ? json['id'] as int
          : int.tryParse(json['id']?.toString() ?? '') ?? 0,

      addressType: addressType,

      label: json['label']?.toString() ?? '',
      fullAddress: json['fullAddress']?.toString() ?? '',
      landmark: json['landmark']?.toString() ?? '',

      // --------------------------------------------------------
      // CITY
      // --------------------------------------------------------
      city: json['city']?.toString() ?? '',

      // --------------------------------------------------------
      // STATE
      // --------------------------------------------------------
      state: json['state']?.toString() ?? '',

      // --------------------------------------------------------
      // COUNTRY
      // --------------------------------------------------------
      country: json['country']?.toString() ?? '',

      // --------------------------------------------------------
      // PIN CODE
      // --------------------------------------------------------
      pinCode: json['pinCode']?.toString() ?? '',

      // --------------------------------------------------------
      // LATITUDE
      // --------------------------------------------------------
      latitude: json['latitude'] is num
          ? (json['latitude'] as num).toDouble()
          : double.tryParse(json['latitude']?.toString() ?? '') ?? 0.0,

      // --------------------------------------------------------
      // LONGITUDE
      // --------------------------------------------------------
      longitude: json['longitude'] is num
          ? (json['longitude'] as num).toDouble()
          : double.tryParse(json['longitude']?.toString() ?? '') ?? 0.0,

      // --------------------------------------------------------
      // DEFAULT ADDRESS
      // --------------------------------------------------------
      defaultAddress: json['defaultAddress'] as bool? ?? false,
    );
  }

  // ============================================================
  // TO JSON
  // ============================================================

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'addressType': addressType.name,
      'label': label,
      'fullAddress': fullAddress,
      'landmark': landmark,
      'city': city,
      'state': state,
      'country': country,
      'pinCode': pinCode,
      'latitude': latitude,
      'longitude': longitude,
      'defaultAddress': defaultAddress,
    };
  }
}
