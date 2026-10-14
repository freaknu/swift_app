import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:swift_app/themes/theme_controller.dart';

const _preferenceKey = 'app_theme_mode';

Future<void> _completePendingWork() => Future<void>.delayed(Duration.zero);

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  for (final mode in ThemeMode.values) {
    test('restores the stored ${mode.name} appearance', () async {
      SharedPreferences.setMockInitialValues({_preferenceKey: mode.name});
      final preferences = await SharedPreferences.getInstance();

      final controller = ThemeController(preferences: preferences);

      expect(controller.themeMode, mode);
    });
  }

  test(
    'missing, unknown or incorrectly typed values use system mode',
    () async {
      for (final value in [null, 'unexpected', 7]) {
        SharedPreferences.setMockInitialValues({_preferenceKey: ?value});
        final preferences = await SharedPreferences.getInstance();

        expect(
          ThemeController(preferences: preferences).themeMode,
          ThemeMode.system,
        );
      }
    },
  );

  test('manual modes and returning to system persist across restart', () async {
    SharedPreferences.setMockInitialValues({});
    final preferences = await SharedPreferences.getInstance();
    final controller = ThemeController(preferences: preferences);

    controller.setDarkMode();
    expect(controller.themeMode, ThemeMode.dark);
    await _completePendingWork();
    await preferences.reload();
    expect(preferences.getString(_preferenceKey), 'dark');
    expect(ThemeController(preferences: preferences).themeMode, ThemeMode.dark);

    controller.setLightMode();
    expect(controller.themeMode, ThemeMode.light);
    await _completePendingWork();
    await preferences.reload();
    expect(preferences.getString(_preferenceKey), 'light');
    expect(
      ThemeController(preferences: preferences).themeMode,
      ThemeMode.light,
    );

    controller.useSystemTheme();
    expect(controller.themeMode, ThemeMode.system);
    await _completePendingWork();
    await preferences.reload();
    expect(preferences.getString(_preferenceKey), 'system');
    expect(
      ThemeController(preferences: preferences).themeMode,
      ThemeMode.system,
    );
  });

  test(
    'rapid switches serialize storage writes and preserve last choice',
    () async {
      final preferences = _DelayedPreferences();
      final controller = ThemeController(preferences: preferences);

      controller.setDarkMode();
      controller.setLightMode();
      controller.useSystemTheme();
      expect(controller.themeMode, ThemeMode.system);
      await _completePendingWork();
      expect(preferences.writes.map((write) => write.mode), ['dark']);

      preferences.writes.first.completion.complete(true);
      await _completePendingWork();
      expect(preferences.writes.map((write) => write.mode), ['dark', 'light']);

      preferences.writes[1].completion.complete(true);
      await _completePendingWork();
      expect(preferences.writes.map((write) => write.mode), [
        'dark',
        'light',
        'system',
      ]);
      preferences.writes.last.completion.complete(true);
      await _completePendingWork();
      expect(preferences.savedMode, 'system');
    },
  );

  test(
    'failed persistence does not block later switches or change UI mode',
    () async {
      final preferences = _DelayedPreferences();
      final controller = ThemeController(preferences: preferences);

      controller.setDarkMode();
      await _completePendingWork();
      preferences.writes.first.completion.completeError(
        StateError('Device storage unavailable'),
      );
      await _completePendingWork();
      expect(controller.themeMode, ThemeMode.dark);

      controller.setLightMode();
      expect(controller.themeMode, ThemeMode.light);
      await _completePendingWork();
      expect(preferences.writes.last.mode, 'light');
      preferences.writes.last.completion.complete(true);
      await _completePendingWork();
      expect(preferences.savedMode, 'light');
    },
  );

  testWidgets('the displayed appearance changes before storage completes', (
    tester,
  ) async {
    final preferences = _DelayedPreferences();
    final controller = ThemeController(preferences: preferences);
    await tester.pumpWidget(
      Obx(
        () => MaterialApp(
          theme: ThemeData.light(),
          darkTheme: ThemeData.dark(),
          themeMode: controller.themeMode,
          themeAnimationDuration: Duration.zero,
          home: Builder(
            builder: (context) => Text(Theme.of(context).brightness.name),
          ),
        ),
      ),
    );

    controller.setDarkMode();
    await tester.pump();
    expect(find.text('dark'), findsOneWidget);
    await tester.runAsync(_completePendingWork);
    expect(preferences.writes.single.completion.isCompleted, isFalse);

    preferences.writes.single.completion.complete(true);
    await tester.pump();
  });
}

class _DelayedPreferences implements SharedPreferences {
  final writes = <_PreferenceWrite>[];
  String? savedMode;

  @override
  Object? get(String key) => key == _preferenceKey ? savedMode : null;

  @override
  Future<bool> setString(String key, String value) async {
    final write = _PreferenceWrite(value);
    writes.add(write);
    final succeeded = await write.completion.future;
    if (succeeded) savedMode = value;
    return succeeded;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _PreferenceWrite {
  _PreferenceWrite(this.mode);

  final String mode;
  final completion = Completer<bool>();
}
