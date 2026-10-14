import 'package:dio/dio.dart';
import 'package:swift_app/API/api_error_message.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/login/data/model/login_request_model.dart';
import 'package:swift_app/features/common/authentication/signup/data/model/signup_response_model.dart';

class LoginDatasource {
  final DioClient _dioClient;
  final RiderApiEndpoints _endpoints;

  LoginDatasource({
    required DioClient dioClient,
    required RiderApiEndpoints endpoints,
  }) : _dioClient = dioClient,
       _endpoints = endpoints;

  Future<ApiResponseFormat<SignupResponseModel>> login(
    LoginRequestModel request,
  ) async {
    final endpoint = _endpoints.loginEndpoint;

    try {
      final response = await _dioClient.dio.post(
        endpoint,
        data: request.toJson(),
      );
      final responseData = response.data;

      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<SignupResponseModel>.fromJson(
          json: responseData,
          httpStatusCode: response.statusCode ?? 500,
          endpoint: endpoint,
          defaultSuccessMessage: 'Login successful',
          defaultFailureMessage: 'Login failed',
          fromData: (json) =>
              SignupResponseModel.fromJson(json as Map<String, dynamic>),
        );
      }

      return ApiResponseFormat<SignupResponseModel>.failure(
        statusCode: response.statusCode ?? 500,
        message: 'Invalid login response',
        endpoint: endpoint,
        errors: responseData,
      );
    } on DioException catch (e) {
      final responseData = e.response?.data;
      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<SignupResponseModel>.fromJson(
          json: responseData,
          httpStatusCode: e.response?.statusCode ?? 500,
          endpoint: endpoint,
          defaultFailureMessage: 'Login failed',
          fromData: (json) =>
              SignupResponseModel.fromJson(json as Map<String, dynamic>),
        );
      }

      return ApiResponseFormat<SignupResponseModel>.failure(
        statusCode: e.response?.statusCode ?? 500,
        message: userFacingDioMessage(e),
        endpoint: endpoint,
        errors: responseData,
      );
    } catch (e) {
      return ApiResponseFormat<SignupResponseModel>.failure(
        statusCode: 500,
        message: 'Unexpected error occurred',
        endpoint: endpoint,
        errors: e,
      );
    }
  }
}
