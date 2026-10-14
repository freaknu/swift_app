import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/login/domain/entity/login_request.dart';
import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_response.dart';

abstract class LoginRepository {
  Future<ApiResponseFormat<SignupResponse>> login(LoginRequest request);
}
