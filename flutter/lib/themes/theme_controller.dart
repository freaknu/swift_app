import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ThemeController extends GetxController {
  ThemeController({SharedPreferences? preferences})
    : _preferences = preferences,
      _themeMode = _restoreMode(preferences).obs;

  static const _preferenceKey = 'app_theme_mode';

  final SharedPreferences? _preferences;
  final Rx<ThemeMode> _themeMode;
  Future<void> _pendingSave = Future<void>.value();

  static ThemeMode _restoreMode(SharedPreferences? preferences) =>
      switch (preferences?.get(_preferenceKey)) {
        'dark' => ThemeMode.dark,
        'light' => ThemeMode.light,
        _ => ThemeMode.system,
      };

  ThemeMode get themeMode => _themeMode.value;

  bool get isDarkMode => switch (_themeMode.value) {
    ThemeMode.dark => true,
    ThemeMode.light => false,
    ThemeMode.system =>
      WidgetsBinding.instance.platformDispatcher.platformBrightness ==
          Brightness.dark,
  };

  void toggleTheme() => _setMode(isDarkMode ? ThemeMode.light : ThemeMode.dark);

  void setDarkMode() => _setMode(ThemeMode.dark);

  void setLightMode() => _setMode(ThemeMode.light);

  /// Keep system mode live, so changes in iOS Settings are reflected in-app.
  void useSystemTheme() => _setMode(ThemeMode.system);

  void _setMode(ThemeMode mode) {
    _themeMode.value = mode;
    final preferences = _preferences;
    if (preferences == null) return;

    // Keep writes ordered when a user changes appearance several times quickly.
    // Updating the observable first keeps the switch responsive to every tap.
    _pendingSave = _pendingSave.then((_) async {
      try {
        await preferences.setString(_preferenceKey, mode.name);
      } catch (_) {
        // Appearance changes remain usable if device storage is unavailable.
      }
    });
  }
}
