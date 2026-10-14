import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/common/authentication/login/data/datasource/login_datasource.dart';
import 'package:swift_app/features/common/authentication/login/data/model/login_request_model.dart';
import 'package:swift_app/features/common/authentication/login/domain/entity/login_request.dart';
import 'package:swift_app/features/common/authentication/login/domain/repository/login_repository.dart';
import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_response.dart';

class LoginRepositoryImpl implements LoginRepository {
  final LoginDatasource loginDatasource;
  final AuthLocalDatasource authLocalDatasource;

  LoginRepositoryImpl({
    required this.loginDatasource,
    required this.authLocalDatasource,
  });

  @override
  Future<ApiResponseFormat<SignupResponse>> login(LoginRequest request) async {
    final response = await loginDatasource.login(
      LoginRequestModel.fromEntity(request),
    );

    if (!response.success) {
      return ApiResponseFormat<SignupResponse>.failure(
        statusCode: response.statusCode,
        message: response.message,
        endpoint: response.endpoint,
        errors: response.errors,
      );
    }

    final loginResponse = response.data!;
    await authLocalDatasource.saveAuthUser(loginResponse);

    return ApiResponseFormat<SignupResponse>.success(
      data: loginResponse,
      statusCode: response.statusCode,
      message: response.message,
      endpoint: response.endpoint,
    );
  }
}
