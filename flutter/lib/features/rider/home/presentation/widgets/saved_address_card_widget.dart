import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';
import 'package:flutter/material.dart';

class SavedAddressCard extends StatelessWidget {
  final UserAddress address;
  final VoidCallback? onTap;
  final bool expanded;

  const SavedAddressCard({
    super.key,
    required this.address,
    this.onTap,
    this.expanded = false,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final icon = switch (address.addressType.name) {
      'HOME' => SwiftIcons.house,
      'WORK' => SwiftIcons.briefcase,
      _ => SwiftIcons.bookmark,
    };

    return AppCard(
      padding: EdgeInsets.zero,
      boxShadow: const [],
      onTap: onTap,
      child: SizedBox(
        width: expanded ? null : 185,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 11),
          child: Row(
            children: [
              Container(
                width: 38,
                height: 38,
                decoration: BoxDecoration(
                  color: theme.colorScheme.primary.withValues(
                    alpha: isDark ? 0.16 : 0.09,
                  ),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: theme.colorScheme.primary.withValues(alpha: 0.08),
                  ),
                ),
                child: AppIcon(
                  icon,
                  color: theme.colorScheme.primary,
                  size: 18,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      address.label,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.titleSmall?.copyWith(fontSize: 13),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      address.fullAddress.isEmpty
                          ? 'Saved location'
                          : address.fullAddress,
                      maxLines: expanded ? 2 : 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodySmall?.copyWith(
                        fontSize: 10.5,
                      ),
                    ),
                  ],
                ),
              ),
              if (expanded) ...[
                const SizedBox(width: 8),
                AppIcon(
                  SwiftIcons.chevronRight,
                  color: theme.colorScheme.onSurfaceVariant,
                  size: 16,
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
