import 'package:swift_app/features/driver/data/model/driver_profile_model.dart';
import 'package:swift_app/features/driver/data/model/realtime_ride_event.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:swift_app/features/common/shared/widgets/vehicle_photo.dart';
import 'package:swift_app/themes/app_colors.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:swift_app/themes/app_spacing.dart';
import 'package:flutter/material.dart';

import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_address_point.dart';

class DriverActiveRideCard extends StatelessWidget {
  final RealtimeRideEvent event;
  final String? riderName;
  final DriverVehicleModel? vehicle;
  final VoidCallback onAction;
  final VoidCallback onCancel;
  final VoidCallback onChat;
  final bool busy;

  const DriverActiveRideCard({
    super.key,
    required this.event,
    required this.riderName,
    required this.vehicle,
    required this.onAction,
    required this.onCancel,
    required this.onChat,
    required this.busy,
  });

  String get _primaryText {
    return switch (event.rideStatus) {
      'DRIVER_ASSIGNED' => 'Arrived at pickup',
      'DRIVER_ARRIVED' => 'Start ride',
      'STARTED' => 'Complete ride',
      _ => 'View ride',
    };
  }

  String get _journeyTitle => switch (event.rideStatus) {
    'DRIVER_ASSIGNED' => 'Head to pickup',
    'DRIVER_ARRIVED' => 'Ready to start',
    'STARTED' => 'Trip in progress',
    _ => 'Active ride',
  };

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final fare = event.totalFare == null
        ? 'Fare pending'
        : '${event.currency ?? 'INR'} ${event.totalFare!.toStringAsFixed(2)}';
    return AppCard(
      padding: const EdgeInsets.fromLTRB(18, 18, 18, 16),
      opacity: 1,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          LayoutBuilder(
            builder: (context, constraints) {
              final title = Row(
                children: [
                  VehiclePhoto(
                    code: vehicle?.vehicleType ?? 'SEDAN',
                    width: 58,
                    height: 42,
                  ),
                  const SizedBox(width: AppSpacing.sm),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          _journeyTitle,
                          style: theme.textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          'Ride #${event.rideId ?? '-'}',
                          style: theme.textTheme.bodySmall,
                        ),
                      ],
                    ),
                  ),
                ],
              );
              if (constraints.maxWidth < 300 ||
                  MediaQuery.textScalerOf(context).scale(1) > 1.4) {
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    title,
                    const SizedBox(height: 10),
                    _StatusPill(status: event.rideStatus),
                  ],
                );
              }
              return Row(
                children: [
                  Expanded(child: title),
                  const SizedBox(width: 8),
                  _StatusPill(status: event.rideStatus),
                ],
              );
            },
          ),
          const SizedBox(height: 18),
          _JourneyProgress(status: event.rideStatus),
          const SizedBox(height: 18),
          Row(
            children: [
              CircleAvatar(
                radius: 19,
                backgroundColor: scheme.primary.withValues(alpha: 0.1),
                child: AppIcon(
                  SwiftIcons.userRound,
                  color: scheme.primary,
                  size: 20,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      riderName ?? 'Rider',
                      style: theme.textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    Text('Passenger', style: theme.textTheme.bodySmall),
                  ],
                ),
              ),
              IconButton(
                onPressed: busy ? null : onChat,
                tooltip: 'Chat with rider',
                icon: const AppIcon(SwiftIcons.messageCircle),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              _Metric(
                label: 'Distance',
                value: event.estimatedDistance == null
                    ? '-'
                    : '${event.estimatedDistance!.toStringAsFixed(1)} km',
              ),
              const SizedBox(width: 10),
              _Metric(label: 'Fare', value: fare),
            ],
          ),
          const SizedBox(height: 16),
          DriverAddressPoint(
            icon: SwiftIcons.mapPin,
            label: 'Pickup',
            latitude: event.pickupLatitude,
            longitude: event.pickupLongitude,
            isFirst: true,
          ),
          DriverAddressPoint(
            icon: SwiftIcons.flag,
            label: 'Destination',
            latitude: event.destinationLatitude,
            longitude: event.destinationLongitude,
            isFirst: false,
          ),
          if (event.estimatedDuration != null) ...[
            const SizedBox(height: 8),
            Text(
              'Estimated drive · ${(event.estimatedDuration! / 60).ceil()} min',
              style: theme.textTheme.bodySmall?.copyWith(
                color: scheme.primary,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
          if (vehicle != null) ...[
            const SizedBox(height: 10),
            Text(
              'Vehicle: ${vehicle!.displayName}',
              style: theme.textTheme.bodySmall,
            ),
          ],
          const SizedBox(height: 16),
          Divider(color: scheme.outlineVariant.withValues(alpha: 0.55)),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: FilledButton(
                  onPressed: busy ? null : onAction,
                  style: FilledButton.styleFrom(
                    backgroundColor: scheme.primary,
                    shape: RoundedRectangleBorder(
                      borderRadius: AppRadii.button,
                    ),
                  ),
                  child: busy
                      ? SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(
                            strokeWidth: 2.2,
                            color: scheme.onSurfaceVariant,
                          ),
                        )
                      : Text(_primaryText),
                ),
              ),
              if (!event.isFinished &&
                  event.rideStatus != 'STARTED' &&
                  event.rideStatus != 'COMPLETED') ...[
                const SizedBox(width: 10),
                IconButton(
                  onPressed: busy ? null : onCancel,
                  icon: const AppIcon(SwiftIcons.x),
                  tooltip: 'Cancel ride',
                  style: IconButton.styleFrom(
                    foregroundColor: scheme.error,
                    backgroundColor: scheme.error.withValues(alpha: 0.08),
                    disabledForegroundColor: scheme.onSurfaceVariant.withValues(
                      alpha: 0.4,
                    ),
                    minimumSize: const Size(52, 52),
                    shape: RoundedRectangleBorder(
                      borderRadius: AppRadii.button,
                    ),
                  ),
                ),
              ],
            ],
          ),
        ],
      ),
    );
  }
}

