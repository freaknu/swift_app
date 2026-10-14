class RiderApiEndpoints {
  // Authentication Endpoints
  final String loginEndpoint = "api/v1/auth/login";
  final String signupEndpoint = "api/v1/auth/signup";
  final String verifyOtpEndpoint = "api/v1/auth/verifyOtp/";
  final String sendOtpEndpoint = "api/v1/auth/sendOtp/";
  final String googleLoginEndpoint = "api/v1/auth/google";
  final String appleLoginEndpoint = "api/v1/auth/apple";
  final String refreshTokenEndpoint = "api/v1/auth/refresh-token";
  final String logoutEndpoint = "api/v1/auth/logout";
  final String homeReadyEndpoint = "api/v1/auth/home-ready";
  final String validateTokenEndpoint = "api/v1/auth/validate-token";
  final String passwordResetRequestEndpoint =
      "api/v1/auth/password-reset/request";
  final String passwordResetConfirmEndpoint =
      "api/v1/auth/password-reset/confirm";
  final String verifyEmailEndpoint = "api/v1/auth/verify-email";

  String userCredentials(int userId) {
    return "api/v1/user-cred/getCredentials/$userId";
  }

  // RideEndpoints
  final String getAllSavedAddressEndpoint = "api/v1/user/getUserDetails";
  final String saveAddressEndpoint = "api/v1/user/addAddress";
  final String getRideHistoryHomepage = "api/v1/user/getRideHistory";
  final String requestRideEndpoint = "api/v1/ride/requestRide";
  final String driverProfileEndpoint = "api/v1/driver/profile";
  final String driverProfileCompletenessEndpoint =
      "api/v1/driver/profile/completeness";
  final String documentUploadEndpoint = "api/v1/document/upload";
  final String driverDocumentsEndpoint = "api/v1/document";
  final String driverStatusEndpoint = "api/v1/driver/status";
  final String vehiclesEndpoint = "api/v1/vehicle";
  final String vehicleTypesEndpoint = "api/v1/vehicle/types";

  String updateVehicle(int vehicleId) => "$vehiclesEndpoint/$vehicleId";

  String sendOtp(String email) {
    return '$sendOtpEndpoint${Uri.encodeComponent(email)}';
  }

  String verifyOtp(String email, String otp) {
    return '$verifyOtpEndpoint${Uri.encodeComponent(email)}/${Uri.encodeComponent(otp)}';
  }

  String get singupEndpoint => signupEndpoint;
  String get veriryOtpEndpoint => verifyOtpEndpoint;

  String get getOffersEndpoint => "api/v1/offer/getOffers";

  String get calculateEstimate => "api/v1/fare/estimate";

  String get routeEtaEndpoint => "api/v1/location/eta";

  String getRideFare(int rideId) {
    return "api/v1/fare/rides/$rideId/fare";
  }

  String getRideDetail(int rideId) {
    return "api/v1/ride/getRideDetail/$rideId";
  }

  String getRideStatusHistory(int rideId) {
    return "api/v1/ride/getRideHistory/$rideId";
  }

  String getRideHistoryPaginated(int riderId) {
    return "api/v1/ride/getRideHistoryPaginated/$riderId";
  }

  String getActiveRide(int riderId) {
    return "api/v1/ride/riders/$riderId/rides/active";
  }

  String driverArrived(int rideId) {
    return "api/v1/ride/rides/$rideId/arrive";
  }

  String startRide(int rideId) {
    return "api/v1/ride/rides/$rideId/start";
  }

  String completeRide(int rideId) {
    return "api/v1/ride/rides/$rideId/complete";
  }

  String cancelRide(int rideId) {
    return "api/v1/ride/rides/$rideId/cancel";
  }

  String getRideLocationSuggestion(int riderId) {
    return "api/v1/ride/rides/getRideLocationSuggestion/$riderId";
  }

  // User service endpoints
  final String getAllUsersEndpoint = "api/v1/user/getAllUsers";
  final String userDetailsEndpoint = "api/v1/user/getUserDetails";
  final String userPreferencesEndpoint = "api/v1/user/preferences";
  final String favouritePlacesEndpoint = "api/v1/user/favourite-places";
  final String profilePhotoEndpoint = "api/v1/user/profile-photo";

  String updateUser(String deviceId) {
    return "api/v1/user/updateUser/${Uri.encodeComponent(deviceId)}";
  }

  String deleteUser(int userId) {
    return "api/v1/user/delete/$userId";
  }

  String deleteAddress(int userId, int addressId) {
    return "api/v1/user/deleteAddress/$userId/$addressId";
  }

  String getEmergencyContact(int userId) {
    return "api/v1/user/getEmergencyContact/$userId";
  }

  String deleteContact(int userId, int contactId) {
    return "api/v1/user/deleteContact/$userId/$contactId";
  }

  String addContact(int userId) {
    return "api/v1/user/addContact/$userId";
  }

  // Driver service endpoints
  final String driverRegisterEndpoint = "api/v1/driver/register";
  final String driverMiniDetailEndpoint = "api/v1/driver/driverMiniDetail";
  final String driverSuspendEndpoint = "api/v1/driver/suspend";
  final String driverReactivateEndpoint = "api/v1/driver/reactivate";
  final String driverEarningsEndpoint = "api/v1/driver/earnings";
  final String driverNotificationPreferencesEndpoint =
      "api/v1/driver/notification-preferences";
  final String driverDeviceInfoEndpoint = "api/v1/driver/device-info";
  final String driverStaticEndpoint = "api/v1/driverStatic/getDriverStatic";
  // Driver offer endpoints (matching flow via driver service + Kafka)
  final String pendingOffersEndpoint = "api/v1/driver/offers";
  final String driverActiveRideEndpoint = "api/v1/driver/rides/active";

  // Chat service endpoints. Chat sessions are created by the websocket
  // service and messages/history are owned by chat-service.
  String chatSessionByRide(int rideId) {
    return "api/v1/chat/sessions/by-ride/$rideId";
  }

  String chatMessages(int sessionId) {
    return "api/v1/chat/sessions/$sessionId/messages";
  }

  String chatMessageCreate(int sessionId) {
    return chatMessages(sessionId);
  }

  String acceptOffer(int rideId) {
    return "api/v1/driver/offers/$rideId/accept";
  }

  String rejectOffer(int rideId) {
    return "api/v1/driver/offers/$rideId/reject";
  }

  final String vehicleVerifyEndpoint = "api/v1/vehicle/verify";
  final String vehicleDeactivateEndpoint = "api/v1/vehicle/deactivate";

  String documentStatus(int documentId) {
    return "api/v1/document/getDocumentStatus/$documentId";
  }

  final String changeDocumentStatusEndpoint =
      "api/v1/document/changeDocumentStatus";

  String updateDocument(int documentId) {
    return "api/v1/document/updateDocument/$documentId";
  }

  String deleteDocument(int documentId) {
    return "api/v1/document/deleteDocument/$documentId";
  }

  String getDriverPreference(int driverId) {
    return "api/v1/driverPreference/getDriverPreference/$driverId";
  }

  final String addDriverPreferenceEndpoint =
      "api/v1/driverPreference/addDriverPreference";
  final String updateDriverPreferenceEndpoint =
      "api/v1/driverPreference/updateDriverPreference";

  // Location service endpoints
  final String saveLocationEndpoint = "api/v1/location/saveLocation";
  final String completeLocationRideEndpoint = "api/v1/location/completeRide";
  final String nearbyDriversEndpoint = "api/v1/location/nearbyDrivers";

  String getRecentLocation(int rideId, int driverId) {
    return "api/v1/location/getRecentLocation/$rideId/$driverId";
  }

  String getLocationHistory(int rideId, int driverId) {
    return "api/v1/location/getLocationHistory/$rideId/$driverId";
  }

  // Rating service endpoints
  final String getRatingEndpoint = "api/v1/rating/getRating";
  final String createRatingEndpoint = "api/v1/rating/createRating";

  String deleteRating(int rateId) {
    return "api/v1/rating/deleteRating/$rateId";
  }

  // Analytics service endpoints
  final String analyticsDashboardEndpoint = "api/v1/analytics/dashboard";

  String driverAnalytics(int driverId) {
    return "api/v1/analytics/driver/$driverId";
  }

  String riderAnalytics(int riderId) {
    return "api/v1/analytics/rider/$riderId";
  }

  String driverAnalyticsEarnings(int driverId) {
    return "api/v1/analytics/driver/$driverId/earnings";
  }
}
