import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/constants/usecase/usecase.dart';
import 'package:swift_app/features/common/authentication/otp/domain/repository/otp_repository.dart';

class SendOtpUsecase extends UseCase<String, ApiResponseFormat<String>> {
  final OtpRepository otpRepository;

  SendOtpUsecase({required this.otpRepository});

  @override
  Future<ApiResponseFormat<String>> call(String request) {
    return otpRepository.sendOtp(request);
  }
}