class _JourneyProgress extends StatelessWidget {
  final String status;

  const _JourneyProgress({required this.status});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final current = switch (status) {
      'DRIVER_ARRIVED' => 1,
      'STARTED' => 2,
      _ => 0,
    };
    const labels = ['Pickup', 'Start', 'Complete'];
    if (MediaQuery.textScalerOf(context).scale(1) > 1.6) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          for (var index = 0; index < labels.length; index++)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Row(
                children: [
                  AppIcon(
                    index < current ? SwiftIcons.check : SwiftIcons.circle,
                    size: 16,
                    color: index <= current
                        ? scheme.primary
                        : scheme.onSurfaceVariant,
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      labels[index],
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ),
                ],
              ),
            ),
        ],
      );
    }
    return Row(
      children: [
        for (var index = 0; index < labels.length; index++) ...[
          if (index > 0)
            Expanded(
              child: Container(
                height: 2,
                color: index <= current
                    ? scheme.primary
                    : scheme.outlineVariant,
              ),
            ),
          Column(
            children: [
              CircleAvatar(
                radius: 12,
                backgroundColor: index <= current
                    ? scheme.primary
                    : scheme.outlineVariant,
                child: AppIcon(
                  index < current
                      ? SwiftIcons.check
                      : index == 0
                      ? SwiftIcons.mapPin
                      : index == 1
                      ? SwiftIcons.play
                      : SwiftIcons.flag,
                  size: 14,
                  color: index <= current
                      ? scheme.onPrimary
                      : scheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 5),
              Text(
                labels[index],
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: index == current
                      ? scheme.primary
                      : scheme.onSurfaceVariant,
                  fontWeight: index == current
                      ? FontWeight.w600
                      : FontWeight.w500,
                ),
              ),
            ],
          ),
        ],
      ],
    );
  }
}

class _Metric extends StatelessWidget {
  final String label;
  final String value;

  const _Metric({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: theme.textTheme.bodySmall),
          const SizedBox(height: 3),
          Text(
            value,
            style: theme.textTheme.titleSmall?.copyWith(
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}

class _StatusPill extends StatelessWidget {
  final String status;

  const _StatusPill({required this.status});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final display = switch (status) {
      'DRIVER_ASSIGNED' => 'Pickup',
      'DRIVER_ARRIVED' => 'Arrived',
      'STARTED' => 'In progress',
      'COMPLETED' => 'Completed',
      'CANCELLED' => 'Cancelled',
      'REQUESTED' || 'MATCHING' || '' => 'New request',
      _ => status.replaceAll('_', ' ').toLowerCase(),
    };
    final color = switch (status) {
      'CANCELLED' => scheme.error,
      'STARTED' || 'DRIVER_ARRIVED' => AppColors.iosBlue,
      _ => scheme.primary,
    };

    return Semantics(
      label: 'Ride status: $display',
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        decoration: BoxDecoration(
          color: color.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(999),
          border: Border.all(color: color.withValues(alpha: 0.15)),
        ),
        child: Text(
          display,
          style: Theme.of(context).textTheme.labelSmall?.copyWith(
            color: color,
            fontWeight: FontWeight.w600,
            letterSpacing: 0.15,
          ),
        ),
      ),
    );
  }
}
