class SignupResponse {
  final int userId;
  final String name;
  final String email;
  final bool accountLocked;
  final bool deviceLocked;
  final int pinCode;
  final String accessToken;
  final String refreshToken;
  final String role;
  final bool active;
  final String? otpCompletionToken;

  SignupResponse({
    required this.userId,
    required this.name,
    required this.email,
    required this.accountLocked,
    required this.deviceLocked,
    required this.pinCode,
    required this.accessToken,
    required this.refreshToken,
    required this.role,
    required this.active,
    this.otpCompletionToken,
  });
}
