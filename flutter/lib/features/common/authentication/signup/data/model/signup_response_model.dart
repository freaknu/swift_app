import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_response.dart';

class SignupResponseModel extends SignupResponse {
  SignupResponseModel({
    required super.userId,
    required super.name,
    required super.email,
    required super.accountLocked,
    required super.deviceLocked,
    required super.pinCode,
    required super.accessToken,
    required super.refreshToken,
    required super.role,
    required super.active,
    super.otpCompletionToken,
  });

  factory SignupResponseModel.fromJson(Map<String, dynamic> json) {
    return SignupResponseModel(
      userId: _asInt(json['userId']),
      name: json['name']?.toString() ?? '',
      email: json['email']?.toString() ?? '',
      accountLocked: _asBool(json['accountLocked']),
      deviceLocked: _asBool(json['deviceLocked']),
      pinCode: _asInt(json['pinCode']),
      accessToken: json['accessToken']?.toString() ?? '',
      refreshToken: json['refreshToken']?.toString() ?? '',
      role: json['role']?.toString() ?? '',
      active: _asBool(json['active'] ?? json['isActive']),
      otpCompletionToken: json['otpCompletionToken']?.toString(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'userId': userId,
      'name': name,
      'email': email,
      'accountLocked': accountLocked,
      'deviceLocked': deviceLocked,
      'pinCode': pinCode,
      'accessToken': accessToken,
      'refreshToken': refreshToken,
      'role': role,
      'active': active,
      if (otpCompletionToken != null) 'otpCompletionToken': otpCompletionToken,
    };
  }

  static int _asInt(dynamic value) {
    if (value is int) return value;
    return int.tryParse(value?.toString() ?? '') ?? 0;
  }

  static bool _asBool(dynamic value) {
    if (value is bool) return value;
    return value?.toString().toLowerCase() == 'true';
  }
}
