import 'package:dio/dio.dart';
import 'package:swift_app/API/api_error_message.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/rider/home/data/model/address_create_model.dart';
import 'package:swift_app/features/rider/home/data/model/user_address_model.dart';
import 'package:swift_app/features/rider/home/data/model/user_response_model.dart';
import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';

class UserRemoteDatasource {
  final DioClient _dioClient;
  final RiderApiEndpoints riderApiEndpoints;
  final AuthLocalDatasource authLocalDatasource;

  UserRemoteDatasource({
    required DioClient dioClient,
    required this.riderApiEndpoints,
    required this.authLocalDatasource,
  }) : _dioClient = dioClient;
  Future<ApiResponseFormat<UserAddressModel>> addAddress(
    AddressCreateModel data,
  ) async {
    final String endpoint = riderApiEndpoints.saveAddressEndpoint;

    final int authUserId = authLocalDatasource.userId ?? -1;

    try {
      final response = await _dioClient.dio.post(
        endpoint,
        data: data.toJson(),
        options: Options(headers: {'x-user-id': authUserId}),
      );

      final responseData = response.data;

      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<UserAddressModel>.fromJson(
          json: responseData,
          httpStatusCode: response.statusCode ?? 500,
          endpoint: endpoint,
          defaultSuccessMessage: 'Address added successfully',
          defaultFailureMessage: 'Failed to add address',
          fromData: (json) => _createdAddress(json, data),
        );
      }

      return ApiResponseFormat<UserAddressModel>.failure(
        statusCode: response.statusCode ?? 500,
        message: 'Invalid address response',
        endpoint: endpoint,
        errors: responseData,
      );
    } on DioException catch (e) {
      final responseData = e.response?.data;

      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<UserAddressModel>.fromJson(
          json: responseData,
          httpStatusCode: e.response?.statusCode ?? 500,
          endpoint: endpoint,
          defaultFailureMessage: 'Failed to add address',
          fromData: (json) => _createdAddress(json, data),
        );
      }

      return ApiResponseFormat<UserAddressModel>.failure(
        statusCode: e.response?.statusCode ?? 500,
        message: userFacingDioMessage(e),
        endpoint: endpoint,
        errors: responseData,
      );
    } catch (e) {
      return ApiResponseFormat<UserAddressModel>.failure(
        statusCode: 500,
        message: 'Unexpected error occurred',
        endpoint: endpoint,
        errors: e,
      );
    }
  }

  // The backend returns the complete saved-place list after an insert.
  // Select the submitted address instead of treating that list as one map.
  static UserAddressModel _createdAddress(
    dynamic json,
    AddressCreateModel sent,
  ) {
    if (json is Map<String, dynamic>) return UserAddressModel.fromJson(json);
    if (json is List) {
      final addresses = json
          .whereType<Map<String, dynamic>>()
          .map(UserAddressModel.fromJson)
          .toList();
      final payload = sent.toJson();
      final matches =
          addresses
              .where(
                (address) =>
                    address.label == payload['label'] &&
                    (address.latitude - (payload['latitude'] as num).toDouble())
                            .abs() <
                        0.000001 &&
                    (address.longitude -
                                (payload['longitude'] as num).toDouble())
                            .abs() <
                        0.000001,
              )
              .toList()
            ..sort((a, b) => b.id.compareTo(a.id));
      if (matches.isNotEmpty) return matches.first;
    }
    throw const FormatException('Created saved place is missing from response');
  }

  Future<ApiResponseFormat<UserReponse>> fetchUserDetail() async {
    final String endpoint = riderApiEndpoints.getAllSavedAddressEndpoint;

    final int authUserId = authLocalDatasource.userId ?? -1;

    try {
      final response = await _dioClient.dio.get(
        endpoint,
        options: Options(headers: {'x-user-id': authUserId}),
      );

      final responseData = response.data;

      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<UserReponse>.fromJson(
          json: responseData,
          httpStatusCode: response.statusCode ?? 500,
          endpoint: endpoint,
          defaultSuccessMessage: 'User details fetched successfully',
          defaultFailureMessage: 'Failed to fetch user details',
          fromData: (json) =>
              UserResponseModel.fromJson(json as Map<String, dynamic>),
        );
      }

      return ApiResponseFormat<UserReponse>.failure(
        statusCode: response.statusCode ?? 500,
        message: 'Invalid user response',
        endpoint: endpoint,
        errors: responseData,
      );
    } on DioException catch (e) {
      final responseData = e.response?.data;

      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<UserReponse>.fromJson(
          json: responseData,
          httpStatusCode: e.response?.statusCode ?? 500,
          endpoint: endpoint,
          defaultFailureMessage: 'Failed to fetch user details',
          fromData: (json) =>
              UserResponseModel.fromJson(json as Map<String, dynamic>),
        );
      }

      return ApiResponseFormat<UserReponse>.failure(
        statusCode: e.response?.statusCode ?? 500,
        message: userFacingDioMessage(e),
        endpoint: endpoint,
        errors: responseData,
      );
    } catch (e) {
      return ApiResponseFormat<UserReponse>.failure(
        statusCode: 500,
        message: 'Unexpected error occurred',
        endpoint: endpoint,
        errors: e,
      );
    }
  }
}
