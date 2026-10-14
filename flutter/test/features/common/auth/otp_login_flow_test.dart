import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart' hide Response;
import 'package:shared_preferences/shared_preferences.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_home_ready_datasource.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_bloc.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_state.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/widgets/auth_home_ready_reporter.dart';
import 'package:swift_app/features/common/authentication/login/data/datasource/login_datasource.dart';
import 'package:swift_app/features/common/authentication/login/data/repository/login_repository_impl.dart';
import 'package:swift_app/features/common/authentication/login/domain/usecase/login_usecase.dart';
import 'package:swift_app/features/common/authentication/otp/data/datasource/otp_datasource.dart';
import 'package:swift_app/features/common/authentication/otp/data/repository/otp_repository_impl.dart';
import 'package:swift_app/features/common/authentication/otp/domain/usecase/send_otp_usecase.dart';
import 'package:swift_app/features/common/authentication/otp/domain/usecase/verify_otp_usecase.dart';
import 'package:swift_app/features/common/authentication/otp/presentation/pages/otp_page.dart';
import 'package:swift_app/features/common/authentication/signup/data/datasource/signup_datasource.dart';
import 'package:swift_app/features/common/authentication/signup/data/repository/signup_repository_impl.dart';
import 'package:swift_app/features/common/authentication/signup/domain/usecase/signup_usecase.dart';
import 'package:swift_app/themes/app_theme.dart';

