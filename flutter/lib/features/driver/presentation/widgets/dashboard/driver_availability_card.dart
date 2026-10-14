import 'package:swift_app/features/driver/data/model/profile_completeness_model.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:swift_app/themes/app_colors.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:swift_app/themes/app_spacing.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class DriverCompleteProfileBanner extends StatelessWidget {
  final ProfileCompletenessModel completeness;
  final VoidCallback onTap;

  const DriverCompleteProfileBanner({
    super.key,
    required this.completeness,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = Theme.of(context).colorScheme;
    final remaining = completeness.missing.length;
    return AppCard.flush(
      margin: const EdgeInsets.fromLTRB(16, 14, 16, 8),
      opacity: 1,
      border: Border.all(color: scheme.primary.withValues(alpha: 0.2)),
      semanticLabel: 'Complete your driver profile',
      child: Material(
        type: MaterialType.transparency,
        child: InkWell(
          borderRadius: AppRadii.cardRadius,
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 28,
                  height: 28,
                  decoration: BoxDecoration(
                    color: scheme.primary.withValues(alpha: 0.13),
                    borderRadius: AppRadii.small,
                  ),
                  child: AppIcon(
                    SwiftIcons.userRound,
                    color: scheme.primary,
                    size: 18,
                  ),
                ),
                const SizedBox(width: AppSpacing.sm),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              'Finish your profile',
                              style: theme.textTheme.titleSmall?.copyWith(
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                          AppIcon(
                            SwiftIcons.chevronRight,
                            size: 14,
                            color: scheme.onSurfaceVariant,
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        remaining == 0
                            ? 'Add the remaining details to go online.'
                            : '$remaining ${remaining == 1 ? 'detail' : 'details'} remaining before you can go online.',
                        style: theme.textTheme.bodySmall,
                      ),
                      const SizedBox(height: 12),
                      ClipRRect(
                        borderRadius: BorderRadius.circular(999),
                        child: LinearProgressIndicator(
                          value: _profileProgress(completeness),
                          minHeight: 4,
                          backgroundColor: scheme.onSurface.withValues(
                            alpha: 0.08,
                          ),
                          color: scheme.primary,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

double _profileProgress(ProfileCompletenessModel completeness) {
  final completed = [
    completeness.personal.complete,
    completeness.vehicle.complete,
    completeness.documents.complete,
  ].where((section) => section).length;
  return completed / 3;
}

class DriverAvailabilityCard extends StatelessWidget {
  final bool online;
  final bool busyWithRide;
  final bool loading;
  final ValueChanged<bool>? onChanged;

  const DriverAvailabilityCard({
    super.key,
    required this.online,
    required this.busyWithRide,
    required this.loading,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final statusColor = busyWithRide
        ? AppColors.iosBlue
        : online
        ? scheme.primary
        : scheme.onSurfaceVariant;
    return AppCard.flush(
      margin: const EdgeInsets.fromLTRB(16, 14, 16, 8),
      opacity: 1,
      border: Border.all(
        color: statusColor.withValues(
          alpha: online || busyWithRide ? 0.18 : 0.1,
        ),
      ),
      semanticLabel: 'Driver availability status',
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            _StatusDot(active: online || busyWithRide),
            const SizedBox(width: AppSpacing.sm),
            Expanded(
              child: AnimatedSwitcher(
                duration: MediaQuery.disableAnimationsOf(context)
                    ? Duration.zero
                    : const Duration(milliseconds: 220),
                transitionBuilder: (child, animation) => FadeTransition(
                  opacity: animation,
                  child: SlideTransition(
                    position: Tween<Offset>(
                      begin: const Offset(0, 0.15),
                      end: Offset.zero,
                    ).animate(animation),
                    child: child,
                  ),
                ),
                child: Column(
                  key: ValueKey('$online-$busyWithRide'),
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      busyWithRide
                          ? 'On a ride'
                          : online
                          ? 'You’re online'
                          : 'You’re offline',
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      loading
                          ? 'Updating availability…'
                          : busyWithRide
                          ? 'New requests are paused until this trip ends'
                          : online
                          ? 'Ready to receive ride requests'
                          : 'Go online when you’re ready to drive',
                      style: theme.textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(width: AppSpacing.sm),
            Semantics(
              label: busyWithRide
                  ? 'Availability locked while a ride is in progress'
                  : online
                  ? 'Go offline and pause ride requests'
                  : 'Go online and receive ride requests',
              value: busyWithRide
                  ? 'On a ride'
                  : online
                  ? 'Online'
                  : 'Offline',
              toggled: online,
              child: CupertinoSwitch(
                value: online,
                activeTrackColor: scheme.primary,
                onChanged: loading || busyWithRide ? null : onChanged,
              ),
            ),
            if (loading) ...[
              const SizedBox(width: 8),
              const SizedBox(
                width: 14,
                height: 14,
                child: CircularProgressIndicator(strokeWidth: 1.8),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _StatusDot extends StatelessWidget {
  final bool active;

  const _StatusDot({required this.active});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final color = active ? scheme.primary : scheme.onSurfaceVariant;
    return AnimatedContainer(
      duration: MediaQuery.disableAnimationsOf(context)
          ? Duration.zero
          : const Duration(milliseconds: 260),
      width: 8,
      height: 8,
      decoration: BoxDecoration(shape: BoxShape.circle, color: color),
    );
  }
}
