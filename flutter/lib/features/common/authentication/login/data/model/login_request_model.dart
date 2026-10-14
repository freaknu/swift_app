import 'package:swift_app/features/common/authentication/login/domain/entity/login_request.dart';

class LoginRequestModel extends LoginRequest {
  const LoginRequestModel({required super.email, required super.otp});

  factory LoginRequestModel.fromEntity(LoginRequest request) {
    return LoginRequestModel(email: request.email, otp: request.otp);
  }

  Map<String, dynamic> toJson() {
    return {'email': email, 'otp': otp};
  }
}
