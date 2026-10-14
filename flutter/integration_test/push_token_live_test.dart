import 'package:dio/dio.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:swift_app/core/notifications/push_device_registration.dart';
import 'package:swift_app/core/notifications/push_notification_service.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_response.dart';

class _CapturingRegistration extends PushDeviceRegistration {
  _CapturingRegistration(super.dio, super.preferences);

  int calls = 0;
  String? registeredRole;
  bool? registeredEnabled;

  @override
  Future<void> register({
    required String token,
    required String role,
    required bool enabled,
  }) async {
    expect(token, isNotEmpty);
    calls++;
    registeredRole = role;
    registeredEnabled = enabled;
  }

  @override
  Future<void> unregister() async {}
}

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('FCM enabled QA startup obtains and registers a device token', (
    tester,
  ) async {
    final package = await PackageInfo.fromPlatform();
    expect(package.packageName, endsWith('.qa'));
    final preferences = await SharedPreferences.getInstance();
    await preferences.clear();
    final auth = AuthLocalDatasource(preferences: preferences);
    await auth.saveAuthUser(
      SignupResponse(
        userId: 1,
        name: 'QA push probe',
        email: 'qa@example.invalid',
        accountLocked: false,
        deviceLocked: false,
        pinCode: 0,
        accessToken: 'qa-probe-no-network',
        refreshToken: 'qa-probe-no-network',
        role: 'RIDER',
        active: true,
      ),
    );
    final registration = _CapturingRegistration(Dio(), preferences);
    final push = PushNotificationService(
      auth: auth,
      registration: registration,
    );
    expect(push.supported, isTrue);
    try {
      try {
        await push.initialize();
      } on PlatformException catch (error) {
        fail('Push initialization failed (${error.code}).');
      }
      await push.startSession();
      final settings = await FirebaseMessaging.instance
          .getNotificationSettings();
      expect(
        settings.authorizationStatus,
        anyOf(AuthorizationStatus.authorized, AuthorizationStatus.provisional),
      );
      try {
        final token = await FirebaseMessaging.instance.getToken().timeout(
          const Duration(seconds: 30),
        );
        expect(token, isNotEmpty);
      } on FirebaseException catch (error) {
        fail('FCM token acquisition failed (${error.code}).');
      }
      for (var i = 0; i < 15 && registration.calls == 0; i++) {
        await tester.pump(const Duration(seconds: 1));
        await push.sync();
      }
      expect(registration.calls, greaterThan(0));
      expect(registration.registeredRole, 'RIDER');
      expect(registration.registeredEnabled, isTrue);
    } finally {
      await push.endSession();
      await preferences.clear();
    }
  });
}
