import 'package:swift_app/features/rider/home/domain/entity/ride_type.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/vehicle_photo.dart';
import 'package:flutter/material.dart';

class RideTypeCard extends StatelessWidget {
  final RideType rideType;
  final bool selected;
  final VoidCallback? onTap;

  const RideTypeCard({
    super.key,
    required this.rideType,
    this.selected = false,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);

    final bool isDark = theme.brightness == Brightness.dark;

    final Color primary = theme.colorScheme.primary;

    return Semantics(
      selected: selected,
      button: true,
      label: rideType.maxPassengers > 0
          ? '${rideType.name}, up to ${rideType.maxPassengers} passengers'
          : rideType.name,

      child: AnimatedScale(
        duration: const Duration(milliseconds: 190),
        scale: selected ? 1 : 0.985,

        child: Material(
          color: Colors.transparent,

          child: InkWell(
            borderRadius: AppRadii.cardRadius,
            onTap: onTap,

            child: AnimatedContainer(
              duration: const Duration(milliseconds: 240),
              curve: Curves.easeOutCubic,

              padding: const EdgeInsets.all(12),

              decoration: BoxDecoration(
                borderRadius: AppRadii.cardRadius,

                color: selected
                    ? primary.withValues(alpha: isDark ? 0.16 : 0.08)
                    : theme.colorScheme.surface,

                border: Border.all(
                  color: selected
                      ? primary.withValues(alpha: 0.80)
                      : theme.colorScheme.onSurface.withValues(alpha: 0.07),
                  width: selected ? 1.35 : 1,
                ),
              ),

              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,

                children: [
                  Row(
                    children: [
                      Expanded(
                        child: VehiclePhoto(code: rideType.code, height: 46),
                      ),

                      AnimatedSwitcher(
                        duration: const Duration(milliseconds: 180),
                        child: selected
                            ? AppIcon(
                                SwiftIcons.circleCheck,
                                key: const ValueKey(true),
                                size: 19,
                                color: primary,
                              )
                            : const SizedBox(
                                key: ValueKey(false),
                                width: 19,
                                height: 19,
                              ),
                      ),
                    ],
                  ),

                  const SizedBox(height: 8),

                  Text(
                    rideType.name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.titleSmall?.copyWith(
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                      letterSpacing: -0.2,
                    ),
                  ),

                  if (rideType.description.trim().isNotEmpty) ...[
                    const SizedBox(height: 2),
                    Text(
                      rideType.description,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodySmall?.copyWith(
                        fontSize: 10.5,
                      ),
                    ),
                  ],

                  const Spacer(),

                  if (rideType.maxPassengers > 0 ||
                      rideType.cashSupported ||
                      rideType.onlinePaymentSupported)
                    Row(
                      children: [
                        if (rideType.maxPassengers > 0)
                          AppIcon(
                            SwiftIcons.userRound,
                            size: 14,
                            color: theme.colorScheme.onSurfaceVariant,
                          ),

                        if (rideType.maxPassengers > 0)
                          const SizedBox(width: 4),

                        if (rideType.maxPassengers > 0)
                          Text(
                            '${rideType.maxPassengers}',
                            style: theme.textTheme.labelSmall,
                          ),

                        const Spacer(),

                        if (rideType.cashSupported)
                          AppIcon(
                            SwiftIcons.banknote,
                            size: 15,
                            color: theme.colorScheme.onSurfaceVariant,
                          ),

                        if (rideType.onlinePaymentSupported) ...[
                          const SizedBox(width: 6),
                          AppIcon(
                            SwiftIcons.creditCard,
                            size: 15,
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ],
                      ],
                    ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
