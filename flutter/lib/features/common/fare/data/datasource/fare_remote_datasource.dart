import 'package:dio/dio.dart';
import 'package:swift_app/API/api_error_message.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/common/fare/data/model/fare_estimate_request_model.dart';
import 'package:swift_app/features/common/fare/data/model/fare_estimate_response_model.dart';

class FareRemoteDatasource {
  final DioClient _dioClient;
  final RiderApiEndpoints _endpoints;
  final AuthLocalDatasource _authLocalDatasource;

  FareRemoteDatasource({
    required DioClient dioClient,
    required RiderApiEndpoints endpoints,
    required AuthLocalDatasource authLocalDatasource,
  }) : _dioClient = dioClient,
       _endpoints = endpoints,
       _authLocalDatasource = authLocalDatasource;

  Future<ApiResponseFormat<FareEstimateResponseModel>> estimateFare(
    FareEstimateRequestModel request,
  ) async {
    final String endpoint = _endpoints.calculateEstimate;

    final int? authUserId = _authLocalDatasource.userId;

    if (authUserId == null) {
      return ApiResponseFormat<FareEstimateResponseModel>.failure(
        statusCode: 401,
        message: 'Authenticated user not found',
        endpoint: endpoint,
        errors: 'User ID is missing from local authentication data',
      );
    }

    try {
      final response = await _dioClient.dio.post(
        endpoint,
        data: request.toJson(),
        options: Options(headers: {'x-user-id': authUserId.toString()}),
      );

      final responseData = response.data;

      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<FareEstimateResponseModel>.fromJson(
          json: responseData,
          httpStatusCode: response.statusCode ?? 500,
          endpoint: endpoint,
          defaultSuccessMessage: 'Fare estimated successfully',
          defaultFailureMessage: 'Failed to estimate fare',
          fromData: (json) {
            return FareEstimateResponseModel.fromJson(
              json as Map<String, dynamic>,
            );
          },
        );
      }

      return ApiResponseFormat<FareEstimateResponseModel>.failure(
        statusCode: response.statusCode ?? 500,
        message: 'Invalid fare response',
        endpoint: endpoint,
        errors: responseData,
      );
    } on DioException catch (e) {
      final responseData = e.response?.data;

      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<FareEstimateResponseModel>.fromJson(
          json: responseData,
          httpStatusCode: e.response?.statusCode ?? 500,
          endpoint: endpoint,
          defaultFailureMessage: 'Failed to estimate fare',
          fromData: (json) {
            return FareEstimateResponseModel.fromJson(
              json as Map<String, dynamic>,
            );
          },
        );
      }

      return ApiResponseFormat<FareEstimateResponseModel>.failure(
        statusCode: e.response?.statusCode ?? 500,
        message: userFacingDioMessage(e),
        endpoint: endpoint,
        errors: responseData,
      );
    } catch (e) {
      return ApiResponseFormat<FareEstimateResponseModel>.failure(
        statusCode: 500,
        message: 'Unexpected error occurred',
        endpoint: endpoint,
        errors: e,
      );
    }
  }
}
