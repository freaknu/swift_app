import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/otp/data/datasource/otp_datasource.dart';
import 'package:swift_app/features/common/authentication/otp/domain/entity/verify_otp_request.dart';
import 'package:swift_app/features/common/authentication/otp/domain/repository/otp_repository.dart';

class OtpRepositoryImpl implements OtpRepository {
  final OtpDatasource otpDatasource;

  OtpRepositoryImpl({required this.otpDatasource});

  @override
  Future<ApiResponseFormat<String>> sendOtp(String email) {
    return otpDatasource.sendOtp(email);
  }

  @override
  Future<ApiResponseFormat<bool>> verifyOtp(VerifyOtpRequest request) {
    return otpDatasource.verifyOtp(request);
  }
}
