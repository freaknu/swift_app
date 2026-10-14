import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_request.dart';

enum AuthOtpFlow { signup, login }

class AuthOtpSession {
  final AuthOtpFlow flow;
  final String email;
  final SignupRequest? signupRequest;

  const AuthOtpSession({
    required this.flow,
    required this.email,
    this.signupRequest,
  });

  factory AuthOtpSession.signup(SignupRequest request) {
    return AuthOtpSession(
      flow: AuthOtpFlow.signup,
      email: request.email,
      signupRequest: request,
    );
  }

  factory AuthOtpSession.login(String email) {
    return AuthOtpSession(flow: AuthOtpFlow.login, email: email);
  }

  bool get isSignup => flow == AuthOtpFlow.signup;

  bool get isLogin => flow == AuthOtpFlow.login;
}
