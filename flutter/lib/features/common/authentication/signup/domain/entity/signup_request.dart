import 'package:swift_app/constants/enums/role_enum.dart';

class SignupRequest {
  final String email;
  final String name;
  final String phoneNumber;
  RoleEnum role;

  SignupRequest({
    required this.email,
    required this.name,
    required this.phoneNumber,
    required this.role,
  });
}
