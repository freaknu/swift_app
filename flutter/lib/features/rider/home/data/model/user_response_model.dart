import 'package:swift_app/features/rider/home/data/model/user_address_model.dart';
import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';

class UserResponseModel extends UserReponse {
  UserResponseModel({
    required super.id,
    required super.authUserId,
    required super.fullName,
    required super.email,
    required super.phoneNumber,
    required super.profileImageUrl,
    required super.active,
    required super.deleted,
    required super.allAddresses,
  });

  // ============================================================
  // FROM JSON
  // ============================================================

  factory UserResponseModel.fromJson(Map<String, dynamic> json) {
    return UserResponseModel(
      // --------------------------------------------------------
      // ID
      // --------------------------------------------------------
      id: json['id'] is int
          ? json['id'] as int
          : int.tryParse(json['id']?.toString() ?? '') ?? 0,

      // --------------------------------------------------------
      // AUTH USER ID
      // --------------------------------------------------------
      authUserId: json['authUserId'] is int
          ? json['authUserId'] as int
          : int.tryParse(json['authUserId']?.toString() ?? '') ?? 0,

      // --------------------------------------------------------
      // FULL NAME
      // --------------------------------------------------------
      fullName: json['fullName']?.toString() ?? '',

      // --------------------------------------------------------
      // EMAIL
      //
      // Backend can return:
      // "email": null
      // --------------------------------------------------------
      email: json['email']?.toString(),

      // --------------------------------------------------------
      // PHONE NUMBER
      // --------------------------------------------------------
      phoneNumber: json['phoneNumber']?.toString() ?? '',

      // --------------------------------------------------------
      // PROFILE IMAGE
      //
      // Backend can return:
      // "profileImageUrl": null
      // --------------------------------------------------------
      profileImageUrl: json['profileImageUrl']?.toString(),

      // --------------------------------------------------------
      // ACTIVE
      // --------------------------------------------------------
      active: json['active'] as bool? ?? false,

      // --------------------------------------------------------
      // DELETED
      // --------------------------------------------------------
      deleted: json['deleted'] as bool? ?? false,

      // --------------------------------------------------------
      // ADDRESSES
      //
      // Backend can return:
      //
      // "allAddresses": []
      //
      // OR:
      //
      // "allAddresses": null
      // --------------------------------------------------------
      allAddresses: json['allAddresses'] is List
          ? (json['allAddresses'] as List)
                .whereType<Map<String, dynamic>>()
                .map((address) => UserAddressModel.fromJson(address))
                .toList()
          : [],
    );
  }

  // ============================================================
  // TO JSON
  // ============================================================

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'authUserId': authUserId,
      'fullName': fullName,
      'email': email,
      'phoneNumber': phoneNumber,
      'profileImageUrl': profileImageUrl,
      'active': active,
      'deleted': deleted,

      'allAddresses': allAddresses.map((address) {
        if (address is UserAddressModel) {
          return address.toJson();
        }

        return address;
      }).toList(),
    };
  }
}
