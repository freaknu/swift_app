import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_request.dart';
import 'package:swift_app/constants/enums/role_enum.dart';

class SignupRequestModel extends SignupRequest {
  SignupRequestModel({
    required super.email,
    required super.name,
    required super.phoneNumber,
    required super.role,
  });

  factory SignupRequestModel.fromJson(Map<String, dynamic> json) {
    return SignupRequestModel(
      email: json['email']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      phoneNumber: json['phoneNumber']?.toString() ?? '',
      role: RoleEnum.values.firstWhere(
        (role) =>
            role.name ==
            (json['role'] ?? json['roleEnum'])?.toString().toUpperCase(),
        orElse: () => RoleEnum.RIDER,
      ),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'email': email,
      'name': name,
      'phoneNumber': phoneNumber,
      'role': role.name,
    };
  }
}
