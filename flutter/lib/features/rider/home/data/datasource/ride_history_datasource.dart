import 'package:dio/dio.dart';
import 'package:swift_app/API/api_error_message.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/rider/home/data/model/ride_history_address_model.dart';

class RideHistoryDatasource {
  final DioClient _dioClient;
  final RiderApiEndpoints riderApiEndpoints;
  final AuthLocalDatasource authLocalDatasource;

  RideHistoryDatasource({
    required DioClient dioClient,
    required this.riderApiEndpoints,
    required this.authLocalDatasource,
  }) : _dioClient = dioClient;

  Future<ApiResponseFormat<List<RideHistoryAddressModel>>>
  fetchRideHistory() async {
    final String endpoint = riderApiEndpoints.getRideHistoryHomepage;

    final int authUserId = authLocalDatasource.userId ?? -1;

    try {
      final response = await _dioClient.dio.get(
        endpoint,
        options: Options(headers: {'x-user-id': authUserId}),
      );

      final responseData = response.data;
      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<List<RideHistoryAddressModel>>.fromJson(
          json: responseData,
          httpStatusCode: response.statusCode ?? 500,
          endpoint: endpoint,
          defaultSuccessMessage: 'Ride history fetched successfully',
          defaultFailureMessage: 'Failed to fetch ride history',
          fromData: (json) {
            if (json is! List) {
              return <RideHistoryAddressModel>[];
            }

            return json
                .whereType<Map<String, dynamic>>()
                .map((item) => RideHistoryAddressModel.fromJson(item))
                .toList();
          },
        );
      }
      return ApiResponseFormat<List<RideHistoryAddressModel>>.failure(
        statusCode: response.statusCode ?? 500,
        message: 'Invalid ride history response',
        endpoint: endpoint,
        errors: responseData,
      );
    } on DioException catch (e) {
      final responseData = e.response?.data;

      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<List<RideHistoryAddressModel>>.fromJson(
          json: responseData,
          httpStatusCode: e.response?.statusCode ?? 500,
          endpoint: endpoint,
          defaultSuccessMessage: 'Ride history fetched successfully',
          defaultFailureMessage: 'Failed to fetch ride history',
          fromData: (json) {
            if (json is! List) {
              return <RideHistoryAddressModel>[];
            }

            return json
                .whereType<Map<String, dynamic>>()
                .map((item) => RideHistoryAddressModel.fromJson(item))
                .toList();
          },
        );
      }

      return ApiResponseFormat<List<RideHistoryAddressModel>>.failure(
        statusCode: e.response?.statusCode ?? 500,
        message: userFacingDioMessage(e),
        endpoint: endpoint,
        errors: responseData,
      );
    } catch (e) {
      return ApiResponseFormat<List<RideHistoryAddressModel>>.failure(
        statusCode: 500,
        message: 'Unexpected error occurred',
        endpoint: endpoint,
        errors: e,
      );
    }
  }
}
