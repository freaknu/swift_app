import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:swift_app/API/api_error_message.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/driver/data/model/driver_profile_model.dart';
import 'package:swift_app/features/driver/data/model/driver_statistics_model.dart';
import 'package:swift_app/features/driver/data/model/driver_earnings_model.dart';
import 'package:swift_app/features/driver/data/model/driver_rating_model.dart';
import 'package:swift_app/features/driver/data/model/driver_notification_preferences_model.dart';
import 'package:swift_app/features/driver/data/model/driver_journey_model.dart';
import 'package:swift_app/features/driver/data/model/driver_rider_summary_model.dart';
import 'package:swift_app/features/driver/data/model/driver_document_model.dart';
import 'package:swift_app/features/driver/data/model/profile_completeness_model.dart';
import 'package:swift_app/features/driver/data/model/vehicle_type_option_model.dart';
import 'package:swift_app/features/driver/data/model/pending_offer_model.dart';
import 'package:swift_app/features/rider/ride/data/model/ride_response_model.dart';

class DriverRemoteDatasource {
  final DioClient _dioClient;
  final RiderApiEndpoints _endpoints;
  final AuthLocalDatasource _authLocalDatasource;

  DriverRemoteDatasource({
    required DioClient dioClient,
    required RiderApiEndpoints endpoints,
    required AuthLocalDatasource authLocalDatasource,
  }) : _dioClient = dioClient,
       _endpoints = endpoints,
       _authLocalDatasource = authLocalDatasource;

  Future<ApiResponseFormat<DriverProfileModel>> getProfile() async {
    final endpoint = _endpoints.driverProfileEndpoint;

    return _send<DriverProfileModel>(
      endpoint: endpoint,
      request: () => _dioClient.dio.get(endpoint, options: _authOptions()),
      fromData: (json) =>
          DriverProfileModel.fromJson(json as Map<String, dynamic>),
      defaultFailureMessage: 'Failed to fetch driver profile',
    );
  }

  Future<ApiResponseFormat<DriverStatisticsModel>> getStatistics(
    int driverId,
  ) async {
    final endpoint = _endpoints.driverStaticEndpoint;
    return _send<DriverStatisticsModel>(
      endpoint: endpoint,
      request: () => _dioClient.dio.get(
        endpoint,
        queryParameters: {'driverId': driverId},
        options: _authOptions(),
      ),
      fromData: (json) =>
          DriverStatisticsModel.fromJson(json as Map<String, dynamic>),
      defaultFailureMessage: 'Failed to fetch driver statistics',
    );
  }

  Future<ApiResponseFormat<DriverJourneyPage>> getJourneys({
    int page = 0,
    int size = 20,
    String? status,
  }) async {
    const endpoint = 'api/v1/ride/drivers/me/rides/history';
    final queryParameters = <String, Object>{'page': page, 'size': size};
    if (status != null) queryParameters['status'] = status;
    return _send<DriverJourneyPage>(
      endpoint: endpoint,
      request: () => _dioClient.dio.get(
        endpoint,
        queryParameters: queryParameters,
        options: _authOptions(),
      ),
      fromData: (json) =>
          DriverJourneyPage.fromJson(json as Map<String, dynamic>),
      defaultFailureMessage: 'Failed to fetch your ride history',
    );
  }

  Future<ApiResponseFormat<DriverEarningsModel>> getEarnings() async {
    final endpoint = _endpoints.driverEarningsEndpoint;
    return _send<DriverEarningsModel>(
      endpoint: endpoint,
      request: () => _dioClient.dio.get(endpoint, options: _authOptions()),
      fromData: (json) =>
          DriverEarningsModel.fromJson(json as Map<String, dynamic>),
      defaultFailureMessage: 'Failed to fetch earnings',
    );
  }

