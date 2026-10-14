import 'dart:async';

import 'package:bloc/bloc.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/common/authentication/auth/domain/entity/auth_otp_session.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_event.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_state.dart';
import 'package:swift_app/features/common/authentication/login/domain/entity/login_request.dart';
import 'package:swift_app/features/common/authentication/login/domain/usecase/login_usecase.dart';
import 'package:swift_app/features/common/authentication/otp/domain/entity/verify_otp_request.dart';
import 'package:swift_app/features/common/authentication/otp/domain/usecase/send_otp_usecase.dart';
import 'package:swift_app/features/common/authentication/otp/domain/usecase/verify_otp_usecase.dart';
import 'package:swift_app/features/common/authentication/signup/domain/usecase/signup_usecase.dart';

class AuthBloc extends Bloc<AuthEvent, AuthState> {
  final SendOtpUsecase _sendOtpUsecase;
  final VerifyOtpUsecase _verifyOtpUsecase;
  final SignupUsecase _signupUsecase;
  final LoginUsecase _loginUsecase;
  final AuthLocalDatasource _authLocalDatasource;
  final Future<void> Function()? onAuthenticated;
  final Future<void> Function()? beforeLogout;

  AuthBloc({
    required SendOtpUsecase sendOtpUsecase,
    required VerifyOtpUsecase verifyOtpUsecase,
    required SignupUsecase signupUsecase,
    required LoginUsecase loginUsecase,
    required AuthLocalDatasource authLocalDatasource,
    this.onAuthenticated,
    this.beforeLogout,
  }) : _sendOtpUsecase = sendOtpUsecase,
       _verifyOtpUsecase = verifyOtpUsecase,
       _signupUsecase = signupUsecase,
       _loginUsecase = loginUsecase,
       _authLocalDatasource = authLocalDatasource,
       super(const AuthInitial()) {
    on<AuthStarted>(_onStarted);
    on<AuthSignupOtpRequested>(_onSignupOtpRequested);
    on<AuthLoginOtpRequested>(_onLoginOtpRequested);
    on<AuthOtpResendRequested>(_onOtpResendRequested);
    on<AuthOtpSubmitted>(_onOtpSubmitted);
    on<AuthLogoutRequested>(_onLogoutRequested);
  }

  Future<void> _onStarted(AuthStarted event, Emitter<AuthState> emit) async {
    emit(const AuthLoading(AuthAction.checking));
    final savedUser = _authLocalDatasource.getSavedUser();

    if (_authLocalDatasource.isAuthenticated) {
      emit(AuthAuthenticated(user: savedUser));
      unawaited(onAuthenticated?.call());
      return;
    }

    emit(const AuthUnauthenticated());
  }

  Future<void> _onSignupOtpRequested(
    AuthSignupOtpRequested event,
    Emitter<AuthState> emit,
  ) async {
    await _sendOtp(
      email: event.request.email,
      session: AuthOtpSession.signup(event.request),
      emit: emit,
    );
  }

  Future<void> _onLoginOtpRequested(
    AuthLoginOtpRequested event,
    Emitter<AuthState> emit,
  ) async {
    await _sendOtp(
      email: event.email,
      session: AuthOtpSession.login(event.email),
      emit: emit,
    );
  }

  Future<void> _onOtpResendRequested(
    AuthOtpResendRequested event,
    Emitter<AuthState> emit,
  ) async {
    await _sendOtp(
      email: event.session.email,
      session: event.session,
      emit: emit,
    );
  }

  Future<void> _sendOtp({
    required String email,
    required AuthOtpSession session,
    required Emitter<AuthState> emit,
  }) async {
    emit(const AuthLoading(AuthAction.sendOtp));
    final response = await _sendOtpUsecase(email);

    if (!_isOk(response)) {
      emit(AuthFailure(message: response.message, action: AuthAction.sendOtp));
      return;
    }

    emit(
      AuthOtpSent(
        session: session,
        message: response.message.isEmpty
            ? 'OTP sent successfully'
            : response.message,
      ),
    );
  }

  Future<void> _onOtpSubmitted(
    AuthOtpSubmitted event,
    Emitter<AuthState> emit,
  ) async {
    final otp = event.otp.trim();
    if (otp.isEmpty) {
      emit(
        const AuthFailure(
          message: 'Please enter OTP',
          action: AuthAction.verifyOtp,
        ),
      );
      return;
    }

    final otpValue = int.tryParse(otp);
    if (otpValue == null) {
      emit(
        const AuthFailure(
          message: 'Please enter a valid OTP',
          action: AuthAction.verifyOtp,
        ),
      );
      return;
    }

    emit(const AuthLoading(AuthAction.verifyOtp));
    final verifyResponse = await _verifyOtpUsecase(
      VerifyOtpRequest(email: event.session.email, otp: otp),
    );

    if (!_isOk(verifyResponse) || verifyResponse.data != true) {
      emit(
        AuthFailure(
          message: verifyResponse.message,
          action: AuthAction.verifyOtp,
        ),
      );
      return;
    }

    if (event.session.isSignup) {
      await _completeSignup(event.session, emit);
      return;
    }

    await _completeLogin(event.session.email, otpValue, emit);
  }

  Future<void> _completeSignup(
    AuthOtpSession session,
    Emitter<AuthState> emit,
  ) async {
    final request = session.signupRequest;

    if (request == null) {
      emit(
        const AuthFailure(
          message: 'Signup details are missing',
          action: AuthAction.signup,
        ),
      );
      return;
    }

    emit(const AuthLoading(AuthAction.signup));
    final signupResponse = await _signupUsecase(request);

    if (!_isOk(signupResponse)) {
      emit(
        AuthFailure(message: signupResponse.message, action: AuthAction.signup),
      );
      return;
    }

    unawaited(onAuthenticated?.call());
    emit(
      AuthAuthenticated(
        user: signupResponse.data,
        message: signupResponse.message.isEmpty
            ? 'Signup successful'
            : signupResponse.message,
      ),
    );
  }

  Future<void> _completeLogin(
    String email,
    int otp,
    Emitter<AuthState> emit,
  ) async {
    emit(const AuthLoading(AuthAction.login));
    final loginResponse = await _loginUsecase(
      LoginRequest(email: email, otp: otp),
    );

    if (!_isOk(loginResponse)) {
      final message = loginResponse.message;
      emit(
        AuthFailure(
          message:
              loginResponse.statusCode >= 500 &&
                  !message.toLowerCase().contains('new code')
              ? '$message Request a new code to continue.'
              : message,
          action: AuthAction.login,
        ),
      );
      return;
    }

    unawaited(onAuthenticated?.call());
    emit(
      AuthAuthenticated(
        user: loginResponse.data,
        message: loginResponse.message.isEmpty
            ? 'Login successful'
            : loginResponse.message,
      ),
    );
  }

  Future<void> _onLogoutRequested(
    AuthLogoutRequested event,
    Emitter<AuthState> emit,
  ) async {
    emit(const AuthLoading(AuthAction.logout));
    await beforeLogout?.call();
    await _authLocalDatasource.clear();
    emit(const AuthUnauthenticated());
  }

  bool _isOk<T>(ApiResponseFormat<T> response) {
    return response.success && response.statusCode == 200;
  }
}
