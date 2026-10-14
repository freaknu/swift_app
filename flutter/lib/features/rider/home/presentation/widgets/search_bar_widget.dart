import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:swift_app/themes/app_radii.dart';

class SearchBarWidget extends StatelessWidget {
  final String title;
  final String subtitle;
  final VoidCallback onTap;
  final VoidCallback? onClear;

  const SearchBarWidget({
    super.key,
    this.title = 'Where to?',
    this.subtitle = 'Search destination',
    required this.onTap,
    this.onClear,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 0, 12, 16),
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: theme.colorScheme.primary.withValues(alpha: 0.08),
          borderRadius: AppRadii.cardRadius,
        ),
        child: Row(
          children: [
            Expanded(
              child: CupertinoButton(
                padding: const EdgeInsets.all(16),
                onPressed: onTap,
                child: Row(
                  children: [
                    AppIcon(
                      SwiftIcons.search,
                      color: theme.colorScheme.primary,
                      size: 20,
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            title,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.titleLarge?.copyWith(
                              fontSize: 22,
                            ),
                          ),
                          if (subtitle.isNotEmpty) ...[
                            const SizedBox(height: 4),
                            Text(
                              subtitle,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: theme.textTheme.bodySmall,
                            ),
                          ],
                        ],
                      ),
                    ),
                    if (onClear == null)
                      AppIcon(
                        SwiftIcons.chevronRight,
                        size: 17,
                        color: theme.colorScheme.primary,
                      ),
                  ],
                ),
              ),
            ),
            if (onClear != null)
              CupertinoButton(
                padding: const EdgeInsets.all(12),
                minimumSize: const Size(44, 44),
                onPressed: onClear,
                child: const AppIcon(
                  SwiftIcons.circleX,
                  semanticLabel: 'Clear destination',
                  size: 22,
                ),
              ),
          ],
        ),
      ),
    );
  }
}
