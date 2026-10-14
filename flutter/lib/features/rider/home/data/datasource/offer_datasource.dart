import 'package:dio/dio.dart';
import 'package:swift_app/API/api_error_message.dart';

import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';

import 'package:swift_app/constants/api_response/api_response_format.dart';

import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';

import 'package:swift_app/features/rider/home/data/model/offer_request_model.dart';
import 'package:swift_app/features/rider/home/data/model/offer_response_model.dart';

class OfferDatasource {
  final DioClient _dioClient;
  final RiderApiEndpoints riderApiEndpoints;
  final AuthLocalDatasource authLocalDatasource;

  OfferDatasource({
    required DioClient dioClient,
    required this.riderApiEndpoints,
    required this.authLocalDatasource,
  }) : _dioClient = dioClient;

  // ============================================================
  // FETCH OFFERS
  // ============================================================

  Future<ApiResponseFormat<List<OfferResponseModel>>> fetchOffers(
    OfferRequestModel request,
  ) async {
    final String endpoint = riderApiEndpoints.getOffersEndpoint;

    final int authUserId = authLocalDatasource.userId ?? -1;

    try {
      // ========================================================
      // POST REQUEST
      // ========================================================

      final response = await _dioClient.dio.post(
        endpoint,

        data: request.toJson(),

        options: Options(headers: {'x-user-id': authUserId}),
      );

      final responseData = response.data;

      // ========================================================
      // RESPONSE
      // ========================================================

      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<List<OfferResponseModel>>.fromJson(
          json: responseData,

          httpStatusCode: response.statusCode ?? 500,

          endpoint: endpoint,

          defaultSuccessMessage: 'Offers fetched successfully',

          defaultFailureMessage: 'Failed to fetch offers',

          fromData: (json) {
            if (json is! List) {
              return <OfferResponseModel>[];
            }

            return json
                .whereType<Map<String, dynamic>>()
                .map((item) => OfferResponseModel.fromJson(item))
                .toList();
          },
        );
      }

      return ApiResponseFormat<List<OfferResponseModel>>.failure(
        statusCode: response.statusCode ?? 500,

        message: 'Invalid offers response',

        endpoint: endpoint,

        errors: responseData,
      );
    }
    // ==========================================================
    // DIO ERROR
    // ==========================================================
    on DioException catch (e) {
      final responseData = e.response?.data;

      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<List<OfferResponseModel>>.fromJson(
          json: responseData,

          httpStatusCode: e.response?.statusCode ?? 500,

          endpoint: endpoint,

          defaultSuccessMessage: 'Offers fetched successfully',

          defaultFailureMessage: 'Failed to fetch offers',

          fromData: (json) {
            if (json is! List) {
              return <OfferResponseModel>[];
            }

            return json
                .whereType<Map<String, dynamic>>()
                .map((item) => OfferResponseModel.fromJson(item))
                .toList();
          },
        );
      }

      return ApiResponseFormat<List<OfferResponseModel>>.failure(
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
      return ApiResponseFormat<List<OfferResponseModel>>.failure(
        statusCode: 500,

        message: 'Unexpected error occurred',

        endpoint: endpoint,

        errors: e,
      );
    }
  }
}
