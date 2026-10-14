import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/otp/domain/entity/verify_otp_request.dart';

abstract class OtpRepository {
  Future<ApiResponseFormat<String>> sendOtp(String email);

  Future<ApiResponseFormat<bool>> verifyOtp(VerifyOtpRequest request);
}
