import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class HomeSectionHeading extends StatelessWidget {
  final String title;
  final String? subtitle;
  final Widget? action;

  const HomeSectionHeading({
    super.key,
    required this.title,
    this.subtitle,
    this.action,
  });

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);

    return Padding(
      padding: const EdgeInsets.only(bottom: 11),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontSize: 17,
                    height: 1.1,
                    fontWeight: FontWeight.w700,
                    letterSpacing: -0.45,
                  ),
                ),
                if (subtitle != null) ...[
                  const SizedBox(height: 3),
                  Text(
                    subtitle!,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodySmall?.copyWith(fontSize: 11),
                  ),
                ],
              ],
            ),
          ),
          ?action,
        ],
      ),
    );
  }
}

class HomeStatusCard extends StatelessWidget {
  final SwiftIcons icon;
  final String title;
  final String? message;
  final VoidCallback? onRetry;
  final String actionLabel;
  final bool loading;

  const HomeStatusCard({
    super.key,
    required this.icon,
    required this.title,
    this.message,
    this.onRetry,
    this.actionLabel = 'Try again',
    this.loading = false,
  });

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);

    final bool isDark = theme.brightness == Brightness.dark;
    Widget card(Widget child) {
      return AppCard(
        padding: const EdgeInsets.all(15),
        opacity: 1,
        tint: theme.colorScheme.surface.withValues(alpha: 0.90),
        border: Border.all(
          color: theme.colorScheme.onSurface.withValues(alpha: 0.07),
        ),
        boxShadow: const [],
        child: child,
      );
    }

    if (loading) {
      return Semantics(
        label: title,
        child: AppShimmer(
          child: card(
            const Row(
              children: [
                ShimmerBlock(width: 42, height: 42, radius: 13),
                SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      ShimmerBlock(width: 160, height: 14),
                      SizedBox(height: 9),
                      ShimmerBlock(width: double.infinity, height: 11),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return card(
      Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 42,
            height: 42,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: theme.colorScheme.primary.withValues(
                alpha: isDark ? 0.14 : 0.08,
              ),
              borderRadius: BorderRadius.circular(13),
            ),
            child: AppIcon(icon, color: theme.colorScheme.primary, size: 20),
          ),

          const SizedBox(width: 12),

          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: theme.textTheme.titleSmall?.copyWith(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                  ),
                ),

                if (message != null) ...[
                  const SizedBox(height: 3),
                  Text(
                    message!,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodySmall?.copyWith(
                      fontSize: 11,
                      height: 1.35,
                    ),
                  ),
                ],

                if (onRetry != null) ...[
                  const SizedBox(height: 6),
                  CupertinoButton(
                    padding: EdgeInsets.zero,
                    minimumSize: const Size(44, 44),
                    onPressed: onRetry,
                    child: Text(
                      actionLabel,
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.primary,
                        fontSize: 12,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}
