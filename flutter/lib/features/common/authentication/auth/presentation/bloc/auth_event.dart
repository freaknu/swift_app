import 'package:swift_app/features/common/authentication/auth/domain/entity/auth_otp_session.dart';
import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_request.dart';

abstract class AuthEvent {
  const AuthEvent();
}

class AuthStarted extends AuthEvent {
  const AuthStarted();
}

class AuthSignupOtpRequested extends AuthEvent {
  final SignupRequest request;

  const AuthSignupOtpRequested(this.request);
}

class AuthLoginOtpRequested extends AuthEvent {
  final String email;

  const AuthLoginOtpRequested(this.email);
}

class AuthOtpResendRequested extends AuthEvent {
  final AuthOtpSession session;

  const AuthOtpResendRequested(this.session);
}

class AuthOtpSubmitted extends AuthEvent {
  final AuthOtpSession session;
  final String otp;

  const AuthOtpSubmitted({required this.session, required this.otp});
}

class AuthLogoutRequested extends AuthEvent {
  const AuthLogoutRequested();
}
