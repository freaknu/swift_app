import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/constants/usecase/usecase.dart';
import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_request.dart';
import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_response.dart';
import 'package:swift_app/features/common/authentication/signup/domain/repository/signup_repository.dart';

class SignupUsecase
    extends UseCase<SignupRequest, ApiResponseFormat<SignupResponse>> {
  final SignupRepository signupRepository;
  SignupUsecase({required this.signupRepository});
  @override
  Future<ApiResponseFormat<SignupResponse>> call(SignupRequest request) {
    return signupRepository.signup(request);
  }
}
