import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_response.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class OfferCard extends StatelessWidget {
  final OfferResponse offer;

  const OfferCard({super.key, required this.offer});

  String _number(double value) {
    return value == value.roundToDouble()
        ? value.toStringAsFixed(0)
        : value.toStringAsFixed(2);
  }

  String _date(DateTime date) {
    const months = [
      'Jan',
      'Feb',
      'Mar',
      'Apr',
      'May',
      'Jun',
      'Jul',
      'Aug',
      'Sep',
      'Oct',
      'Nov',
      'Dec',
    ];

    return '${date.day} '
        '${months[date.month - 1]} '
        '${date.year}';
  }

  String get _discountText {
    final value = offer.discountValue;

    if (value == null) {
      return 'Ride offer';
    }

    final String number = _number(value);

    return switch (offer.discountType?.toUpperCase()) {
      'PERCENTAGE' || 'PERCENT' => '$number% off',
      'FIXED_AMOUNT' || 'FIXED' || 'FLAT' => '₹$number off',
      _ => 'Ride offer',
    };
  }

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);

    final bool isDark = theme.brightness == Brightness.dark;

    final String? code = offer.offerCode?.trim();

    return AppCard(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      boxShadow: const [],

      child: Column(
        mainAxisSize: MainAxisSize.min,

        crossAxisAlignment: CrossAxisAlignment.start,

        children: [
          Row(
            children: [
              Container(
                width: 42,
                height: 42,

                decoration: BoxDecoration(
                  color: theme.colorScheme.primary.withValues(
                    alpha: isDark ? 0.15 : 0.08,
                  ),

                  borderRadius: BorderRadius.circular(13),
                ),

                child: AppIcon(
                  SwiftIcons.tag,
                  color: theme.colorScheme.primary,
                  size: 20,
                ),
              ),

              const SizedBox(width: 11),

              Expanded(
                child: Text(
                  _discountText,

                  style: theme.textTheme.titleLarge?.copyWith(
                    color: theme.colorScheme.primary,

                    fontSize: 21,

                    fontWeight: FontWeight.w700,

                    letterSpacing: -0.5,
                  ),
                ),
              ),
            ],
          ),

          if (offer.title?.trim().isNotEmpty == true) ...[
            const SizedBox(height: 10),

            Text(
              offer.title!,

              style: theme.textTheme.titleSmall?.copyWith(
                fontSize: 13.5,

                fontWeight: FontWeight.w600,
              ),
            ),
          ],

          if (offer.description?.trim().isNotEmpty == true) ...[
            const SizedBox(height: 3),

            Text(
              offer.description!,

              maxLines: 2,

              overflow: TextOverflow.ellipsis,

              style: theme.textTheme.bodySmall?.copyWith(
                fontSize: 11,
                height: 1.4,
              ),
            ),
          ],

          if (offer.maximumDiscount != null || offer.minimumFare != null) ...[
            const SizedBox(height: 9),

            Text(
              [
                if (offer.maximumDiscount != null)
                  'Up to ₹${_number(offer.maximumDiscount!)}',

                if (offer.minimumFare != null)
                  'Min. ₹${_number(offer.minimumFare!)}',
              ].join(' · '),

              style: theme.textTheme.bodySmall?.copyWith(fontSize: 10.5),
            ),
          ],

          if (offer.validUntil != null) ...[
            const SizedBox(height: 5),

            Row(
              children: [
                AppIcon(
                  SwiftIcons.clock3,
                  size: 18,
                  color: theme.colorScheme.onSurfaceVariant,
                ),

                const SizedBox(width: 5),

                Text(
                  'Until ${_date(offer.validUntil!.toLocal())}',

                  style: theme.textTheme.bodySmall?.copyWith(fontSize: 10.5),
                ),
              ],
            ),
          ],

          if (code != null && code.isNotEmpty) ...[
            const SizedBox(height: 13),

            CupertinoButton(
              padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 8),

              minimumSize: const Size(44, 44),

              color: theme.colorScheme.primary.withValues(
                alpha: isDark ? 0.15 : 0.09,
              ),

              borderRadius: BorderRadius.circular(13),

              onPressed: () async {
                await Clipboard.setData(ClipboardData(text: code));

                if (!context.mounted) {
                  return;
                }

                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    behavior: SnackBarBehavior.floating,
                    content: const Text('Offer code copied'),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(14),
                    ),
                  ),
                );
              },

              child: Row(
                mainAxisSize: MainAxisSize.min,

                children: [
                  const AppIcon(SwiftIcons.copy, size: 18),

                  const SizedBox(width: 6),

                  Text(
                    code,

                    style: const TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}
