import 'package:swift_app/constants/enums/role_enum.dart';
import 'package:swift_app/features/common/authentication/auth/domain/entity/auth_otp_session.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_bloc.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_event.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_state.dart';
import 'package:swift_app/features/common/authentication/login/presentation/pages/login_page.dart';
import 'package:swift_app/features/common/authentication/otp/presentation/pages/otp_page.dart';
import 'package:swift_app/features/common/authentication/signup/presentation/pages/signup_page.dart';
import 'package:swift_app/features/common/splash/welcome/presentation/pages/welcome_page.dart';
import 'package:swift_app/themes/custom_theme_data.dart';
import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';

void main() {
  testWidgets(
    'OTP stays reachable with keyboard, large text, and 44 point actions',
    (tester) async {
      await _pumpAuth(
        tester,
        const OtpPage(),
        arguments: const OtpPageProps(email: 'rider@example.com', pageType: 2),
        size: const Size(320, 568),
        textScale: 2,
        keyboardHeight: 240,
      );
      final change = find.widgetWithText(CupertinoButton, 'Change');
      expect(tester.getSize(change).height, greaterThanOrEqualTo(44));
      await tester.ensureVisible(find.text('Verify'));
      await tester.pumpAndSettle();
      expect(
        tester.getRect(find.text('Verify')).bottom,
        lessThanOrEqualTo(328),
      );
      await tester.pump(const Duration(seconds: 31));
      final resend = find.widgetWithText(TextButton, 'Resend code');
      await tester.ensureVisible(resend);
      expect(tester.getSize(resend).height, greaterThanOrEqualTo(44));
      expect(tester.takeException(), isNull);
    },
  );

  for (final entry in <String, Widget>{
    'Create account': const LoginPage(),
    'Sign in': const SignupPage(),
  }.entries) {
    testWidgets('${entry.key} has a comfortable authentication touch target', (
      tester,
    ) async {
      await _pumpAuth(tester, entry.value);
      final link = find.widgetWithText(CupertinoButton, entry.key);
      expect(tester.getSize(link).height, greaterThanOrEqualTo(44));
      expect(tester.takeException(), isNull);
    });
  }

  testWidgets('sign-in validates email and prevents duplicate requests', (
    tester,
  ) async {
    final auth = await _pumpAuth(tester, const LoginPage());
    final submit = find.text('Continue');
    await tester.enterText(find.byType(TextFormField), 'invalid');
    await tester.ensureVisible(submit);
    await tester.tap(submit);
    await tester.pump();
    expect(auth.events, isEmpty);
    expect(
      find.text('Enter a valid email, like name@example.com'),
      findsOneWidget,
    );

    await tester.enterText(find.byType(TextFormField), ' rider@example.com ');
    await tester.ensureVisible(submit);
    await tester.tap(submit);
    await tester.pump();
    expect(auth.events.single, isA<AuthLoginOtpRequested>());
    expect(
      (auth.events.single as AuthLoginOtpRequested).email,
      'rider@example.com',
    );
    await tester.tap(find.text('Sending…'));
    expect(auth.events, hasLength(1));

    auth.update(
      const AuthFailure(
        message: 'Please try again',
        action: AuthAction.sendOtp,
      ),
    );
    await tester.pump();
    expect(find.text('Please try again'), findsOneWidget);
    await tester.ensureVisible(submit);
    await tester.tap(submit);
    expect(auth.events, hasLength(2));
    expect(tester.takeException(), isNull);
  });

  testWidgets('sign-up validates details and submits the rider profile', (
    tester,
  ) async {
    final auth = await _pumpAuth(tester, const SignupPage());
    final submit = find.text('Continue');
    await tester.ensureVisible(submit);
    await tester.tap(submit);
    await tester.pump();
    expect(auth.events, isEmpty);
    expect(find.text('Enter your name'), findsOneWidget);
    expect(find.text('Enter your email address'), findsOneWidget);
    expect(find.text('Enter a 10-digit mobile number'), findsOneWidget);

    await tester.enterText(find.byType(TextFormField).at(0), 'Asha Rao');
    await tester.enterText(
      find.byType(TextFormField).at(1),
      'asha@example.com',
    );
    await tester.enterText(find.byType(TextFormField).at(2), '9000000000');
    await tester.ensureVisible(submit);
    await tester.tap(submit);
    await tester.pump();
    final request = (auth.events.single as AuthSignupOtpRequested).request;
    expect(request.name, 'Asha Rao');
    expect(request.email, 'asha@example.com');
    expect(request.phoneNumber, '9000000000');
    expect(request.role, RoleEnum.RIDER);
    expect(tester.takeException(), isNull);
  });

  testWidgets('sign-up sends the selected driver role', (tester) async {
    final auth = await _pumpAuth(
      tester,
      const SignupPage(initialRole: RoleEnum.DRIVER),
    );
    final submit = find.text('Continue');

    expect(find.text('Signing up as Driver'), findsOneWidget);
    await tester.enterText(find.byType(TextFormField).at(0), 'Dev Driver');
    await tester.enterText(
      find.byType(TextFormField).at(1),
      'driver@example.com',
    );
    await tester.enterText(find.byType(TextFormField).at(2), '9000000001');
    await tester.ensureVisible(submit);
    await tester.tap(submit);
    await tester.pump();

    final request = (auth.events.single as AuthSignupOtpRequested).request;
    expect(request.role, RoleEnum.DRIVER);
    expect(tester.takeException(), isNull);
  });

  testWidgets('OTP requires six digits and locks resend during verification', (
    tester,
  ) async {
    final auth = await _pumpAuth(
      tester,
      const OtpPage(),
      arguments: const OtpPageProps(email: 'rider@example.com', pageType: 2),
    );
    final submit = find.text('Verify');
    expect(find.text('Resend in 30s'), findsOneWidget);
    await tester.enterText(find.byType(TextFormField), '123');
    await tester.ensureVisible(submit);
    await tester.tap(submit);
    await tester.pump();
    expect(auth.events, isEmpty);
    expect(find.text('Enter the complete 6-digit code'), findsOneWidget);

    // A single field supports a complete pasted or autofilled code.
    await tester.enterText(find.byType(TextFormField), '012345');
    await tester.ensureVisible(submit);
    await tester.tap(submit);
    await tester.pump();
    final event = auth.events.single as AuthOtpSubmitted;
    expect(event.otp, '012345');
    expect(event.session.email, 'rider@example.com');
    auth.update(const AuthLoading(AuthAction.verifyOtp));
    await tester.pump(const Duration(seconds: 31));
    final resend = tester.widget<TextButton>(
      find.widgetWithText(TextButton, 'Resend code'),
    );
    expect(resend.onPressed, isNull);
    expect(tester.takeException(), isNull);
  });

  testWidgets('OTP resend cooldown restarts only after a successful send', (
    tester,
  ) async {
    final auth = await _pumpAuth(
      tester,
      const OtpPage(),
      arguments: const OtpPageProps(email: 'rider@example.com', pageType: 2),
    );
    await tester.pump(const Duration(seconds: 31));
    await tester.ensureVisible(find.text('Resend code'));
    await tester.tap(find.text('Resend code'));
    expect(auth.events.single, isA<AuthOtpResendRequested>());
    auth.update(
      AuthOtpSent(
        session: AuthOtpSession.login('rider@example.com'),
        message: 'Code sent',
      ),
    );
    await tester.pump();
    expect(find.text('Resend in 30s'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  for (final entry in <String, Widget>{
    'sign-in': const LoginPage(),
    'sign-up': const SignupPage(),
  }.entries) {
    testWidgets(
      '${entry.key} keeps its action reachable with large text and keyboard',
      (tester) async {
        await _pumpAuth(
          tester,
          entry.value,
          size: const Size(320, 568),
          textScale: 1.5,
          keyboardHeight: 240,
        );
        final action = find.text('Continue');
        await tester.ensureVisible(action);
        await tester.pumpAndSettle();
        expect(tester.getRect(action).bottom, lessThanOrEqualTo(328));
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets('welcome stays usable on small screens with large text', (
    tester,
  ) async {
    await _pumpAuth(
      tester,
      const WelcomePage(),
      size: const Size(320, 568),
      textScale: 2,
    );
    expect(tester.takeException(), isNull);
    expect(find.text('RIDER'), findsOneWidget);
    expect(find.text('DRIVER'), findsOneWidget);
    await tester.ensureVisible(find.text('DRIVER'));
    await tester.tap(find.text('DRIVER'));
    await tester.pumpAndSettle();
    final getStarted = find.text('Get started');
    await tester.ensureVisible(getStarted);
    await tester.tap(getStarted);
    await tester.pumpAndSettle();
    expect(find.text('Signup destination'), findsOneWidget);
    expect(find.text('DRIVER'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}

Future<_AuthBloc> _pumpAuth(
  WidgetTester tester,
  Widget page, {
  Object? arguments,
  Size size = const Size(390, 844),
  double textScale = 1,
  double keyboardHeight = 0,
}) async {
  Get.testMode = true;
  tester.view.physicalSize = size;
  tester.view.devicePixelRatio = 1;
  tester.view.viewInsets = FakeViewPadding(bottom: keyboardHeight);
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
  addTearDown(tester.view.resetViewInsets);
  final auth = _AuthBloc();
  addTearDown(() async {
    await tester.pumpWidget(const SizedBox.shrink());
    await auth.close();
    Get.reset();
  });
  await tester.pumpWidget(
    BlocProvider<AuthBloc>.value(
      value: auth,
      child: GetMaterialApp(
        theme: customThemeData(),
        builder: (context, child) => MediaQuery(
          data: MediaQuery.of(
            context,
          ).copyWith(textScaler: TextScaler.linear(textScale)),
          child: child!,
        ),
        home: arguments == null ? page : const Scaffold(),
        getPages: [
          GetPage(name: '/otp', page: () => page),
          GetPage(
            name: '/signup',
            page: () => Scaffold(
              body: Column(
                children: [
                  const Text('Signup destination'),
                  Text(
                    Get.arguments is RoleEnum
                        ? (Get.arguments as RoleEnum).name
                        : '',
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    ),
  );
  if (arguments != null) {
    Get.toNamed('/otp', arguments: arguments);
  }
  await tester.pumpAndSettle();
  return auth;
}

class _AuthBloc extends Cubit<AuthState> implements AuthBloc {
  _AuthBloc() : super(const AuthUnauthenticated());
  final events = <AuthEvent>[];

  @override
  void add(AuthEvent event) => events.add(event);
  void update(AuthState state) => emit(state);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}
