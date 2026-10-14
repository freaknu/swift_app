import 'package:dio/dio.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';

class BackendApiDatasource {
  final DioClient _dioClient;
  final RiderApiEndpoints _endpoints;
  final AuthLocalDatasource _authLocalDatasource;

  BackendApiDatasource({
    required DioClient dioClient,
    required RiderApiEndpoints endpoints,
    required AuthLocalDatasource authLocalDatasource,
  }) : _dioClient = dioClient,
       _endpoints = endpoints,
       _authLocalDatasource = authLocalDatasource;

  Future<ApiResponseFormat<dynamic>> login(Map<String, dynamic> data) {
    return _publicPost(_endpoints.loginEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> signup(Map<String, dynamic> data) {
    return _publicPost(_endpoints.signupEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> sendOtp(String email) {
    return _publicGet(_endpoints.sendOtp(email));
  }

  Future<ApiResponseFormat<dynamic>> verifyOtp(String email, String otp) {
    return _publicGet(_endpoints.verifyOtp(email, otp));
  }

  Future<ApiResponseFormat<dynamic>> googleLogin(Map<String, dynamic> data) {
    return _publicPost(_endpoints.googleLoginEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> appleLogin(Map<String, dynamic> data) {
    return _publicPost(_endpoints.appleLoginEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> refreshToken(Map<String, dynamic> data) {
    return _publicPost(_endpoints.refreshTokenEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> logout(Map<String, dynamic> data) {
    return _authPost(_endpoints.logoutEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> validateToken(Map<String, dynamic> data) {
    return _publicPost(_endpoints.validateTokenEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> requestPasswordReset(
    Map<String, dynamic> data,
  ) {
    return _publicPost(_endpoints.passwordResetRequestEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> confirmPasswordReset(
    Map<String, dynamic> data,
  ) {
    return _publicPost(_endpoints.passwordResetConfirmEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> verifyEmail(Map<String, dynamic> data) {
    return _publicPost(_endpoints.verifyEmailEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> getCredentials(int userId) {
    return _publicGet(_endpoints.userCredentials(userId));
  }

  Future<ApiResponseFormat<dynamic>> getAllUsers() {
    return _authGet(_endpoints.getAllUsersEndpoint);
  }

  Future<ApiResponseFormat<dynamic>> updateUser({
    required String deviceId,
    required Map<String, dynamic> data,
  }) {
    return _authPost(_endpoints.updateUser(deviceId), data: data);
  }

  Future<ApiResponseFormat<dynamic>> getUserDetails() {
    return _authGet(_endpoints.userDetailsEndpoint);
  }

  Future<ApiResponseFormat<dynamic>> deleteUser(int userId) {
    return _authDelete(_endpoints.deleteUser(userId));
  }

  Future<ApiResponseFormat<dynamic>> addAddress(Map<String, dynamic> data) {
    return _authPost(_endpoints.saveAddressEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> deleteAddress({
    required int userId,
    required int addressId,
  }) {
    return _authDelete(_endpoints.deleteAddress(userId, addressId));
  }

  Future<ApiResponseFormat<dynamic>> getEmergencyContact(int userId) {
    return _authGet(_endpoints.getEmergencyContact(userId));
  }

  Future<ApiResponseFormat<dynamic>> deleteContact({
    required int userId,
    required int contactId,
  }) {
    return _authDelete(_endpoints.deleteContact(userId, contactId));
  }

  Future<ApiResponseFormat<dynamic>> addContact({
    required int userId,
    required List<Map<String, dynamic>> data,
  }) {
    return _authPost(_endpoints.addContact(userId), data: data);
  }

  Future<ApiResponseFormat<dynamic>> getUserRideHistory({
    int page = 0,
    int pageSize = 5,
  }) {
    return _authGet(
      _endpoints.getRideHistoryHomepage,
      queryParameters: {'page': page, 'pageSize': pageSize},
    );
  }

  Future<ApiResponseFormat<dynamic>> updateProfilePhoto(
    Map<String, dynamic> data,
  ) {
    return _authPost(_endpoints.profilePhotoEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> getUserPreferences() {
    return _authGet(_endpoints.userPreferencesEndpoint);
  }

  Future<ApiResponseFormat<dynamic>> updateUserPreferences(
    Map<String, dynamic> data,
  ) {
    return _authPut(_endpoints.userPreferencesEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> getFavouritePlaces() {
    return _authGet(_endpoints.favouritePlacesEndpoint);
  }

  Future<ApiResponseFormat<dynamic>> addFavouritePlace(
    Map<String, dynamic> data,
  ) {
    return _authPost(_endpoints.favouritePlacesEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> getRideTypes({required bool isDriver}) {
    return _authGet(
      'api/v1/ridetype/getAllRideTypes',
      queryParameters: {'isDriver': isDriver},
    );
  }

  Future<ApiResponseFormat<dynamic>> requestRide(Map<String, dynamic> data) {
    return _authPost(_endpoints.requestRideEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> getRideDetail(int rideId) {
    return _authGet(_endpoints.getRideDetail(rideId));
  }

  Future<ApiResponseFormat<dynamic>> getRideStatusHistory(int rideId) {
    return _authGet(_endpoints.getRideStatusHistory(rideId));
  }

  Future<ApiResponseFormat<dynamic>> getRideHistoryPaginated({
    required int riderId,
    int page = 0,
    int size = 20,
  }) {
    return _authGet(
      _endpoints.getRideHistoryPaginated(riderId),
      queryParameters: {'page': page, 'size': size},
    );
  }

  Future<ApiResponseFormat<dynamic>> getActiveRide(int riderId) {
    return _authGet(_endpoints.getActiveRide(riderId));
  }

  Future<ApiResponseFormat<dynamic>> driverArrived({
    required int rideId,
    required int driverId,
  }) {
    return _driverRideAction(_endpoints.driverArrived(rideId), driverId);
  }

  Future<ApiResponseFormat<dynamic>> startRide({
    required int rideId,
    required int driverId,
  }) {
    return _driverRideAction(_endpoints.startRide(rideId), driverId);
  }

  Future<ApiResponseFormat<dynamic>> completeRide({
    required int rideId,
    required int driverId,
  }) {
    return _driverRideAction(_endpoints.completeRide(rideId), driverId);
  }

  Future<ApiResponseFormat<dynamic>> cancelRide({
    required int rideId,
    required int actorId,
    required String actorType,
    required String reason,
  }) {
    return _authPost(
      _endpoints.cancelRide(rideId),
      data: {'actorId': actorId, 'actorType': actorType, 'reason': reason},
    );
  }

  Future<ApiResponseFormat<dynamic>> getRideLocationSuggestion({
    required int riderId,
    required double latitude,
    required double longitude,
  }) {
    return _authPost(
      _endpoints.getRideLocationSuggestion(riderId),
      queryParameters: {'latitude': latitude, 'longitude': longitude},
    );
  }

  Future<ApiResponseFormat<dynamic>> estimateFare(Map<String, dynamic> data) {
    return _authPost(_endpoints.calculateEstimate, data: data);
  }

  Future<ApiResponseFormat<dynamic>> getRideFare(int rideId) {
    return _authGet(_endpoints.getRideFare(rideId));
  }

  Future<ApiResponseFormat<dynamic>> getOffers(Map<String, dynamic> data) {
    return _authPost(_endpoints.getOffersEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> registerDriver(Map<String, dynamic> data) {
    return _authPost(_endpoints.driverRegisterEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> getDriverProfile() {
    return _authGet(_endpoints.driverProfileEndpoint);
  }

  Future<ApiResponseFormat<dynamic>> updateDriverStatus(String driverStatus) {
    return _authPut(
      _endpoints.driverStatusEndpoint,
      queryParameters: {'driverStatus': driverStatus},
    );
  }

  Future<ApiResponseFormat<dynamic>> getDriverMiniDetail() {
    return _authGet(_endpoints.driverMiniDetailEndpoint);
  }

  Future<ApiResponseFormat<dynamic>> suspendDriver(String reason) {
    return _authPut(_endpoints.driverSuspendEndpoint, data: {'reason': reason});
  }

  Future<ApiResponseFormat<dynamic>> reactivateDriver() {
    return _authPut(_endpoints.driverReactivateEndpoint);
  }

  Future<ApiResponseFormat<dynamic>> getDriverEarnings() {
    return _authGet(_endpoints.driverEarningsEndpoint);
  }

  Future<ApiResponseFormat<dynamic>> getDriverNotificationPreferences() {
    return _authGet(_endpoints.driverNotificationPreferencesEndpoint);
  }

  Future<ApiResponseFormat<dynamic>> updateDriverNotificationPreferences(
    Map<String, dynamic> data,
  ) {
    return _authPut(
      _endpoints.driverNotificationPreferencesEndpoint,
      data: data,
    );
  }

  Future<ApiResponseFormat<dynamic>> saveDriverDeviceInfo(
    Map<String, dynamic> data,
  ) {
    return _authPost(_endpoints.driverDeviceInfoEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> getDriverStatic(int driverId) {
    return _authGet(
      _endpoints.driverStaticEndpoint,
      queryParameters: {'driverId': driverId},
    );
  }

  Future<ApiResponseFormat<dynamic>> addVehicle(Map<String, dynamic> data) {
    return _authPost(_endpoints.vehiclesEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> getVehicles() {
    return _authGet(_endpoints.vehiclesEndpoint);
  }

  Future<ApiResponseFormat<dynamic>> verifyVehicle(int vehicleId) {
    return _authPut(
      _endpoints.vehicleVerifyEndpoint,
      queryParameters: {'vehicleId': vehicleId},
    );
  }

  Future<ApiResponseFormat<dynamic>> deactivateVehicle(int vehicleId) {
    return _authDelete(
      _endpoints.vehicleDeactivateEndpoint,
      queryParameters: {'vehicleId': vehicleId},
    );
  }

  Future<ApiResponseFormat<dynamic>> getDocumentStatus(int documentId) {
    return _authGet(_endpoints.documentStatus(documentId));
  }

  Future<ApiResponseFormat<dynamic>> changeDocumentStatus(
    Map<String, dynamic> data,
  ) {
    return _authPost(_endpoints.changeDocumentStatusEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> updateDocument({
    required int documentId,
    required Map<String, dynamic> data,
  }) {
    return _authPut(_endpoints.updateDocument(documentId), data: data);
  }

  Future<ApiResponseFormat<dynamic>> deleteDocument(int documentId) {
    return _authDelete(_endpoints.deleteDocument(documentId));
  }

  Future<ApiResponseFormat<dynamic>> getDriverPreference(int driverId) {
    return _authGet(_endpoints.getDriverPreference(driverId));
  }

  Future<ApiResponseFormat<dynamic>> addDriverPreference(
    Map<String, dynamic> data,
  ) {
    return _authPost(_endpoints.addDriverPreferenceEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> updateDriverPreference(
    Map<String, dynamic> data,
  ) {
    return _authPut(_endpoints.updateDriverPreferenceEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> saveLocation(Map<String, dynamic> data) {
    return _authPost(_endpoints.saveLocationEndpoint, data: data);
  }

  Future<ApiResponseFormat<dynamic>> getRecentLocation({
    required int rideId,
    required int driverId,
  }) {
    return _authGet(_endpoints.getRecentLocation(rideId, driverId));
  }

  Future<ApiResponseFormat<dynamic>> getLocationHistory({
    required int rideId,
    required int driverId,
  }) {
    return _authGet(_endpoints.getLocationHistory(rideId, driverId));
  }

  Future<ApiResponseFormat<dynamic>> completeLocationRide(int rideId) {
    return _authPost(
      _endpoints.completeLocationRideEndpoint,
      queryParameters: {'rideId': rideId},
    );
  }

  Future<ApiResponseFormat<dynamic>> getNearbyDrivers({
    required double latitude,
    required double longitude,
    required double distance,
  }) {
    return _authGet(
      _endpoints.nearbyDriversEndpoint,
      queryParameters: {
        'latitude': latitude,
        'longitude': longitude,
        'distance': distance,
      },
    );
  }

  Future<ApiResponseFormat<dynamic>> getRating({int? driverId, int? riderId}) {
    if ((driverId == null) == (riderId == null)) {
      throw ArgumentError('Provide exactly one rating recipient ID');
    }
    final queryParameters = <String, dynamic>{};
    if (driverId != null) {
      queryParameters['driverId'] = driverId;
    } else {
      queryParameters['riderId'] = riderId;
    }
    return _authGet(
      _endpoints.getRatingEndpoint,
      queryParameters: queryParameters,
    );
  }

  Future<ApiResponseFormat<dynamic>> createRating({
    int? ratingId,
    required Map<String, dynamic> data,
  }) {
    return _authPost(
      _endpoints.createRatingEndpoint,
      data: data,
      queryParameters: ratingId == null ? null : {'ratingId': ratingId},
    );
  }

  Future<ApiResponseFormat<dynamic>> deleteRating(int rateId) {
    return _authDelete(_endpoints.deleteRating(rateId));
  }

  Future<ApiResponseFormat<dynamic>> getDriverAnalytics(int driverId) {
    return _authGet(_endpoints.driverAnalytics(driverId));
  }

  Future<ApiResponseFormat<dynamic>> getRiderAnalytics(int riderId) {
    return _authGet(_endpoints.riderAnalytics(riderId));
  }

  Future<ApiResponseFormat<dynamic>> getDashboardMetrics() {
    return _authGet(_endpoints.analyticsDashboardEndpoint);
  }

  Future<ApiResponseFormat<dynamic>> getDriverAnalyticsEarnings({
    required int driverId,
    String? period,
  }) {
    return _authGet(
      _endpoints.driverAnalyticsEarnings(driverId),
      queryParameters: {
        if (period != null && period.isNotEmpty) 'period': period,
      },
    );
  }

  Future<ApiResponseFormat<dynamic>> _driverRideAction(
    String endpoint,
    int driverId,
  ) {
    return _authPost(endpoint, data: {'driverId': driverId});
  }

  Future<ApiResponseFormat<dynamic>> _publicGet(String endpoint) {
    return _send(
      endpoint: endpoint,
      request: () => _dioClient.dio.get(endpoint),
    );
  }

  Future<ApiResponseFormat<dynamic>> _publicPost(
    String endpoint, {
    Object? data,
    Map<String, dynamic>? queryParameters,
  }) {
    return _send(
      endpoint: endpoint,
      request: () => _dioClient.dio.post(
        endpoint,
        data: data,
        queryParameters: queryParameters,
      ),
    );
  }

  Future<ApiResponseFormat<dynamic>> _authGet(
    String endpoint, {
    Map<String, dynamic>? queryParameters,
  }) {
    return _authSend(
      endpoint: endpoint,
      request: (options) => _dioClient.dio.get(
        endpoint,
        queryParameters: queryParameters,
        options: options,
      ),
    );
  }

  Future<ApiResponseFormat<dynamic>> _authPost(
    String endpoint, {
    Object? data,
    Map<String, dynamic>? queryParameters,
  }) {
    return _authSend(
      endpoint: endpoint,
      request: (options) => _dioClient.dio.post(
        endpoint,
        data: data,
        queryParameters: queryParameters,
        options: options,
      ),
    );
  }

  Future<ApiResponseFormat<dynamic>> _authPut(
    String endpoint, {
    Object? data,
    Map<String, dynamic>? queryParameters,
  }) {
    return _authSend(
      endpoint: endpoint,
      request: (options) => _dioClient.dio.put(
        endpoint,
        data: data,
        queryParameters: queryParameters,
        options: options,
      ),
    );
  }

  Future<ApiResponseFormat<dynamic>> _authDelete(
    String endpoint, {
    Object? data,
    Map<String, dynamic>? queryParameters,
  }) {
    return _authSend(
      endpoint: endpoint,
      request: (options) => _dioClient.dio.delete(
        endpoint,
        data: data,
        queryParameters: queryParameters,
        options: options,
      ),
    );
  }

  Future<ApiResponseFormat<dynamic>> _authSend({
    required String endpoint,
    required Future<Response<dynamic>> Function(Options options) request,
  }) {
    final authUserId = _authLocalDatasource.userId;

    if (authUserId == null) {
      return Future.value(
        ApiResponseFormat<dynamic>.failure(
          statusCode: 401,
          message: 'Authenticated user not found',
          endpoint: endpoint,
        ),
      );
    }

    return _send(
      endpoint: endpoint,
      request: () => request(Options(headers: {'x-user-id': authUserId})),
    );
  }

  Future<ApiResponseFormat<dynamic>> _send({
    required String endpoint,
    required Future<Response<dynamic>> Function() request,
  }) async {
    try {
      final response = await request();
      return _parseResponse(
        endpoint: endpoint,
        httpStatusCode: response.statusCode ?? 500,
        responseData: response.data,
      );
    } on DioException catch (e) {
      return _parseResponse(
        endpoint: endpoint,
        httpStatusCode: e.response?.statusCode ?? 500,
        responseData: e.response?.data,
        fallbackMessage: e.message ?? 'Request failed',
      );
    } catch (e) {
      return ApiResponseFormat<dynamic>.failure(
        statusCode: 500,
        message: 'Unexpected error occurred',
        endpoint: endpoint,
        errors: e,
      );
    }
  }

  ApiResponseFormat<dynamic> _parseResponse({
    required String endpoint,
    required int httpStatusCode,
    required dynamic responseData,
    String fallbackMessage = 'Request failed',
  }) {
    if (responseData is Map<String, dynamic>) {
      return ApiResponseFormat<dynamic>.fromJson(
        json: responseData,
        httpStatusCode: httpStatusCode,
        endpoint: endpoint,
        fromData: (json) => json,
        defaultFailureMessage: fallbackMessage,
      );
    }

    if (responseData is Map) {
      return ApiResponseFormat<dynamic>.fromJson(
        json: Map<String, dynamic>.from(responseData),
        httpStatusCode: httpStatusCode,
        endpoint: endpoint,
        fromData: (json) => json,
        defaultFailureMessage: fallbackMessage,
      );
    }

    final isSuccess = httpStatusCode >= 200 && httpStatusCode < 300;

    if (isSuccess) {
      return ApiResponseFormat<dynamic>.success(
        data: responseData,
        statusCode: httpStatusCode,
        message: 'Request successful',
        endpoint: endpoint,
      );
    }

    return ApiResponseFormat<dynamic>.failure(
      statusCode: httpStatusCode,
      message: fallbackMessage,
      endpoint: endpoint,
      errors: responseData,
    );
  }
}
