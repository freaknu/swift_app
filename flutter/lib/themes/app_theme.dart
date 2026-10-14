import 'package:swift_app/themes/custom_theme_data.dart';
import 'package:flutter/material.dart';

/// Named entry point for consumers that prefer a single theme facade.
///
/// The original top-level theme functions remain the source-compatible API;
/// this wrapper simply gives new code a discoverable place to start.
class AppTheme {
  const AppTheme._();

  static ThemeData light() => customThemeData();

  static ThemeData dark() => customDarkThemeData();

  static ThemeData forBrightness(Brightness brightness) {
    return brightness == Brightness.dark ? dark() : light();
  }
}
