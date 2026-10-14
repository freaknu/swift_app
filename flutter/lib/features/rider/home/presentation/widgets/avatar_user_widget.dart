import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class AvatarUserWidget extends StatelessWidget {
  final String name;
  final String? profileImageUrl;
  final VoidCallback? onNotificationTap;

  const AvatarUserWidget({
    super.key,
    required this.name,
    this.profileImageUrl,
    this.onNotificationTap,
  });

  static final RegExp _spaces = RegExp(r'\s+');

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);

    final bool isDark = theme.brightness == Brightness.dark;

    final String trimmed = name.trim();

    final String firstName = trimmed.isEmpty
        ? ''
        : trimmed.split(_spaces).first;

    final String? imageUrl = profileImageUrl?.trim();

    return Container(
      color: Colors.transparent,

      padding: const EdgeInsets.fromLTRB(18, 8, 8, 10),

      child: Row(
        children: [
          Container(
            width: 42,
            height: 42,

            decoration: BoxDecoration(
              shape: BoxShape.circle,

              color: theme.colorScheme.primary.withValues(
                alpha: isDark ? 0.16 : 0.09,
              ),

              border: Border.all(
                color: theme.colorScheme.primary.withValues(alpha: 0.14),
              ),
            ),

            clipBehavior: Clip.antiAlias,

            child: imageUrl != null && imageUrl.isNotEmpty
                ? Image.network(
                    imageUrl,
                    fit: BoxFit.cover,
                    // Displayed at 42px: bound the decode instead of keeping
                    // the full network resolution in memory.
                    cacheWidth: 96,
                    cacheHeight: 96,
                    errorBuilder: (_, _, _) => _avatar(theme, firstName),
                  )
                : _avatar(theme, firstName),
          ),

          const SizedBox(width: 11),

          Expanded(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Good day',
                  style: theme.textTheme.bodySmall?.copyWith(
                    fontSize: 10.5,
                    color: theme.colorScheme.onSurfaceVariant,
                    fontWeight: FontWeight.w500,
                  ),
                ),

                const SizedBox(height: 1),

                Text(
                  firstName.isEmpty ? 'Ready to ride?' : 'Hi, $firstName',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontSize: 17,
                    fontWeight: FontWeight.w700,
                    letterSpacing: -0.4,
                  ),
                ),
              ],
            ),
          ),

          if (onNotificationTap != null)
            CupertinoButton(
              padding: EdgeInsets.zero,
              minimumSize: const Size(40, 40),
              onPressed: onNotificationTap,
              child: Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: theme.colorScheme.surface,
                  border: Border.all(
                    color: theme.colorScheme.onSurface.withValues(alpha: 0.07),
                  ),
                ),
                child: AppIcon(
                  SwiftIcons.bell,
                  size: 19,
                  color: theme.colorScheme.onSurface,
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _avatar(ThemeData theme, String firstName) {
    if (firstName.isEmpty) {
      return AppIcon(
        SwiftIcons.userRound,
        color: theme.colorScheme.primary,
        size: 20,
      );
    }

    return Center(
      child: Text(
        firstName.characters.first.toUpperCase(),
        style: theme.textTheme.titleLarge?.copyWith(
          color: theme.colorScheme.primary,
          fontSize: 17,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}
