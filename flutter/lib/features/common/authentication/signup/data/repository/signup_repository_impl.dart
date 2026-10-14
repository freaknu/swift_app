import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/common/authentication/signup/data/datasource/signup_datasource.dart';
import 'package:swift_app/features/common/authentication/signup/data/model/signup_request_model.dart';
import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_request.dart';
import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_response.dart';
import 'package:swift_app/features/common/authentication/signup/domain/repository/signup_repository.dart';

class SignupRepositoryImpl implements SignupRepository {
  final SignupDatasource signupDatasource;
  final AuthLocalDatasource authLocalDatasource;

  SignupRepositoryImpl({
    required this.signupDatasource,
    required this.authLocalDatasource,
  });

  @override
  Future<ApiResponseFormat<SignupResponse>> signup(SignupRequest data) async {
    final requestModel = SignupRequestModel(
      email: data.email,
      name: data.name,
      phoneNumber: data.phoneNumber,
      role: data.role,
    );

    final response = await signupDatasource.signUp(requestModel);

    if (!response.success) {
      return ApiResponseFormat.failure(
        statusCode: response.statusCode,
        message: response.message,
        endpoint: response.endpoint,
        errors: response.errors,
      );
    }

    final signupResponse = response.data!;
    await authLocalDatasource.saveAuthUser(signupResponse);

    return ApiResponseFormat.success(
      data: signupResponse,
      statusCode: response.statusCode,
      message: response.message,
      endpoint: response.endpoint,
    );
  }
}
