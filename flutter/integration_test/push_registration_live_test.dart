import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/core/notifications/push_notification_service.dart';

import 'qa_harness.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets(
    'authenticated QA app registers FCM and displays a live test push',
    (tester) async {
      await launchQaApp(tester, 'RIDER');
      final push = sl<PushNotificationService>();
      await waitForCondition(
        tester,
        () => push.status.state == PushDeliveryState.ready,
        timeout: const Duration(seconds: 60),
        description:
            'ready push registration: ${push.status.code ?? 'pending'}',
      );

      final preferences = await SharedPreferences.getInstance();
      final installationId = preferences.getString('push.installationId');
      expect(installationId, isNotNull);
      final dio = sl<DioClient>().dio;
      final path = 'api/v1/notifications/devices/$installationId';
      final statusResponse = await dio.get<dynamic>(path);
      expect(statusResponse.statusCode, 200);
      final status = (statusResponse.data as Map)['data'] as Map;
      expect(status['registered'], true);
      expect(status['enabled'], true);
      expect(status['role'], 'RIDER');

      // The isolated endpoint sends a real Firebase message to this owned QA
      // device. The foreground handler must display it through the local plugin.
      final sent = await dio.post<dynamic>('$path/test');
      expect(sent.statusCode, 200);
      expect((sent.data as Map)['data']['deliveryStatus'], 'SENT');
      final local = FlutterLocalNotificationsPlugin();
      var displayed = false;
      for (var attempt = 0; attempt < 30; attempt++) {
        await tester.pump(const Duration(seconds: 1));
        final active = await local.getActiveNotifications();
        if (active.any(
          (item) => item.title == 'Swift notifications are connected',
        )) {
          displayed = true;
          break;
        }
      }
      expect(
        displayed,
        isTrue,
        reason: 'FCM test push was not displayed locally',
      );
    },
  );
}
