import 'dart:developer';

import 'package:dio/dio.dart';
import 'package:swift_app/API/api_error_message.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/rider/ride/data/model/ride_request_model.dart';
import 'package:swift_app/features/rider/ride/data/model/ride_response_model.dart';
import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';

/// Remote ride operations used by the rider journey.
///
/// Keeping ride lifecycle calls here means the UI can use the same response
/// parsing and error handling for request, refresh, resume, and cancellation.
class RideDatasource {
  final DioClient dioClient;
  final RiderApiEndpoints endpoints;

  RideDatasource({required this.dioClient, required this.endpoints});

  Future<ApiResponseFormat<RideResponse>> bookRide(RideRequestModel data) {
    return _sendRideRequest(
      endpoint: endpoints.requestRideEndpoint,
      request: () => dioClient.dio.post(
        endpoints.requestRideEndpoint,
        data: data.toJson(),
      ),
      failureMessage: 'Unable to request ride',
    );
  }

  Future<ApiResponseFormat<RideResponse>> getRide(int rideId) {
    final endpoint = endpoints.getRideDetail(rideId);
    return _sendRideRequest(
      endpoint: endpoint,
      request: () => dioClient.dio.get(endpoint),
      failureMessage: 'Unable to refresh ride status',
    );
  }

  Future<ApiResponseFormat<RideResponse>> getActiveRide(int riderId) {
    final endpoint = endpoints.getActiveRide(riderId);
    return _sendRideRequest(
      endpoint: endpoint,
      request: () => dioClient.dio.get(endpoint),
      failureMessage: 'Unable to find an active ride',
    );
  }

  Future<ApiResponseFormat<RideResponse>> cancelRide({
    required int rideId,
    required int riderId,
    required String reason,
  }) {
    final endpoint = endpoints.cancelRide(rideId);
    return _sendRideRequest(
      endpoint: endpoint,
      request: () => dioClient.dio.post(
        endpoint,
        data: {'actorId': riderId, 'actorType': 'RIDER', 'reason': reason},
      ),
      failureMessage: 'Unable to cancel ride',
    );
  }

  Future<ApiResponseFormat<RideResponse>> _sendRideRequest({
    required String endpoint,
    required Future<Response<dynamic>> Function() request,
    required String failureMessage,
  }) async {
    try {
      final response = await request();
      final json = _toJsonMap(response.data);
      log('Ride response [$endpoint] => $json', name: 'RideDatasource');
      return _parseResponse(
        json: json,
        statusCode: response.statusCode ?? 500,
        endpoint: endpoint,
        failureMessage: failureMessage,
      );
    } on DioException catch (error, stackTrace) {
      log(
        'Dio error [$endpoint]: ${error.message}',
        name: 'RideDatasource',
        error: error,
        stackTrace: stackTrace,
      );

      final response = error.response;
      if (response?.data is Map) {
        return _parseResponse(
          json: _toJsonMap(response!.data),
          statusCode: response.statusCode ?? 500,
          endpoint: endpoint,
          failureMessage: failureMessage,
        );
      }

      return ApiResponseFormat<RideResponse>.failure(
        statusCode: response?.statusCode ?? 500,
        message: userFacingDioMessage(error),
        endpoint: endpoint,
        errors: error.error,
      );
    } catch (error, stackTrace) {
      log(
        'Ride request failed [$endpoint]',
        name: 'RideDatasource',
        error: error,
        stackTrace: stackTrace,
      );
      return ApiResponseFormat<RideResponse>.failure(
        statusCode: 500,
        message: failureMessage,
        endpoint: endpoint,
        errors: error,
      );
    }
  }

  ApiResponseFormat<RideResponse> _parseResponse({
    required Map<String, dynamic> json,
    required int statusCode,
    required String endpoint,
    required String failureMessage,
  }) {
    return ApiResponseFormat<RideResponse>.fromJson(
      json: json,
      httpStatusCode: statusCode,
      endpoint: endpoint,
      defaultFailureMessage: failureMessage,
      fromData: (data) {
        if (data is! Map) {
          throw const FormatException('Ride response data is not an object');
        }
        return RideResponseModel.fromJson(Map<String, dynamic>.from(data));
      },
    );
  }

  Map<String, dynamic> _toJsonMap(dynamic data) {
    if (data is Map<String, dynamic>) return data;
    if (data is Map) return Map<String, dynamic>.from(data);
    throw FormatException(
      'Expected JSON object but received ${data.runtimeType}',
    );
  }
}
