import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:flutter/material.dart';

void showDriverSnackBar(BuildContext context, String message) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(
      SnackBar(
        content: Text(message),
        behavior: SnackBarBehavior.floating,
        margin: const EdgeInsets.all(16),
        shape: RoundedRectangleBorder(borderRadius: AppRadii.button),
      ),
    );
}

class DriverRideLoadingCard extends StatelessWidget {
  const DriverRideLoadingCard({super.key});

  @override
  Widget build(BuildContext context) {
    return AppShimmer(
      child: AppCard(
        opacity: 0.85,
        semanticLabel: 'Loading ride offers',
        child: const Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            DriverSkeletonBlock(width: 160, height: 16),
            SizedBox(height: 22),
            DriverSkeletonBlock(width: double.infinity, height: 13),
            SizedBox(height: 12),
            DriverSkeletonBlock(width: 220, height: 13),
            SizedBox(height: 20),
            DriverSkeletonBlock(width: double.infinity, height: 45),
          ],
        ),
      ),
    );
  }
}

class DriverSectionLabel extends StatelessWidget {
  final String eyebrow;
  final String title;

  const DriverSectionLabel({
    super.key,
    required this.eyebrow,
    required this.title,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return Text(
      title,
      style: theme.textTheme.titleMedium?.copyWith(
        color: scheme.onSurface,
        fontWeight: FontWeight.w600,
      ),
    );
  }
}

class DriverSkeletonBlock extends StatelessWidget {
  final double width;
  final double height;

  const DriverSkeletonBlock({
    super.key,
    required this.width,
    required this.height,
  });

  @override
  Widget build(BuildContext context) =>
      ShimmerBlock(width: width, height: height, radius: 999);
}

class DriverMessageState extends StatelessWidget {
  final SwiftIcons icon;
  final String title;
  final String message;
  final String? actionText;
  final VoidCallback? onAction;

  const DriverMessageState({
    super.key,
    required this.icon,
    required this.title,
    required this.message,
    this.actionText,
    this.onAction,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return Center(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(12, 18, 12, 32),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 440),
          child: AppCard(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 20),
            opacity: 1,
            semanticLabel: title,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 36,
                  height: 36,
                  decoration: BoxDecoration(
                    color: scheme.primary.withValues(alpha: 0.1),
                    shape: BoxShape.circle,
                  ),
                  child: AppIcon(icon, size: 20, color: scheme.primary),
                ),
                const SizedBox(height: 16),
                Text(
                  title,
                  textAlign: TextAlign.center,
                  style: theme.textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 7),
                Text(
                  message,
                  textAlign: TextAlign.center,
                  style: theme.textTheme.bodyMedium,
                ),
                if (actionText != null && onAction != null) ...[
                  const SizedBox(height: 16),
                  OutlinedButton(onPressed: onAction, child: Text(actionText!)),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
