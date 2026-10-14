import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/constants/usecase/usecase.dart';
import 'package:swift_app/features/common/authentication/otp/domain/entity/verify_otp_request.dart';
import 'package:swift_app/features/common/authentication/otp/domain/repository/otp_repository.dart';

class VerifyOtpUsecase
    extends UseCase<VerifyOtpRequest, ApiResponseFormat<bool>> {
  final OtpRepository otpRepository;

  VerifyOtpUsecase({required this.otpRepository});

  @override
  Future<ApiResponseFormat<bool>> call(VerifyOtpRequest request) {
    return otpRepository.verifyOtp(request);
  }
}