  Future<ApiResponseFormat<DriverRatingModel>> getRating(int driverId) async {
    final endpoint = _endpoints.getRatingEndpoint;
    return _send<DriverRatingModel>(
      endpoint: endpoint,
      request: () => _dioClient.dio.get(
        endpoint,
        queryParameters: {'driverId': driverId},
        options: _authOptions(),
      ),
      fromData: (json) =>
          DriverRatingModel.fromJson(json as Map<String, dynamic>),
      defaultFailureMessage: 'Failed to fetch driver ratings',
    );
  }

  Future<ApiResponseFormat<DriverNotificationPreferencesModel>>
  getNotificationPreferences() async {
    final endpoint = _endpoints.driverNotificationPreferencesEndpoint;
    return _send<DriverNotificationPreferencesModel>(
      endpoint: endpoint,
      request: () => _dioClient.dio.get(endpoint, options: _authOptions()),
      fromData: (json) => DriverNotificationPreferencesModel.fromJson(
        json as Map<String, dynamic>,
      ),
      defaultFailureMessage: 'Failed to fetch notification preferences',
    );
  }

  Future<ApiResponseFormat<DriverNotificationPreferencesModel>>
  updateNotificationPreferences(
    DriverNotificationPreferencesModel preferences,
  ) async {
    final endpoint = _endpoints.driverNotificationPreferencesEndpoint;
    return _send<DriverNotificationPreferencesModel>(
      endpoint: endpoint,
      request: () => _dioClient.dio.put(
        endpoint,
        data: preferences.toJson(),
        options: _authOptions(),
      ),
      fromData: (json) => DriverNotificationPreferencesModel.fromJson(
        json as Map<String, dynamic>,
      ),
      defaultFailureMessage: 'Failed to save notification preferences',
    );
  }

  Future<ApiResponseFormat<List<DriverVehicleModel>>> getVehicles() async {
    final endpoint = _endpoints.vehiclesEndpoint;

    return _send<List<DriverVehicleModel>>(
      endpoint: endpoint,
      request: () => _dioClient.dio.get(endpoint, options: _authOptions()),
      fromData: (json) {
        if (json is! List) return <DriverVehicleModel>[];
        return json
            .whereType<Map<String, dynamic>>()
            .map(DriverVehicleModel.fromJson)
            .toList();
      },
      defaultFailureMessage: 'Failed to fetch vehicles',
    );
  }

  Future<ApiResponseFormat<List<VehicleTypeOption>>> getVehicleTypes() async {
    final endpoint = _endpoints.vehicleTypesEndpoint;
    return _send<List<VehicleTypeOption>>(
      endpoint: endpoint,
      request: () => _dioClient.dio.get(endpoint, options: _authOptions()),
      fromData: (json) => json is List
          ? json
                .whereType<Map<String, dynamic>>()
                .map(VehicleTypeOption.fromJson)
                .where((option) => option.code.isNotEmpty)
                .toList()
          : <VehicleTypeOption>[],
      defaultFailureMessage: 'Failed to fetch vehicle types',
    );
  }

  Future<ApiResponseFormat<List<DriverDocumentModel>>> getDocuments() async {
    final endpoint = _endpoints.driverDocumentsEndpoint;
    return _send<List<DriverDocumentModel>>(
      endpoint: endpoint,
      request: () => _dioClient.dio.get(endpoint, options: _authOptions()),
      fromData: (json) => json is List
          ? json
                .whereType<Map<String, dynamic>>()
                .map(DriverDocumentModel.fromJson)
                .toList()
          : <DriverDocumentModel>[],
      defaultFailureMessage: 'Failed to fetch documents',
    );
  }

  Future<ApiResponseFormat<bool>> updateStatus(String driverStatus) async {
    final endpoint = _endpoints.driverStatusEndpoint;

    return _send<bool>(
      endpoint: endpoint,
      request: () => _dioClient.dio.put(
        endpoint,
        queryParameters: {'driverStatus': driverStatus},
        options: _authOptions(),
      ),
      fromData: (json) => json == true || json?.toString() == 'true',
      defaultFailureMessage: 'Failed to update driver status',
    );
  }