void main() {
  for (final role in ['RIDER', 'DRIVER']) {
    testWidgets(
      '$role verification persists login, leaves OTP and acknowledges painted home',
      (tester) async {
        final fixture = await _pump(tester, role: role);
        await _verify(tester);

        expect(find.byType(OtpPage), findsNothing);
        expect(find.text('$role home'), findsOneWidget);
        expect(fixture.auth.state, isA<AuthAuthenticated>());
        expect(fixture.local.accessToken, 'fixture-access-token');
        expect(fixture.local.role, role);
        expect(fixture.local.otpCompletionToken, 'fixture-completion-token');
        expect(
          fixture.local.getSavedUser()?.otpCompletionToken,
          'fixture-completion-token',
        );
        expect(fixture.requests.map((request) => request.path), [
          'api/v1/auth/verifyOtp/rider%40example.com/123456',
          'api/v1/auth/login',
        ]);
        expect(fixture.requests[1].data, {
          'email': 'rider@example.com',
          'otp': 123456,
        });

        // Navigation alone is insufficient: the startup data/map must settle.
        fixture.homeReady.value = true;
        await tester.pumpAndSettle();
        await tester.pump(const Duration(milliseconds: 10));
        expect(
          WidgetsBinding.instance.lifecycleState,
          AppLifecycleState.resumed,
        );
        expect(
          ModalRoute.of(
            tester.element(find.byType(AuthHomeReadyReporter)),
          )?.isCurrent,
          isTrue,
        );
        expect(fixture.requests.last.path, 'api/v1/auth/home-ready');
        expect(fixture.requests.last.data, {
          'otpCompletionToken': 'fixture-completion-token',
        });
        expect(
          fixture.requests.last.headers['Authorization'],
          'Bearer fixture-access-token',
        );
        expect(fixture.local.otpCompletionToken, isNull);
        expect(tester.takeException(), isNull);
      },
    );
  }

  for (final data in [false, null, 'OTP verified successfully']) {
    testWidgets(
      'misleading success message with data:$data stays a useful verification error',
      (tester) async {
        final fixture = await _pump(tester, verificationData: data);
        await _verify(tester);

        expect(find.byType(OtpPage), findsOneWidget);
        expect(find.text('OTP Verified Successfully'), findsNothing);
        expect(
          find.text(
            'Invalid verification code. Please check it and try again.',
          ),
          findsOneWidget,
        );
        expect(fixture.auth.state, isA<AuthFailure>());
        expect(fixture.local.isAuthenticated, isFalse);
        expect(fixture.requests, hasLength(1));
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets(
    'session storage failure shows recovery and cannot acknowledge home',
    (tester) async {
      final fixture = await _pump(tester, failPersistence: true);
      await _verify(tester);
      expect(find.byType(OtpPage), findsOneWidget);
      expect(
        find.text(
          'Could not save your sign-in. Request a new code to continue.',
        ),
        findsOneWidget,
      );
      expect(fixture.auth.state, isA<AuthFailure>());
      expect(fixture.local.isAuthenticated, isFalse);
      expect(fixture.requests.map((request) => request.path), [
        'api/v1/auth/verifyOtp/rider%40example.com/123456',
        'api/v1/auth/login',
      ]);
      expect(find.text('Verify'), findsOneWidget);
      final resend = tester.widget<TextButton>(
        find.widgetWithText(TextButton, 'Resend code'),
      );
      expect(resend.onPressed, isNotNull);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'lost login response permits a fresh code without remaining busy',
    (tester) async {
      final fixture = await _pump(tester, failLoginResponse: true);
      await _verify(tester);
      expect(find.byType(OtpPage), findsOneWidget);
      expect(
        find.textContaining('Request a new code to continue.'),
        findsOneWidget,
      );
      expect(fixture.auth.state, isA<AuthFailure>());
      expect(fixture.local.isAuthenticated, isFalse);
      final resend = tester.widget<TextButton>(
        find.widgetWithText(TextButton, 'Resend code'),
      );
      expect(resend.onPressed, isNotNull);
      expect(fixture.requests, hasLength(2));
      expect(tester.takeException(), isNull);
    },
  );
}

Future<void> _verify(WidgetTester tester) async {
  await tester.enterText(find.byType(TextFormField), '123456');
  await tester.ensureVisible(find.text('Verify'));
  await tester.tap(find.text('Verify'));
  await tester.pumpAndSettle();
}

Future<_Fixture> _pump(
  WidgetTester tester, {
  String role = 'RIDER',
  Object? verificationData = true,
  bool failPersistence = false,
  bool failLoginResponse = false,
}) async {
  Get.testMode = true;
  tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
  SharedPreferences.setMockInitialValues({});
  final preferences = await SharedPreferences.getInstance();
  final local = failPersistence
      ? _FailingAuthLocalDatasource(preferences: preferences)
      : AuthLocalDatasource(preferences: preferences);
  final client = DioClient(
    accessTokenReader: () async => local.accessToken,
    authUserIdReader: () async => local.userId,
  );
  final requests = <RequestOptions>[];
  client.dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (request, handler) {
        requests.add(request);
        if (request.path == 'api/v1/auth/login' && failLoginResponse) {
          handler.reject(
            DioException(
              requestOptions: request,
              type: DioExceptionType.receiveTimeout,
            ),
          );
          return;
        }
        final data = switch (request.path) {
          'api/v1/auth/login' => {
            'userId': 42,
            'name': 'Fixture User',
            'email': 'rider@example.com',
            'role': role,
            'isActive': true,
            'accessToken': 'fixture-access-token',
            'refreshToken': 'fixture-refresh-token',
            'otpCompletionToken': 'fixture-completion-token',
          },
          'api/v1/auth/home-ready' => true,
          _ => verificationData,
        };
        handler.resolve(
          Response(
            requestOptions: request,
            statusCode: 200,
            data: {
              'success': true,
              'statusCode': 200,
              'data': data,
              'message': 'OTP Verified Successfully',
            },
          ),
        );
      },
    ),
  );
  final endpoints = RiderApiEndpoints();
  final otpRepository = OtpRepositoryImpl(
    otpDatasource: OtpDatasource(dioClient: client, endpoints: endpoints),
  );
  final auth = AuthBloc(
    sendOtpUsecase: SendOtpUsecase(otpRepository: otpRepository),
    verifyOtpUsecase: VerifyOtpUsecase(otpRepository: otpRepository),
    loginUsecase: LoginUsecase(
      loginRepository: LoginRepositoryImpl(
        loginDatasource: LoginDatasource(
          dioClient: client,
          endpoints: endpoints,
        ),
        authLocalDatasource: local,
      ),
    ),
    signupUsecase: SignupUsecase(
      signupRepository: SignupRepositoryImpl(
        signupDatasource: SignupDatasource(dioClient: client),
        authLocalDatasource: local,
      ),
    ),
    authLocalDatasource: local,
  );
  final homeReady = ValueNotifier(false);
  final home = AuthHomeReadyDatasource(
    dioClient: client,
    endpoints: endpoints,
    authLocalDatasource: local,
  );
  addTearDown(() async {
    await tester.pumpWidget(const SizedBox.shrink());
    await auth.close();
    homeReady.dispose();
    Get.reset();
  });
  await tester.pumpWidget(
    BlocProvider<AuthBloc>.value(
      value: auth,
      child: GetMaterialApp(
        theme: AppTheme.light(),
        home: const Scaffold(),
        getPages: [
          GetPage(name: '/otp', page: () => const OtpPage()),
          GetPage(
            name: '/home',
            page: () => ValueListenableBuilder<bool>(
              valueListenable: homeReady,
              builder: (context, ready, _) => AuthHomeReadyReporter(
                ready: ready,
                onReady: home.acknowledge,
                child: Scaffold(body: Text('${local.role} home')),
              ),
            ),
          ),
        ],
      ),
    ),
  );
  unawaited(
    Get.toNamed<void>(
      '/otp',
      arguments: const OtpPageProps(email: 'rider@example.com', pageType: 2),
    ),
  );
  await tester.pumpAndSettle();
  return _Fixture(auth, local, requests, homeReady);
}

class _Fixture {
  _Fixture(this.auth, this.local, this.requests, this.homeReady);
  final AuthBloc auth;
  final AuthLocalDatasource local;
  final List<RequestOptions> requests;
  final ValueNotifier<bool> homeReady;
}

class _FailingAuthLocalDatasource extends AuthLocalDatasource {
  _FailingAuthLocalDatasource({required super.preferences});

  @override
  Future<void> saveAuthUser(user) async {
    throw StateError('Fixture storage failure');
  }
}
