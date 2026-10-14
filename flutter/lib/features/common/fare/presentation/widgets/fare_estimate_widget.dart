import 'package:swift_app/features/common/fare/presentation/blocs/fare_bloc.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_state.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

class FareEstimateWidget extends StatelessWidget {
  const FareEstimateWidget({super.key});

  String _formatDuration(int seconds) {
    final minutes = (seconds / 60).round();
    if (minutes < 60) return '$minutes min';
    final hours = minutes ~/ 60;
    final remainingMinutes = minutes % 60;
    return remainingMinutes == 0
        ? '${hours}h'
        : '${hours}h ${remainingMinutes}m';
  }

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<FareBloc, FareState>(
      builder: (context, state) {
        if (state.status == FareStatus.initial) return const SizedBox.shrink();

        if (state.status == FareStatus.loading) {
          return Semantics(
            label: 'Calculating your fare',
            child: const _FareGlassCard(
              child: AppShimmer(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: ShimmerBlock(
                            width: double.infinity,
                            height: 18,
                          ),
                        ),
                        SizedBox(width: 30),
                        ShimmerBlock(width: 92, height: 22),
                      ],
                    ),
                    SizedBox(height: 10),
                    ShimmerBlock(width: 150, height: 11),
                    SizedBox(height: 24),
                    Row(
                      children: [
                        Expanded(
                          child: ShimmerBlock(
                            width: double.infinity,
                            height: 46,
                          ),
                        ),
                        SizedBox(width: 10),
                        Expanded(
                          child: ShimmerBlock(
                            width: double.infinity,
                            height: 46,
                          ),
                        ),
                      ],
                    ),
                    SizedBox(height: 19),
                    ShimmerBlock(width: double.infinity, height: 12),
                    SizedBox(height: 11),
                    ShimmerBlock(width: double.infinity, height: 12),
                    SizedBox(height: 11),
                    ShimmerBlock(width: double.infinity, height: 12),
                  ],
                ),
              ),
            ),
          );
        }

        if (state.status == FareStatus.failure) {
          return _FareGlassCard(
            child: Row(
              children: [
                AppIcon(
                  SwiftIcons.circleAlert,
                  size: 20,
                  color: Theme.of(context).colorScheme.error,
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    state.message ?? 'Unable to calculate the fare.',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ),
              ],
            ),
          );
        }

        final estimate = state.fareEstimate;
        if (estimate == null) return const SizedBox.shrink();
        final fare = estimate.fare;
        final theme = Theme.of(context);

        return _FareGlassCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Estimated fare',
                          style: theme.textTheme.titleSmall,
                        ),
                        const SizedBox(height: 3),
                        Text(
                          'Final fare may vary slightly',
                          style: theme.textTheme.bodySmall?.copyWith(
                            fontSize: 10.5,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 12),
                  Text(
                    '${fare.currency} ${fare.totalFare.toStringAsFixed(2)}',
                    style: theme.textTheme.titleLarge?.copyWith(
                      color: theme.colorScheme.primary,
                      fontWeight: FontWeight.w700,
                      letterSpacing: -0.55,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: _InfoItem(
                      icon: SwiftIcons.map,
                      title: 'Distance',
                      value:
                          '${estimate.estimatedDistanceKm.toStringAsFixed(1)} km',
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: _InfoItem(
                      icon: SwiftIcons.clock3,
                      title: 'Duration',
                      value: _formatDuration(estimate.estimatedDurationSeconds),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              const Divider(),
              const SizedBox(height: 12),
              _FareRow(
                title: 'Base fare',
                value: fare.baseFare,
                currency: fare.currency,
              ),
              _FareRow(
                title: 'Distance',
                value: fare.distanceFare,
                currency: fare.currency,
              ),
              _FareRow(
                title: 'Time',
                value: fare.timeFare,
                currency: fare.currency,
              ),
              _FareRow(
                title: 'Booking fee',
                value: fare.bookingFee,
                currency: fare.currency,
              ),
              _FareRow(title: 'Tax', value: fare.tax, currency: fare.currency),
              if (fare.discount > 0)
                _FareRow(
                  title: 'Discount',
                  value: -fare.discount,
                  currency: fare.currency,
                  valueColor: theme.colorScheme.primary,
                ),
            ],
          ),
        );
      },
    );
  }
}

class _FareGlassCard extends StatelessWidget {
  final Widget child;

  const _FareGlassCard({required this.child});

  @override
  Widget build(BuildContext context) {
    return AppCard(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      padding: const EdgeInsets.all(16),
      child: child,
    );
  }
}

class _InfoItem extends StatelessWidget {
  final SwiftIcons icon;
  final String title;
  final String value;

  const _InfoItem({
    required this.icon,
    required this.title,
    required this.value,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: theme.colorScheme.primary.withValues(alpha: 0.075),
        borderRadius: AppRadii.small,
      ),
      child: Row(
        children: [
          AppIcon(icon, size: 18, color: theme.colorScheme.primary),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: theme.textTheme.bodySmall?.copyWith(fontSize: 10),
                ),
                const SizedBox(height: 2),
                Text(
                  value,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.labelMedium,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _FareRow extends StatelessWidget {
  final String title;
  final double value;
  final String currency;
  final Color? valueColor;

  const _FareRow({
    required this.title,
    required this.value,
    required this.currency,
    this.valueColor,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        children: [
          Expanded(child: Text(title, style: theme.textTheme.bodySmall)),
          Text(
            '$currency ${value.toStringAsFixed(2)}',
            style: theme.textTheme.bodySmall?.copyWith(
              color: valueColor ?? theme.colorScheme.onSurface,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}
