import 'dart:async';

import 'package:swift_app/features/driver/data/model/pending_offer_model.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:flutter/material.dart';

import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_dashboard_states.dart';
import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_address_point.dart';

class DriverOfferCard extends StatelessWidget {
  final PendingOfferModel offer;
  final bool busy;
  final bool loadingDetails;
  final VoidCallback onAccept;
  final VoidCallback onReject;

  const DriverOfferCard({
    super.key,
    required this.offer,
    required this.busy,
    required this.loadingDetails,
    required this.onAccept,
    required this.onReject,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final fare = offer.totalFare == null
        ? '—'
        : '${offer.currency == 'INR' ? '₹' : (offer.currency ?? '₹')} ${offer.totalFare!.toStringAsFixed(0)}';
    final tripDistance = offer.estimatedDistance == null
        ? '—'
        : '${offer.estimatedDistance!.toStringAsFixed(1)} km';
    final tripMinutes = offer.estimatedDuration == null
        ? '—'
        : '${(offer.estimatedDuration! / 60).ceil()} min';
    final pickupDistance = offer.distanceKm == null
        ? '—'
        : '${offer.distanceKm!.toStringAsFixed(1)} km away';

    return AppCard(
      padding: const EdgeInsets.fromLTRB(18, 18, 18, 16),
      opacity: 1,
      border: Border.all(color: scheme.primary.withValues(alpha: 0.2)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'New ride request',
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      'Ride #${offer.rideId ?? '—'}',
                      style: theme.textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
              RepaintBoundary(child: _OfferCountdown(offer: offer)),
            ],
          ),
          const SizedBox(height: 14),
          Wrap(
            spacing: 8,
            runSpacing: 6,
            children: [
              if (offer.rideTypeCode?.isNotEmpty == true)
                _OfferInfoChip(
                  icon: SwiftIcons.car,
                  label: offer.rideTypeCode!,
                ),
              if (offer.estimatedDistance != null)
                _OfferInfoChip(icon: SwiftIcons.route, label: tripDistance),
              if (offer.estimatedDuration != null)
                _OfferInfoChip(icon: SwiftIcons.clock3, label: tripMinutes),
              if (offer.distanceKm != null)
                _OfferInfoChip(
                  icon: SwiftIcons.navigation,
                  label: pickupDistance,
                ),
            ],
          ),
          const SizedBox(height: 14),
          Text(
            'Estimated fare',
            style: theme.textTheme.bodySmall?.copyWith(
              color: scheme.onSurfaceVariant,
            ),
          ),
          Text(
            fare,
            style: theme.textTheme.headlineMedium?.copyWith(
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 18),
          DriverAddressPoint(
            icon: SwiftIcons.mapPin,
            label: 'Pickup',
            latitude: offer.pickupLatitude,
            longitude: offer.pickupLongitude,
            isFirst: true,
          ),
          DriverAddressPoint(
            icon: SwiftIcons.flag,
            label: 'Destination',
            latitude: offer.destinationLatitude,
            longitude: offer.destinationLongitude,
            isFirst: false,
          ),
          if (loadingDetails && !offer.hasTripDetails) ...[
            const SizedBox(height: 12),
            const AppShimmer(
              child: DriverSkeletonBlock(width: 180, height: 11),
            ),
          ],
          const SizedBox(height: 18),
          Divider(color: scheme.outlineVariant.withValues(alpha: 0.55)),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: busy || offer.isExpired ? null : onReject,
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size(0, 50),
                    shape: RoundedRectangleBorder(
                      borderRadius: AppRadii.button,
                    ),
                  ),
                  child: const Text('Decline'),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: FilledButton.icon(
                  onPressed: busy || offer.isExpired || !offer.hasTripDetails
                      ? null
                      : onAccept,
                  style: FilledButton.styleFrom(
                    backgroundColor: scheme.primary,
                    minimumSize: const Size(0, 50),
                    shape: RoundedRectangleBorder(
                      borderRadius: AppRadii.button,
                    ),
                  ),
                  icon: busy
                      ? SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: scheme.onSurfaceVariant,
                          ),
                        )
                      : const AppIcon(SwiftIcons.check, size: 18),
                  label: const Text('Accept ride'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

/// Only the expiry badge updates every second; trip details remain stable.
class _OfferCountdown extends StatefulWidget {
  final PendingOfferModel offer;
  const _OfferCountdown({required this.offer});

  @override
  State<_OfferCountdown> createState() => _OfferCountdownState();
}

class _OfferCountdownState extends State<_OfferCountdown> {
  Timer? _timer;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _restartTimer();
  }

  @override
  void didUpdateWidget(covariant _OfferCountdown oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.offer != widget.offer) _restartTimer();
  }

  void _restartTimer() {
    _timer?.cancel();
    if (!TickerMode.valuesOf(context).enabled ||
        widget.offer.secondsRemaining == null ||
        widget.offer.isExpired) {
      return;
    }
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted) return;
      setState(() {});
      if (widget.offer.isExpired) _timer?.cancel();
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final remaining = widget.offer.secondsRemaining;
    final window =
        widget.offer.expiresAt != null && widget.offer.serverTime != null
        ? widget.offer.expiresAt!.difference(widget.offer.serverTime!).inSeconds
        : null;
    final label = remaining == null ? 'Soon' : '${remaining.clamp(0, 999)}s';
    return Semantics(
      label: 'Request expires',
      value: remaining == null ? 'Soon' : '$remaining seconds',
      child: SizedBox(
        width:
            52 +
            12 * (MediaQuery.textScalerOf(context).scale(1) - 1).clamp(0, 2),
        height:
            52 +
            12 * (MediaQuery.textScalerOf(context).scale(1) - 1).clamp(0, 2),
        child: Stack(
          alignment: Alignment.center,
          children: [
            CircularProgressIndicator(
              value: remaining == null || window == null || window <= 0
                  ? 1
                  : (remaining / window).clamp(0.0, 1.0),
              strokeWidth: 2,
              color: scheme.primary,
              backgroundColor: scheme.primary.withValues(alpha: 0.12),
            ),
            ExcludeSemantics(
              child: Text(
                label,
                style: Theme.of(
                  context,
                ).textTheme.labelMedium?.copyWith(fontWeight: FontWeight.w600),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _OfferInfoChip extends StatelessWidget {
  final SwiftIcons icon;
  final String label;

  const _OfferInfoChip({required this.icon, required this.label});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
      decoration: BoxDecoration(
        color: scheme.primary.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          AppIcon(icon, color: scheme.primary, size: 15),
          const SizedBox(width: 5),
          Flexible(
            child: Text(label, style: Theme.of(context).textTheme.labelSmall),
          ),
        ],
      ),
    );
  }
}
