import 'package:swift_app/features/driver/data/model/driver_profile_model.dart';
import 'package:swift_app/features/driver/data/model/driver_statistics_model.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:swift_app/features/common/shared/widgets/vehicle_photo.dart';
import 'package:swift_app/themes/app_spacing.dart';
import 'package:flutter/material.dart';

import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_dashboard_states.dart';

class DriverLifetimeSummary extends StatelessWidget {
  final DriverStatisticsModel? statistics;
  final bool loading;
  final String? error;
  final VoidCallback onRetry;
  final VoidCallback onSeeTrips;

  const DriverLifetimeSummary({
    super.key,
    required this.statistics,
    required this.loading,
    required this.error,
    required this.onRetry,
    required this.onSeeTrips,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return AppCard(
      margin: const EdgeInsets.fromLTRB(16, 14, 16, 4),
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          LayoutBuilder(
            builder: (context, constraints) {
              final heading = Text(
                'Lifetime totals',
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w600,
                ),
              );
              final tripsAction = TextButton(
                onPressed: onSeeTrips,
                child: const Text('See trips'),
              );
              if (constraints.maxWidth < 280 ||
                  MediaQuery.textScalerOf(context).scale(1) > 1.4) {
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [heading, tripsAction],
                );
              }
              return Row(
                children: [
                  Expanded(child: heading),
                  tripsAction,
                ],
              );
            },
          ),
          if (statistics == null && loading)
            const AppShimmer(
              child: Row(
                children: [
                  Expanded(
                    child: DriverSkeletonBlock(
                      width: double.infinity,
                      height: 56,
                    ),
                  ),
                  SizedBox(width: 12),
                  Expanded(
                    child: DriverSkeletonBlock(
                      width: double.infinity,
                      height: 56,
                    ),
                  ),
                ],
              ),
            )
          else if (statistics == null)
            Row(
              children: [
                Expanded(
                  child: Text(
                    error ?? 'Your trip totals are unavailable.',
                    style: theme.textTheme.bodySmall,
                  ),
                ),
                TextButton(onPressed: onRetry, child: const Text('Retry')),
              ],
            )
          else
            LayoutBuilder(
              builder: (context, constraints) {
                final trips = _SummaryMetric(
                  value: '${statistics!.completedTrips}',
                  label: 'Completed trips',
                );
                final earnings = _SummaryMetric(
                  value: '₹${statistics!.lifetimeEarning.toStringAsFixed(0)}',
                  label: 'Total earnings',
                );
                if (constraints.maxWidth < 250 ||
                    MediaQuery.textScalerOf(context).scale(1) > 1.6) {
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [trips, const SizedBox(height: 16), earnings],
                  );
                }
                return Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(child: trips),
                    Container(
                      width: 1,
                      height: 44,
                      color: scheme.outlineVariant,
                    ),
                    const SizedBox(width: 20),
                    Expanded(child: earnings),
                  ],
                );
              },
            ),
        ],
      ),
    );
  }
}

class _SummaryMetric extends StatelessWidget {
  final String value;
  final String label;

  const _SummaryMetric({required this.value, required this.label});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          value,
          style: theme.textTheme.headlineSmall?.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          label,
          style: theme.textTheme.bodySmall?.copyWith(
            color: scheme.onSurfaceVariant,
          ),
        ),
      ],
    );
  }
}

class DriverVehicleSummary extends StatelessWidget {
  final DriverVehicleModel vehicle;
  final VoidCallback onTap;

  const DriverVehicleSummary({
    super.key,
    required this.vehicle,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final registration = vehicle.registrationNumber.trim();
    return AppCard(
      margin: const EdgeInsets.fromLTRB(16, 8, 16, 6),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      opacity: 1,
      semanticLabel: 'Edit vehicle and documents, ${vehicle.displayName}',
      onTap: onTap,
      child: Row(
        children: [
          VehiclePhoto(code: vehicle.vehicleType, width: 76, height: 52),
          const SizedBox(width: AppSpacing.sm),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  vehicle.displayName,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                ),
                if (registration.isNotEmpty) ...[
                  const SizedBox(height: 2),
                  Text(
                    registration,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.labelMedium?.copyWith(
                      color: scheme.primary,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
                if (vehicle.verified != null) ...[
                  const SizedBox(height: 2),
                  Text(
                    vehicle.verified! ? 'Verified' : 'Review pending',
                    style: theme.textTheme.labelSmall?.copyWith(
                      color: vehicle.verified!
                          ? scheme.primary
                          : scheme.onSurfaceVariant,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(width: 8),
          AppIcon(
            SwiftIcons.chevronRight,
            size: 18,
            color: scheme.onSurfaceVariant,
          ),
        ],
      ),
    );
  }
}
