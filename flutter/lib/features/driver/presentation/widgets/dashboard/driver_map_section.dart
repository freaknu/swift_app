import 'package:swift_app/features/rider/home/presentation/widgets/google_map_widget.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:flutter/material.dart';

class DriverMapSection extends StatelessWidget {
  final bool active;
  final bool showLoadingIndicator;
  final VoidCallback onInitialLoadSettled;

  const DriverMapSection({
    super.key,
    required this.active,
    required this.showLoadingIndicator,
    required this.onInitialLoadSettled,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  'Your area',
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              if (active) ...[
                Container(
                  width: 6,
                  height: 6,
                  decoration: BoxDecoration(
                    color: scheme.primary,
                    shape: BoxShape.circle,
                  ),
                ),
                const SizedBox(width: 6),
                Text('Live', style: theme.textTheme.bodySmall),
              ],
            ],
          ),
          const SizedBox(height: 10),
          AppCard.flush(
            height: 240,
            opacity: 1,
            boxShadow: const [],
            semanticLabel: 'Map showing your current location',
            child: ClipRRect(
              borderRadius: AppRadii.cardRadius,
              child: GoogleMapWidget(
                active: active,
                showLoadingIndicator: showLoadingIndicator,
                onInitialLoadSettled: onInitialLoadSettled,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