  /// Publishes a driver's position through the gateway. The location service
  /// resolves availability and vehicle type from the authenticated profile.
  Future<ApiResponseFormat<bool>> publishDriverLocation({
    required int driverId,
    required double latitude,
    required double longitude,
    int? rideId,
  }) {
    final endpoint = _endpoints.saveLocationEndpoint;
    return _send<bool>(
      endpoint: endpoint,
      request: () => _dioClient.dio.post(
        endpoint,
        data: {
          'driverId': driverId,
          'rideId': rideId,
          'latitude': latitude,
          'longitude': longitude,
        },
        options: _authOptions(),
      ),
      fromData: (json) => json is List,
      defaultFailureMessage: 'Could not share your location',
    );
  }

  Future<ApiResponseFormat<RideResponseModel>> driverArrived({
    required int rideId,
    required int driverId,
  }) {
    return _driverOnlyRideAction(
      endpoint: _endpoints.driverArrived(rideId),
      driverId: driverId,
      failureMessage: 'Failed to mark arrival',
    );
  }

  Future<ApiResponseFormat<RideResponseModel>> startRide({
    required int rideId,
    required int driverId,
  }) {
    return _driverOnlyRideAction(
      endpoint: _endpoints.startRide(rideId),
      driverId: driverId,
      failureMessage: 'Failed to start ride',
    );
  }

  Future<ApiResponseFormat<RideResponseModel>> completeRide({
    required int rideId,
    required int driverId,
  }) {
    return _driverOnlyRideAction(
      endpoint: _endpoints.completeRide(rideId),
      driverId: driverId,
      failureMessage: 'Failed to complete ride',
    );
  }

  Future<ApiResponseFormat<RideResponseModel>> cancelRide({
    required int rideId,
    required int driverId,
    required String reason,
  }) {
    return _rideAction(
      endpoint: _endpoints.cancelRide(rideId),
      data: {'actorId': driverId, 'actorType': 'DRIVER', 'reason': reason},
      failureMessage: 'Failed to cancel ride',
    );
  }

  Future<ApiResponseFormat<List<PendingOfferModel>>> getPendingOffers() async {
    final endpoint = _endpoints.pendingOffersEndpoint;

    return _send<List<PendingOfferModel>>(
      endpoint: endpoint,
      request: () => _dioClient.dio.get(endpoint, options: _authOptions()),
      fromData: (json) {
        if (json is! List) return <PendingOfferModel>[];
        return json
            .whereType<Map<String, dynamic>>()
            .map(PendingOfferModel.fromJson)
            .toList();
      },
      defaultFailureMessage: 'Failed to fetch pending offers',
    );
  }

  /// Loads route and fare details if a realtime frame arrives before the
  /// enriched driver offer has been staged in the inbox.
  Future<ApiResponseFormat<RideResponseModel>> getRideDetail(int rideId) {
    final endpoint = _endpoints.getRideDetail(rideId);
    return _send<RideResponseModel>(
      endpoint: endpoint,
      request: () => _dioClient.dio.get(endpoint, options: _authOptions()),
      fromData: (json) =>
          RideResponseModel.fromJson(json as Map<String, dynamic>),
      defaultFailureMessage: 'Failed to fetch ride details',
    );
  }

  Future<ApiResponseFormat<DriverRiderSummaryModel>> getRiderSummary(
    int rideId,
  ) {
    final endpoint = 'api/v1/ride/rides/$rideId/rider-summary';
    return _send<DriverRiderSummaryModel>(
      endpoint: endpoint,
      request: () => _dioClient.dio.get(endpoint, options: _authOptions()),
      fromData: (json) =>
          DriverRiderSummaryModel.fromJson(json as Map<String, dynamic>),
      defaultFailureMessage: 'Failed to load rider details',
    );
  }

