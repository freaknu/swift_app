import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_request.dart';
import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_response.dart';

abstract class SignupRepository {
  Future<ApiResponseFormat<SignupResponse>> signup(SignupRequest data);
}
