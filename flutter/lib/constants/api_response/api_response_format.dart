class ApiResponseFormat<T> {
  final T? data;
  final int statusCode;
  final String message;
  final String endpoint;
  final bool success;
  final Object? errors;

  const ApiResponseFormat({
    this.data,
    required this.statusCode,
    required this.message,
    required this.endpoint,
    required this.success,
    this.errors,
  });

  factory ApiResponseFormat.success({
    required T data,
    required int statusCode,
    required String message,
    required String endpoint,
  }) {
    return ApiResponseFormat<T>(
      data: data,
      statusCode: statusCode,
      message: message,
      endpoint: endpoint,
      success: true,
      errors: null,
    );
  }

  factory ApiResponseFormat.failure({
    required int statusCode,
    required String message,
    required String endpoint,
    Object? errors,
  }) {
    return ApiResponseFormat<T>(
      data: null,
      statusCode: statusCode,
      message: message,
      endpoint: endpoint,
      success: false,
      errors: errors,
    );
  }

  factory ApiResponseFormat.fromJson({
    required Map<String, dynamic> json,
    required int httpStatusCode,
    required String endpoint,
    required T Function(dynamic data) fromData,
    String defaultSuccessMessage = 'Request successful',
    String defaultFailureMessage = 'Request failed',
  }) {
    final int statusCode = _readStatusCode(json, httpStatusCode);
    final String responseEndpoint = json['endpoint']?.toString() ?? endpoint;
    final String message =
        json['message']?.toString() ??
        (statusCode >= 200 && statusCode < 300
            ? defaultSuccessMessage
            : defaultFailureMessage);
    final Object? errors = json['errors'];
    final bool isSuccess =
        (json['success'] is bool ? json['success'] as bool : null) ??
        (statusCode >= 200 && statusCode < 300);

    if (!isSuccess || statusCode < 200 || statusCode >= 300) {
      return ApiResponseFormat<T>.failure(
        statusCode: statusCode,
        message: message,
        endpoint: responseEndpoint,
        errors: errors,
      );
    }

    try {
      return ApiResponseFormat<T>.success(
        data: fromData(json['data']),
        statusCode: statusCode,
        message: message,
        endpoint: responseEndpoint,
      );
    } catch (e) {
      return ApiResponseFormat<T>.failure(
        statusCode: statusCode,
        message: 'Failed to parse response',
        endpoint: responseEndpoint,
        errors: e,
      );
    }
  }

  static int _readStatusCode(Map<String, dynamic> json, int httpStatusCode) {
    final dynamic rawStatusCode = json['statusCode'];
    final int? parsedStatusCode = rawStatusCode is int
        ? rawStatusCode
        : int.tryParse(rawStatusCode?.toString() ?? '');

    if (parsedStatusCode == null || parsedStatusCode == 0) {
      return httpStatusCode;
    }

    return parsedStatusCode;
  }
}
