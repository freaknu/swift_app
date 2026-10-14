import 'package:swift_app/themes/app_colors.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:swift_app/themes/app_spacing.dart';
import 'package:swift_app/themes/app_text_styles.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

ThemeData customThemeData() => _buildTheme(Brightness.light);

ThemeData customDarkThemeData() => _buildTheme(Brightness.dark);

ThemeData _buildTheme(Brightness brightness) {
  final isDark = brightness == Brightness.dark;
  final isApple =
      defaultTargetPlatform == TargetPlatform.iOS ||
      defaultTargetPlatform == TargetPlatform.macOS;
  // Let Flutter use the native San Francisco font on Apple platforms.
  final String? fontFamily = isApple ? null : 'Poppins';
  final primary = isDark ? AppColors.darkPrimaryColor : AppColors.primaryColor;
  final page = isDark ? AppColors.darkPageColor : AppColors.lightPageColor;
  final surface = isDark ? AppColors.darkSurface : AppColors.lightSurface;
  final ink = isDark ? AppColors.darkInk : AppColors.lightInk;
  final muted = isDark ? AppColors.darkMuted : AppColors.lightMuted;
  final border = isDark
      ? AppColors.darkBorderColor
      : AppColors.lightBorderColor;
  final field = isDark ? AppColors.darkFieldColor : AppColors.lightFieldColor;

  final colorScheme = ColorScheme(
    brightness: brightness,
    primary: primary,
    onPrimary: isDark ? AppColors.navy : AppColors.mist,
    primaryContainer: isDark ? AppColors.slate : AppColors.mist,
    onPrimaryContainer: ink,
    primaryFixed: AppColors.mist,
    primaryFixedDim: AppColors.silver,
    onPrimaryFixed: AppColors.navy,
    onPrimaryFixedVariant: AppColors.slate,
    secondary: isDark ? AppColors.silver : AppColors.steel,
    onSecondary: isDark ? AppColors.navy : AppColors.lightSurface,
    secondaryContainer: isDark ? AppColors.slate : AppColors.mist,
    onSecondaryContainer: ink,
    secondaryFixed: AppColors.mist,
    secondaryFixedDim: AppColors.silver,
    onSecondaryFixed: AppColors.navy,
    onSecondaryFixedVariant: AppColors.slate,
    tertiary: isDark ? AppColors.mist : AppColors.deepSlate,
    onTertiary: isDark ? AppColors.navy : AppColors.mist,
    tertiaryContainer: isDark ? AppColors.slate : AppColors.mist,
    onTertiaryContainer: ink,
    tertiaryFixed: AppColors.mist,
    tertiaryFixedDim: AppColors.silver,
    onTertiaryFixed: AppColors.navy,
    onTertiaryFixedVariant: AppColors.slate,
    surface: surface,
    onSurface: ink,
    surfaceDim: page,
    surfaceBright: isDark ? AppColors.slate : AppColors.lightSurface,
    surfaceContainerLowest: page,
    surfaceContainerLow: surface,
    surfaceContainer: surface,
    surfaceContainerHigh: field,
    surfaceContainerHighest: field,
    onSurfaceVariant: muted,
    outline: isDark ? AppColors.silver : AppColors.steel,
    outlineVariant: border,
    inverseSurface: isDark ? AppColors.mist : AppColors.deepSlate,
    onInverseSurface: isDark ? AppColors.navy : AppColors.mist,
    inversePrimary: isDark ? AppColors.slate : AppColors.silver,
    surfaceTint: Colors.transparent,
    shadow: AppColors.navy,
    scrim: AppColors.navy,
    error: AppColors.iosRed,
    onError: Colors.white,
    errorContainer: Color.alphaBlend(
      AppColors.errorColor.withValues(alpha: 0.12),
      surface,
    ),
    onErrorContainer: ink,
  );

  final textTheme = AppTextStyles.buildTextTheme(
    brightness: brightness,
    fontFamily: fontFamily,
    foreground: ink,
    muted: muted,
  );

  OutlineInputBorder inputBorder(Color color, [double width = 1]) {
    return OutlineInputBorder(
      borderRadius: AppRadii.button,
      borderSide: BorderSide(color: color, width: width),
    );
  }

  return ThemeData(
    useMaterial3: true,
    brightness: brightness,
    colorScheme: colorScheme,
    scaffoldBackgroundColor: page,
    canvasColor: page,
    primaryColor: primary,
    fontFamily: fontFamily,
    splashFactory: isApple ? NoSplash.splashFactory : InkRipple.splashFactory,
    highlightColor: isApple ? Colors.transparent : null,
    visualDensity: VisualDensity.standard,
    pageTransitionsTheme: const PageTransitionsTheme(
      builders: {
        TargetPlatform.android: CupertinoPageTransitionsBuilder(),
        TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
        TargetPlatform.macOS: CupertinoPageTransitionsBuilder(),
      },
    ),
    textTheme: textTheme,
    iconTheme: IconThemeData(color: ink, size: 20),
    dividerColor: border,
    dividerTheme: DividerThemeData(color: border, thickness: 0.7, space: 1),
    appBarTheme: AppBarTheme(
      backgroundColor: Colors.transparent,
      foregroundColor: ink,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: false,
      surfaceTintColor: Colors.transparent,
      titleTextStyle: textTheme.titleLarge,
      iconTheme: IconThemeData(color: ink, size: 20),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: field,
      hintStyle: textTheme.bodyMedium?.copyWith(color: muted),
      labelStyle: textTheme.bodyMedium?.copyWith(color: muted),
      floatingLabelStyle: textTheme.bodySmall?.copyWith(
        color: primary,
        fontWeight: FontWeight.w600,
      ),
      prefixIconColor: muted,
      suffixIconColor: muted,
      contentPadding: AppSpacing.input,
      border: inputBorder(border),
      enabledBorder: inputBorder(border),
      disabledBorder: inputBorder(border.withValues(alpha: 0.48)),
      focusedBorder: inputBorder(primary.withValues(alpha: 0.78), 1.35),
      errorBorder: inputBorder(AppColors.iosRed.withValues(alpha: 0.62)),
      focusedErrorBorder: inputBorder(AppColors.iosRed, 1.35),
      errorMaxLines: 3,
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: primary,
        foregroundColor: colorScheme.onPrimary,
        disabledBackgroundColor: field,
        disabledForegroundColor: muted.withValues(alpha: 0.7),
        minimumSize: const Size(48, AppSpacing.controlHeight),
        padding: AppSpacing.control,
        elevation: 0,
        shape: RoundedRectangleBorder(borderRadius: AppRadii.button),
        textStyle: AppTextStyles.button(textTheme),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: ink,
        backgroundColor: isDark
            ? AppColors.mist.withValues(alpha: 0.045)
            : AppColors.lightSurface.withValues(alpha: 0.52),
        minimumSize: const Size(48, AppSpacing.controlHeight),
        padding: AppSpacing.outlinedControl,
        side: BorderSide(color: border),
        shape: RoundedRectangleBorder(borderRadius: AppRadii.button),
        textStyle: textTheme.labelLarge,
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        foregroundColor: primary,
        minimumSize: const Size(44, 44),
        shape: RoundedRectangleBorder(borderRadius: AppRadii.textButtonRadius),
        textStyle: textTheme.labelLarge,
      ),
    ),
    iconButtonTheme: IconButtonThemeData(
      style: IconButton.styleFrom(
        foregroundColor: ink,
        iconSize: 20,
        minimumSize: const Size(44, 44),
        shape: RoundedRectangleBorder(borderRadius: AppRadii.iconButtonRadius),
      ),
    ),
    cardTheme: CardThemeData(
      color: surface,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: AppRadii.cardRadius,
        side: BorderSide(color: border),
      ),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: Colors.transparent,
      surfaceTintColor: Colors.transparent,
      indicatorColor: primary.withValues(alpha: isDark ? 0.18 : 0.11),
      labelTextStyle: WidgetStateProperty.resolveWith((states) {
        return textTheme.labelSmall?.copyWith(
          color: states.contains(WidgetState.selected) ? primary : muted,
          fontWeight: states.contains(WidgetState.selected)
              ? FontWeight.w700
              : FontWeight.w500,
        );
      }),
    ),
    bottomNavigationBarTheme: BottomNavigationBarThemeData(
      backgroundColor: Colors.transparent,
      selectedItemColor: primary,
      unselectedItemColor: muted,
      elevation: 0,
    ),
    dialogTheme: DialogThemeData(
      backgroundColor: surface,
      surfaceTintColor: Colors.transparent,
      shape: RoundedRectangleBorder(borderRadius: AppRadii.dialogRadius),
    ),
    bottomSheetTheme: const BottomSheetThemeData(
      backgroundColor: Colors.transparent,
      surfaceTintColor: Colors.transparent,
      modalBackgroundColor: Colors.transparent,
      shape: RoundedRectangleBorder(borderRadius: AppRadii.sheetRadius),
    ),
    snackBarTheme: SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      backgroundColor: colorScheme.inverseSurface,
      contentTextStyle: textTheme.bodyMedium?.copyWith(
        color: colorScheme.onInverseSurface,
      ),
      elevation: 0,
      insetPadding: const EdgeInsets.all(14),
      shape: RoundedRectangleBorder(borderRadius: AppRadii.button),
    ),
    progressIndicatorTheme: ProgressIndicatorThemeData(color: primary),
    cupertinoOverrideTheme: CupertinoThemeData(
      brightness: brightness,
      primaryColor: primary,
      scaffoldBackgroundColor: Colors.transparent,
      barBackgroundColor: Colors.transparent,
      textTheme: CupertinoTextThemeData(
        primaryColor: primary,
        textStyle: TextStyle(color: ink, fontFamily: fontFamily, fontSize: 17),
        actionTextStyle: TextStyle(
          color: primary,
          fontFamily: fontFamily,
          fontWeight: FontWeight.w600,
        ),
        actionSmallTextStyle: TextStyle(
          color: primary,
          fontFamily: fontFamily,
          fontWeight: FontWeight.w600,
        ),
        tabLabelTextStyle: TextStyle(
          color: muted,
          fontFamily: fontFamily,
          fontWeight: FontWeight.w600,
        ),
        navTitleTextStyle: TextStyle(
          color: ink,
          fontFamily: fontFamily,
          fontSize: 17,
          fontWeight: FontWeight.w600,
        ),
        navLargeTitleTextStyle: TextStyle(
          color: ink,
          fontFamily: fontFamily,
          fontSize: 34,
          fontWeight: FontWeight.w700,
        ),
        navActionTextStyle: TextStyle(
          color: primary,
          fontFamily: fontFamily,
          fontWeight: FontWeight.w600,
        ),
        pickerTextStyle: TextStyle(color: ink, fontFamily: fontFamily),
        dateTimePickerTextStyle: TextStyle(color: ink, fontFamily: fontFamily),
      ),
    ),
  );
}