  /// Returns the ride assigned to this driver across app restarts.
  Future<ApiResponseFormat<int?>> getActiveRideId() {
    final endpoint = _endpoints.driverActiveRideEndpoint;
    return _send<int?>(
      endpoint: endpoint,
      request: () => _dioClient.dio.get(endpoint, options: _authOptions()),
      fromData: (json) =>
          json is num ? json.toInt() : int.tryParse(json?.toString() ?? ''),
      defaultFailureMessage: 'Failed to fetch active ride',
    );
  }

  Future<ApiResponseFormat<bool>> acceptOffer({required int rideId}) async {
    final endpoint = _endpoints.acceptOffer(rideId);

    return _send<bool>(
      endpoint: endpoint,
      request: () => _dioClient.dio.post(endpoint, options: _authOptions()),
      fromData: (json) => json == true || json?.toString() == 'true',
      defaultFailureMessage: 'Failed to accept offer',
    );
  }

  Future<ApiResponseFormat<bool>> rejectOffer({
    required int rideId,
    String? reason,
  }) async {
    final endpoint = _endpoints.rejectOffer(rideId);

    return _send<bool>(
      endpoint: endpoint,
      request: () => _dioClient.dio.post(
        endpoint,
        queryParameters: reason == null ? null : {'reason': reason},
        options: _authOptions(),
      ),
      fromData: (json) => json == true || json?.toString() == 'true',
      defaultFailureMessage: 'Failed to reject offer',
    );
  }

  Future<ApiResponseFormat<RideResponseModel>> _driverOnlyRideAction({
    required String endpoint,
    required int driverId,
    required String failureMessage,
  }) {
    return _rideAction(
      endpoint: endpoint,
      data: {'driverId': driverId},
      failureMessage: failureMessage,
    );
  }

  Future<ApiResponseFormat<RideResponseModel>> _rideAction({
    required String endpoint,
    required Map<String, dynamic> data,
    required String failureMessage,
  }) {
    return _send<RideResponseModel>(
      endpoint: endpoint,
      request: () =>
          _dioClient.dio.post(endpoint, data: data, options: _authOptions()),
      fromData: (json) =>
          RideResponseModel.fromJson(json as Map<String, dynamic>),
      defaultFailureMessage: failureMessage,
    );
  }

  /// Called when the driver app opens: returns whether the driver's
  /// profile is complete across personal details, documents, and vehicle.
  Future<ApiResponseFormat<ProfileCompletenessModel>>
  getProfileCompleteness() async {
    final endpoint = _endpoints.driverProfileCompletenessEndpoint;

    return _send<ProfileCompletenessModel>(
      endpoint: endpoint,
      request: () => _dioClient.dio.get(endpoint, options: _authOptions()),
      fromData: (json) =>
          ProfileCompletenessModel.fromJson(json as Map<String, dynamic>),
      defaultFailureMessage: 'Failed to fetch profile completeness',
    );
  }

  /// Saves the driver's personal/profile details. The backend register
  /// endpoint upserts: only the supplied fields are updated.
  Future<ApiResponseFormat<Map<String, dynamic>>> updateDriverProfile(
    Map<String, dynamic> data,
  ) async {
    final endpoint = _endpoints.driverRegisterEndpoint;

    return _send<Map<String, dynamic>>(
      endpoint: endpoint,
      request: () =>
          _dioClient.dio.post(endpoint, data: data, options: _authOptions()),
      fromData: (json) =>
          json is Map<String, dynamic> ? json : <String, dynamic>{},
      defaultFailureMessage: 'Failed to update driver profile',
    );
  }

  /// Adds a vehicle for the driver.
  Future<ApiResponseFormat<DriverVehicleModel>> addVehicle(
    Map<String, dynamic> data,
  ) async {
    final endpoint = _endpoints.vehiclesEndpoint;

    return _send<DriverVehicleModel>(
      endpoint: endpoint,
      request: () =>
          _dioClient.dio.post(endpoint, data: data, options: _authOptions()),
      fromData: (json) =>
          DriverVehicleModel.fromJson(json as Map<String, dynamic>),
      defaultFailureMessage: 'Failed to add vehicle',
    );
  }

