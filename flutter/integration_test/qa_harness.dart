import 'package:flutter_test/flutter_test.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_response.dart';
import 'package:swift_app/main.dart' as app;

const _qaIntegration = bool.fromEnvironment('QA_INTEGRATION');

Future<void> launchQaApp(WidgetTester tester, String role) async {
  if (!_qaIntegration) {
    fail('QA_INTEGRATION=true is required for the isolated live tests.');
  }
  final package = await PackageInfo.fromPlatform();
  if (!package.packageName.endsWith('.qa')) {
    fail('The isolated live tests may run only in the .qa app package.');
  }
  final normalizedRole = role.toUpperCase();
  final userId = normalizedRole == 'DRIVER'
      ? const int.fromEnvironment('QA_DRIVER_AUTH_ID')
      : const int.fromEnvironment('QA_RIDER_AUTH_ID');
  final token = normalizedRole == 'DRIVER'
      ? const String.fromEnvironment('QA_DRIVER_TOKEN')
      : const String.fromEnvironment('QA_RIDER_TOKEN');
  if (userId <= 0 || token.isEmpty) {
    fail('QA fixture auth ID and token are required for $normalizedRole.');
  }

  // This runtime guard protects preferences from being cleared in the normal
  // app. Flutter's host-side test cleanup also uninstalls the package it saw
  // before building, so launch live tests through run-android-qa.sh.
  final preferences = await SharedPreferences.getInstance();
  await preferences.clear();
  await AuthLocalDatasource(preferences: preferences).saveAuthUser(
    SignupResponse(
      userId: userId,
      name: 'Swift QA $normalizedRole',
      email: 'qa@example.invalid',
      accountLocked: false,
      deviceLocked: false,
      pinCode: 0,
      accessToken: token.replaceFirst(RegExp(r'^Bearer\s+'), ''),
      refreshToken: 'qa-refresh-unused',
      role: normalizedRole,
      active: true,
    ),
  );
  await app.main();
  await tester.pump();
}

Future<void> waitForWidget(
  WidgetTester tester,
  Finder finder, {
  Duration timeout = const Duration(seconds: 30),
}) async {
  final attempts = timeout.inSeconds;
  for (var i = 0; i < attempts; i++) {
    await tester.pump(const Duration(seconds: 1));
    if (finder.evaluate().isNotEmpty) return;
  }
  fail('Timed out waiting for $finder.');
}

Future<void> waitForCondition(
  WidgetTester tester,
  bool Function() condition, {
  Duration timeout = const Duration(seconds: 30),
  String description = 'condition',
}) async {
  for (var i = 0; i < timeout.inSeconds; i++) {
    await tester.pump(const Duration(seconds: 1));
    if (condition()) return;
  }
  fail('Timed out waiting for $description.');
}
