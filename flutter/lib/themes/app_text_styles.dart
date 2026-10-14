import 'package:flutter/material.dart';

/// Typography tokens shared by the Material and Cupertino surfaces.
class AppTextStyles {
  const AppTextStyles._();

  /// Builds the app text theme while retaining the platform font choice made
  /// by the app theme (Poppins on non-Apple platforms, system type on Apple).
  static TextTheme buildTextTheme({
    required Brightness brightness,
    required String? fontFamily,
    required Color foreground,
    required Color muted,
  }) {
    final baseTextTheme =
        ThemeData(
          brightness: brightness,
          fontFamily: fontFamily,
        ).textTheme.apply(
          bodyColor: foreground,
          displayColor: foreground,
          fontFamily: fontFamily,
        );

    return baseTextTheme.copyWith(
      displayLarge: baseTextTheme.displayLarge?.copyWith(
        fontWeight: FontWeight.w700,
        letterSpacing: -1.8,
      ),
      displayMedium: baseTextTheme.displayMedium?.copyWith(
        fontWeight: FontWeight.w700,
        letterSpacing: -1.4,
      ),
      headlineLarge: baseTextTheme.headlineLarge?.copyWith(
        fontSize: 30,
        fontWeight: FontWeight.w700,
        letterSpacing: -0.9,
      ),
      headlineMedium: baseTextTheme.headlineMedium?.copyWith(
        fontSize: 26,
        fontWeight: FontWeight.w700,
        letterSpacing: -0.75,
      ),
      headlineSmall: baseTextTheme.headlineSmall?.copyWith(
        fontSize: 24,
        fontWeight: FontWeight.w700,
        letterSpacing: -0.55,
      ),
      titleLarge: baseTextTheme.titleLarge?.copyWith(
        fontSize: 20,
        fontWeight: FontWeight.w700,
        letterSpacing: -0.45,
      ),
      titleMedium: baseTextTheme.titleMedium?.copyWith(
        fontSize: 16,
        fontWeight: FontWeight.w600,
        letterSpacing: -0.25,
      ),
      titleSmall: baseTextTheme.titleSmall?.copyWith(
        fontSize: 14,
        fontWeight: FontWeight.w600,
        letterSpacing: -0.15,
      ),
      bodyLarge: baseTextTheme.bodyLarge?.copyWith(height: 1.35),
      bodyMedium: baseTextTheme.bodyMedium?.copyWith(height: 1.38),
      bodySmall: baseTextTheme.bodySmall?.copyWith(color: muted, height: 1.35),
      labelLarge: baseTextTheme.labelLarge?.copyWith(
        fontWeight: FontWeight.w600,
      ),
      labelMedium: baseTextTheme.labelMedium?.copyWith(
        fontWeight: FontWeight.w600,
      ),
    );
  }

  static TextStyle button(TextTheme textTheme, {Color? color}) {
    return (textTheme.labelLarge ?? const TextStyle()).copyWith(
      color: color,
      fontWeight: FontWeight.w700,
    );
  }
}
