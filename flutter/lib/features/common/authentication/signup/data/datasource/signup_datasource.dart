import 'package:dio/dio.dart';
import 'package:swift_app/API/api_error_message.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/signup/data/model/signup_request_model.dart';
import 'package:swift_app/features/common/authentication/signup/data/model/signup_response_model.dart';

class SignupDatasource {
  final DioClient _dioClient;

  SignupDatasource({required DioClient dioClient}) : _dioClient = dioClient;

  Future<ApiResponseFormat<SignupResponseModel>> signUp(
    SignupRequestModel data,
  ) async {
    final String endpoint = RiderApiEndpoints().signupEndpoint;
    try {
      final response = await _dioClient.dio.post(endpoint, data: data.toJson());
      final responseData = response.data;

      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<SignupResponseModel>.fromJson(
          json: responseData,
          httpStatusCode: response.statusCode ?? 500,
          endpoint: endpoint,
          defaultSuccessMessage: 'Signup successful',
          defaultFailureMessage: 'Signup failed',
          fromData: (json) =>
              SignupResponseModel.fromJson(json as Map<String, dynamic>),
        );
      }

      return ApiResponseFormat<SignupResponseModel>.failure(
        statusCode: response.statusCode ?? 500,
        message: 'Invalid signup response',
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
          defaultFailureMessage: 'Signup failed',
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
