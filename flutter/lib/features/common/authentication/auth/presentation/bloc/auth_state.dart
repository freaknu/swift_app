import 'package:swift_app/features/common/authentication/auth/domain/entity/auth_otp_session.dart';
import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_response.dart';

enum AuthAction { checking, sendOtp, verifyOtp, signup, login, logout }

abstract class AuthState {
  const AuthState();
}

class AuthInitial extends AuthState {
  const AuthInitial();
}

class AuthLoading extends AuthState {
  final AuthAction action;

  const AuthLoading(this.action);
}

class AuthUnauthenticated extends AuthState {
  const AuthUnauthenticated();
}

class AuthOtpSent extends AuthState {
  final AuthOtpSession session;
  final String message;

  const AuthOtpSent({required this.session, required this.message});
}

class AuthAuthenticated extends AuthState {
  final SignupResponse? user;
  final String message;

  const AuthAuthenticated({this.user, this.message = 'Authenticated'});
}

class AuthFailure extends AuthState {
  final String message;
  final AuthAction? action;

  const AuthFailure({required this.message, this.action});
}
