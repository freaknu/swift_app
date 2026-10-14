import 'package:flutter/material.dart';
import 'package:get/get.dart';

class AppColors {
  // Blue-grey foundation from the supplied palette.
  static const Color navy = Color(0xFF06141B);
  static const Color deepSlate = Color(0xFF11212D);
  static const Color slate = Color(0xFF253745);
  static const Color steel = Color(0xFF4A5C6A);
  static const Color silver = Color(0xFF9BA8AB);
  static const Color mist = Color(0xFFCCD0CF);

  static const Color primaryColor = slate;
  static const Color secondaryColor = steel;
  static const Color accentColor = silver;

  // Semantic accents shared by status, validation and feedback surfaces.
  static const Color successColor = Color(0xFF34C759);
  static const Color warningColor = Color(0xFFFF9500);
  static const Color errorColor = Color(0xFFFF3B30);
  static const Color infoColor = Color(0xFF007AFF);

  static const Color darkPrimaryColor = silver;

  static const Color lightPageColor = mist;

  static const Color lightInk = navy;

  static const Color lightMuted = slate;

  // A lighter mist tint separates cards from the page without pure white.
  static const Color lightSurface = Color(0xFFE6E9E8);

  static const Color lightSoftTint = mist;
  static const Color lightSoftGreen = lightSoftTint;

  static const Color lightFieldColor = mist;

  static const Color lightBorderColor = silver;

  static const Color darkPageColor = navy;

  /// Slightly elevated background.
  static const Color darkBackground = deepSlate;

  /// iOS secondary background.
  static const Color darkSurface = deepSlate;

  /// Elevated/card surface.
  static const Color darkSurfaceElevated = slate;

  /// Dark input field.
  static const Color darkFieldColor = slate;

  static const Color darkInk = mist;

  static const Color darkMuted = silver;

  static const Color darkSoftTint = slate;
  static const Color darkSoftGreen = darkSoftTint;

  static const Color darkBorderColor = steel;

  static bool get isDark => Get.isDarkMode;

  static Color get pageColor => isDark ? darkPageColor : lightPageColor;

  static Color get background => pageColor;

  static Color get ink => isDark ? darkInk : lightInk;

  static Color get muted => isDark ? darkMuted : lightMuted;

  static Color get surface => isDark ? darkSurface : lightSurface;

  static Color get elevatedSurface =>
      isDark ? darkSurfaceElevated : lightSurface;

  static Color get softTint => isDark ? darkSoftTint : lightSoftTint;
  static Color get softGreen => softTint;

  static Color get fieldColor => isDark ? darkFieldColor : lightFieldColor;

  static Color get borderColor => isDark ? darkBorderColor : lightBorderColor;

  static Color get textColor => muted;

  static Color get textPrimary => ink;

  static Color get textSecondary => muted;

  static Color get divider => borderColor;

  static Color get glassSurface => isDark
      ? mist.withValues(alpha: 0.07)
      : lightSurface.withValues(alpha: 0.82);

  static Color get glassBorder =>
      isDark ? silver.withValues(alpha: 0.18) : silver.withValues(alpha: 0.55);

  static Color get glassHighlight => isDark
      ? mist.withValues(alpha: 0.08)
      : lightSurface.withValues(alpha: 0.95);

  static Color get glassOverlay => glassSurface;

  static const Color iosRed = Color(0xFFFF3B30);
  static const Color iosOrange = Color(0xFFFF9500);
  static const Color iosYellow = Color(0xFFFFCC00);
  static const Color iosGreen = Color(0xFF34C759);
  static const Color iosBlue = Color(0xFF007AFF);
}
