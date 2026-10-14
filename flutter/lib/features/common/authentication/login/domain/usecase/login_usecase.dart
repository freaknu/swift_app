import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/constants/usecase/usecase.dart';
import 'package:swift_app/features/common/authentication/login/domain/entity/login_request.dart';
import 'package:swift_app/features/common/authentication/login/domain/repository/login_repository.dart';
import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_response.dart';

class LoginUsecase
    extends UseCase<LoginRequest, ApiResponseFormat<SignupResponse>> {
  final LoginRepository loginRepository;

  LoginUsecase({required this.loginRepository});

  @override
  Future<ApiResponseFormat<SignupResponse>> call(LoginRequest request) {
    return loginRepository.login(request);
  }
}