  Future<ApiResponseFormat<DriverVehicleModel>> updateVehicle(
    int vehicleId,
    Map<String, dynamic> data,
  ) async {
    final endpoint = _endpoints.updateVehicle(vehicleId);
    return _send<DriverVehicleModel>(
      endpoint: endpoint,
      request: () =>
          _dioClient.dio.put(endpoint, data: data, options: _authOptions()),
      fromData: (json) =>
          DriverVehicleModel.fromJson(json as Map<String, dynamic>),
      defaultFailureMessage: 'Failed to save vehicle',
    );
  }

  /// Uploads one driver document (driving licence, Aadhaar, PAN).
  Future<ApiResponseFormat<DriverDocumentModel>> uploadDocument({
    required String documentType,
    required String documentUrl,
  }) async {
    final endpoint = _endpoints.documentUploadEndpoint;

    return _send<DriverDocumentModel>(
      endpoint: endpoint,
      request: () => _dioClient.dio.post(
        endpoint,
        data: {'documentType': documentType, 'documentUrl': documentUrl},
        options: _authOptions(),
      ),
      fromData: (json) =>
          DriverDocumentModel.fromJson(json as Map<String, dynamic>),
      defaultFailureMessage: 'Failed to upload document',
    );
  }

  Future<ApiResponseFormat<DriverDocumentModel>> uploadDocumentFile({
    required String documentType,
    required String fileName,
    required Uint8List bytes,
  }) async {
    const endpoint = 'api/v1/document/upload-file';
    return _send<DriverDocumentModel>(
      endpoint: endpoint,
      request: () => _dioClient.dio.post(
        endpoint,
        data: FormData.fromMap({
          'documentType': documentType,
          'file': MultipartFile.fromBytes(bytes, filename: fileName),
        }),
        options: Options(
          contentType: 'multipart/form-data',
          headers: {'x-user-id': _authLocalDatasource.userId},
        ),
      ),
      fromData: (json) =>
          DriverDocumentModel.fromJson(json as Map<String, dynamic>),
      defaultFailureMessage: 'Failed to upload document file',
    );
  }

  Future<ApiResponseFormat<T>> _send<T>({
    required String endpoint,
    required Future<Response<dynamic>> Function() request,
    required T Function(dynamic json) fromData,
    required String defaultFailureMessage,
  }) async {
    final authUserId = _authLocalDatasource.userId;

    if (authUserId == null) {
      return ApiResponseFormat<T>.failure(
        statusCode: 401,
        message: 'Authenticated user not found',
        endpoint: endpoint,
      );
    }

    try {
      final response = await request();
      final responseData = response.data;

      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<T>.fromJson(
          json: responseData,
          httpStatusCode: response.statusCode ?? 500,
          endpoint: endpoint,
          defaultFailureMessage: defaultFailureMessage,
          fromData: fromData,
        );
      }

      return ApiResponseFormat<T>.failure(
        statusCode: response.statusCode ?? 500,
        message: 'Invalid server response',
        endpoint: endpoint,
        errors: responseData,
      );
    } on DioException catch (e) {
      final responseData = e.response?.data;

      if (responseData is Map<String, dynamic>) {
        return ApiResponseFormat<T>.fromJson(
          json: responseData,
          httpStatusCode: e.response?.statusCode ?? 500,
          endpoint: endpoint,
          defaultFailureMessage: defaultFailureMessage,
          fromData: fromData,
        );
      }

      return ApiResponseFormat<T>.failure(
        statusCode: e.response?.statusCode ?? 500,
        message: userFacingDioMessage(e),
        endpoint: endpoint,
        errors: responseData,
      );
    } catch (e) {
      return ApiResponseFormat<T>.failure(
        statusCode: 500,
        message: 'Unexpected error occurred',
        endpoint: endpoint,
        errors: e,
      );
    }
  }

  Options _authOptions() {
    return Options(headers: {'x-user-id': _authLocalDatasource.userId});
  }
}
