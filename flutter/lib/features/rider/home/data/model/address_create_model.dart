import 'package:swift_app/constants/enums/address_type_enum.dart';
import 'package:swift_app/features/rider/home/domain/entity/address_create.dart';

class AddressCreateModel extends AddressCreate {
  AddressCreateModel({
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

  factory AddressCreateModel.fromJson(Map<String, dynamic> json) {
    return AddressCreateModel(
      addressType: AddressTypeEnum.values.firstWhere(
        (type) => type.name == json['addressType'],
      ),

      label: json['label'] as String,

      fullAddress: json['fullAddress'] as String,

      landmark: json['landmark'] as String,

      city: json['city'] as String,

      state: json['state'] as String,

      country: json['country'] as String,

      pinCode: json['pinCode'] as String,

      latitude: (json['latitude'] as num).toDouble(),

      longitude: (json['longitude'] as num).toDouble(),

      defaultAddress: json['defaultAddress'] as bool,
    );
  }

  Map<String, dynamic> toJson() {
    return {
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
