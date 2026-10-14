import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/themes/theme_controller.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class ThemeToggleButton extends StatelessWidget {
  final double size;

  const ThemeToggleButton({super.key, this.size = 44});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return IconButton(
      icon: AppIcon(isDark ? SwiftIcons.sun : SwiftIcons.moon, size: 18),
      tooltip: isDark ? 'Switch to light mode' : 'Switch to dark mode',
      constraints: BoxConstraints(
        minWidth: size.clamp(48, double.infinity),
        minHeight: 48,
      ),
      padding: const EdgeInsets.all(14),
      onPressed: () {
        final controller = Get.isRegistered<ThemeController>()
            ? Get.find<ThemeController>()
            : Get.put(ThemeController());
        isDark ? controller.setLightMode() : controller.setDarkMode();
      },
    );
  }
}
