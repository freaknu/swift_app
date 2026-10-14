import 'package:dio/dio.dart';
import 'package:swift_app/API/api_error_message.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/rider/home/data/model/ride_type_model.dart';

class RideTypeDatasource {
  final DioClient _dioClient;
  final RiderApiEndpoints riderApiEndpoints;
  final AuthLocalDatasource authLocalDatasource;

  RideTypeDatasource({
    required DioClient dioClient,
    required this.riderApiEndpoints,
    required this.authLocalDatasource,
  }) : _dioClient = dioClient;

  // ============================================================
  // GET RIDE TYPES
  // ============================================================

  Future<ApiResponseFormat<List<RideTypeModel>>> fetchRideTypes() async {
    final String endpoint = riderApiEndpoints.vehicleTypesEndpoint;

    // ==========================================================
    // GET AUTH USER ID
    // ==========================================================

    final int authUserId = authLocalDatasource.userId ?? -1;

    try {
      // ========================================================
      // API REQUEST
      // ========================================================

      final response = await _dioClient.dio.get(
        endpoint,
        options: Options(headers: {'x-user-id': authUserId}),
      );

      final responseData = response.data;

      // ========================================================
      // SUCCESS / API RESPONSE
      // ========================================================

      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<List<RideTypeModel>>.fromJson(
          json: responseData,
          httpStatusCode: response.statusCode ?? 500,
          endpoint: endpoint,
          defaultSuccessMessage: 'Ride types fetched successfully',
          defaultFailureMessage: 'Failed to fetch ride types',
          fromData: (json) {
            // --------------------------------------------------
            // DATA SHOULD BE A LIST
            // --------------------------------------------------

            if (json is! List) {
              return <RideTypeModel>[];
            }

            return json
                .whereType<Map<String, dynamic>>()
                .toList()
                .asMap()
                .entries
                .map(
                  (entry) =>
                      RideTypeModel.fromVehicleTypeJson(entry.value, entry.key),
                )
                .where((type) => type.code.isNotEmpty)
                .toList();
          },
        );
      }

      // ========================================================
      // INVALID RESPONSE
      // ========================================================

      return ApiResponseFormat<List<RideTypeModel>>.failure(
        statusCode: response.statusCode ?? 500,
        message: 'Invalid ride types response',
        endpoint: endpoint,
        errors: responseData,
      );
    }
    // ==========================================================
    // DIO ERROR
    // ==========================================================
    on DioException catch (e) {
      final responseData = e.response?.data;

      // --------------------------------------------------------
      // SERVER RETURNED API RESPONSE
      // --------------------------------------------------------

      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<List<RideTypeModel>>.fromJson(
          json: responseData,
          httpStatusCode: e.response?.statusCode ?? 500,
          endpoint: endpoint,
          defaultSuccessMessage: 'Ride types fetched successfully',
          defaultFailureMessage: 'Failed to fetch ride types',
          fromData: (json) {
            if (json is! List) {
              return <RideTypeModel>[];
            }

            return json
                .whereType<Map<String, dynamic>>()
                .toList()
                .asMap()
                .entries
                .map(
                  (entry) =>
                      RideTypeModel.fromVehicleTypeJson(entry.value, entry.key),
                )
                .where((type) => type.code.isNotEmpty)
                .toList();
          },
        );
      }

      // --------------------------------------------------------
      // NO SERVER RESPONSE
      // --------------------------------------------------------

      return ApiResponseFormat<List<RideTypeModel>>.failure(
        statusCode: e.response?.statusCode ?? 500,
        message: userFacingDioMessage(e),
        endpoint: endpoint,
        errors: responseData,
      );
    }
    // ==========================================================
    // UNEXPECTED ERROR
    // ==========================================================
    catch (e) {
      return ApiResponseFormat<List<RideTypeModel>>.failure(
        statusCode: 500,
        message: 'Unexpected error occurred',
        endpoint: endpoint,
        errors: e,
      );
    }
  }
}
