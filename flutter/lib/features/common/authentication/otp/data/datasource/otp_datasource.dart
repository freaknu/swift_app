import 'package:dio/dio.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/otp/domain/entity/verify_otp_request.dart';

class OtpDatasource {
  final DioClient _dioClient;
  final RiderApiEndpoints _endpoints;

  OtpDatasource({
    required DioClient dioClient,
    required RiderApiEndpoints endpoints,
  }) : _dioClient = dioClient,
       _endpoints = endpoints;

  Future<ApiResponseFormat<String>> sendOtp(String email) async {
    final endpoint = _endpoints.sendOtp(email);

    try {
      final response = await _dioClient.dio.get(endpoint);
      final responseData = response.data;

      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<String>.fromJson(
          json: responseData,
          httpStatusCode: response.statusCode ?? 500,
          endpoint: endpoint,
          defaultSuccessMessage: 'OTP sent successfully',
          defaultFailureMessage: 'Failed to send OTP',
          fromData: (data) => data?.toString() ?? '',
        );
      }

      return ApiResponseFormat<String>.failure(
        statusCode: response.statusCode ?? 500,
        message: 'Invalid send OTP response',
        endpoint: endpoint,
        errors: responseData,
      );
    } on DioException catch (e) {
      return _handleDioException<String>(
        e,
        endpoint: endpoint,
        defaultFailureMessage: 'Failed to send OTP',
        fromData: (data) => data?.toString() ?? '',
      );
    } catch (e) {
      return ApiResponseFormat<String>.failure(
        statusCode: 500,
        message: 'Unexpected error occurred',
        endpoint: endpoint,
        errors: e,
      );
    }
  }

  Future<ApiResponseFormat<bool>> verifyOtp(VerifyOtpRequest request) async {
    final endpoint = _endpoints.verifyOtp(request.email, request.otp);

    try {
      final response = await _dioClient.dio.get(endpoint);
      final responseData = response.data;

      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<bool>.fromJson(
          json: responseData,
          httpStatusCode: response.statusCode ?? 500,
          endpoint: endpoint,
          defaultSuccessMessage: 'OTP verified successfully',
          defaultFailureMessage: 'OTP verification failed',
          fromData: _asBool,
        );
      }

      return ApiResponseFormat<bool>.failure(
        statusCode: response.statusCode ?? 500,
        message: 'Invalid verify OTP response',
        endpoint: endpoint,
        errors: responseData,
      );
    } on DioException catch (e) {
      return _handleDioException<bool>(
        e,
        endpoint: endpoint,
        defaultFailureMessage: 'OTP verification failed',
        fromData: _asBool,
      );
    } catch (e) {
      return ApiResponseFormat<bool>.failure(
        statusCode: 500,
        message: 'Unexpected error occurred',
        endpoint: endpoint,
        errors: e,
      );
    }
  }

  ApiResponseFormat<T> _handleDioException<T>(
    DioException exception, {
    required String endpoint,
    required String defaultFailureMessage,
    required T Function(dynamic data) fromData,
  }) {
    final responseData = exception.response?.data;

    if (responseData is Map<String, dynamic>) {
      return ApiResponseFormat<T>.fromJson(
        json: responseData,
        httpStatusCode: exception.response?.statusCode ?? 500,
        endpoint: endpoint,
        defaultFailureMessage: defaultFailureMessage,
        fromData: fromData,
      );
    }

    return ApiResponseFormat<T>.failure(
      statusCode: exception.response?.statusCode ?? 500,
      message: exception.message ?? defaultFailureMessage,
      endpoint: endpoint,
      errors: responseData,
    );
  }

  bool _asBool(dynamic value) {
    if (value is bool) return value;
    return value?.toString().toLowerCase() == 'true';
  }
}
