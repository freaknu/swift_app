import 'package:swift_app/themes/app_colors.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:flutter/material.dart';

class AppMessenger {
  static final GlobalKey<ScaffoldMessengerState> key =
      GlobalKey<ScaffoldMessengerState>();

  static void showMessage(String message) {
    _show(message, SwiftIcons.info, AppColors.iosBlue);
  }

  static void showError(String message) {
    _show(message, SwiftIcons.circleAlert, AppColors.iosRed);
  }

  static void showSuccess(String message) {
    _show(message, SwiftIcons.circleCheck, AppColors.iosGreen);
  }

  static void _show(String message, SwiftIcons icon, Color accent) {
    key.currentState
      ?..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Row(
            children: [
              AppIcon(icon, color: accent, size: 19),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  message,
                  style: const TextStyle(color: AppColors.mist),
                ),
              ),
            ],
          ),
          behavior: SnackBarBehavior.floating,
          backgroundColor: AppColors.slate.withValues(alpha: 0.94),
          elevation: 0,
          shape: RoundedRectangleBorder(borderRadius: AppRadii.button),
          duration: const Duration(seconds: 3),
        ),
      );
  }
}
