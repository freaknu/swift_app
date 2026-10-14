import 'package:dio/dio.dart';

/// Returns a short message safe to display when Dio has no structured API
/// response. Dio's own message can contain timeout settings, URLs and other
/// implementation details that do not help someone retry the action.
String userFacingDioMessage(DioException error) {
  switch (error.type) {
    case DioExceptionType.connectionTimeout:
    case DioExceptionType.sendTimeout:
    case DioExceptionType.receiveTimeout:
    case DioExceptionType.transformTimeout:
      return 'Connection timed out. Check your connection and try again.';
    case DioExceptionType.connectionError:
      return 'Unable to connect. Check your internet connection and try again.';
    case DioExceptionType.badCertificate:
      return 'Secure connection failed. Please try again.';
    case DioExceptionType.cancel:
      return 'Request cancelled. Please try again.';
    case DioExceptionType.badResponse:
      final status = error.response?.statusCode;
      if (status == 401) return 'Session expired. Please sign in again.';
      if (status == 403) return 'You do not have access to this action.';
      if (status == 429) {
        return 'Too many requests. Please wait and try again.';
      }
      if (status != null && status >= 500) {
        return 'Service is temporarily unavailable. Please try again.';
      }
      return 'Unable to complete this request. Please try again.';
    case DioExceptionType.unknown:
      return 'Unable to complete this request. Please try again.';
  }
}
