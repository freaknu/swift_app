import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:swift_app/core/performance/ui_frame_profiler.dart';
import 'package:swift_app/features/common/shared/widgets/app_loading_page.dart';
import 'package:swift_app/themes/custom_theme_data.dart';

/// Validates actual engine callbacks without preferences, authentication,
/// backend, GPS, native map or checkout activity.
void main() {
  final binding = IntegrationTestWidgetsFlutterBinding.ensureInitialized();
  binding.framePolicy = LiveTestWidgetsFlutterBindingFramePolicy.fullyLive;
  testWidgets('opt-in recorder emits real bounded profile timing windows', (
    tester,
  ) async {
    expect(kProfileMode, isTrue);
    expect(UiFrameProfiler.enabled, isTrue);
    final messages = <Map<String, dynamic>>[];
    final originalPrint = debugPrint;
    debugPrint = (String? message, {int? wrapWidth}) {
      if (message?.startsWith('SWIFT_UI_PROFILE ') == true) {
        messages.add(
          jsonDecode(message!.substring('SWIFT_UI_PROFILE '.length))
              as Map<String, dynamic>,
        );
      }
      originalPrint(message, wrapWidth: wrapWidth);
    };
    addTearDown(() {
      UiFrameProfiler.instance.stop();
      debugPrint = originalPrint;
    });
    UiFrameProfiler.instance.start();
    UiFrameProfiler.instance.start();
    await tester.pumpWidget(
      MaterialApp(
        theme: customDarkThemeData(),
        navigatorObservers: [UiFrameProfiler.instance.navigatorObserver],
        home: const AppLoadingPage(),
      ),
    );
    await tester.pump(const Duration(seconds: 12));
    final windows = messages.where((entry) => entry['reason'] == 'window');
    expect(
      windows,
      hasLength(1),
      reason: 'Duplicate start must not register twice',
    );
    final window = windows.single;
    expect(window['mode'], 'profile');
    expect(window['frameCount'], greaterThan(120));
    expect(window['frameCount'], lessThanOrEqualTo(2400));
    expect(window['discardedSamples'], 0);
    expect(window['rawBuildMicroseconds'], isNull);
    expect((window['build'] as Map)['p95Ms'], isA<num>());
    binding.reportData = {'profiler_smoke': window};
    UiFrameProfiler.instance.stop();
    await tester.pumpWidget(const SizedBox.shrink());
  });
}
