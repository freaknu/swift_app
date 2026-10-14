import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/rider/home/domain/entity/ride_history_address.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:flutter/material.dart';
import 'package:geocoding/geocoding.dart';

class RideHistoryCard extends StatefulWidget {
  final RideHistoryAddress ride;

  const RideHistoryCard({super.key, required this.ride});

  @override
  State<RideHistoryCard> createState() => _RideHistoryCardState();
}

class _RideHistoryCardState extends State<RideHistoryCard> {
  late final Geocoding _geocoding = Geocoding(locale: const Locale('en', 'IN'));

  static final Map<String, String> _addressCache = {};

  String? _destinationPlace;

  int _request = 0;

  @override
  void initState() {
    super.initState();
    _resolveDestination();
  }

  @override
  void didUpdateWidget(covariant RideHistoryCard oldWidget) {
    super.didUpdateWidget(oldWidget);

    if (oldWidget.ride.destinationLatitude != widget.ride.destinationLatitude ||
        oldWidget.ride.destinationLongitude !=
            widget.ride.destinationLongitude) {
      _destinationPlace = null;

      _resolveDestination();
    }
  }

  Future<void> _resolveDestination() async {
    final double? latitude = widget.ride.destinationLatitude;

    final double? longitude = widget.ride.destinationLongitude;

    final int request = ++_request;

    if (latitude == null || longitude == null) {
      if (mounted) {
        setState(() {
          _destinationPlace = 'Destination unavailable';
        });
      }
      return;
    }

    final String key = '$latitude,$longitude';

    if (_addressCache.containsKey(key)) {
      setState(() {
        _destinationPlace = _addressCache[key];
      });
      return;
    }

    String address =
        '${latitude.toStringAsFixed(4)}, '
        '${longitude.toStringAsFixed(4)}';

    try {
      final places = await _geocoding
          .placemarkFromCoordinates(latitude, longitude)
          .timeout(const Duration(seconds: 8));

      if (places.isNotEmpty) {
        final place = places.first;

        final parts =
            [place.name, place.street, place.subLocality, place.locality]
                .whereType<String>()
                .map((part) => part.trim())
                .where((part) => part.isNotEmpty)
                .toSet();

        if (parts.isNotEmpty) {
          address = parts.join(', ');
        }

        if (_addressCache.length >= 100) {
          _addressCache.remove(_addressCache.keys.first);
        }

        _addressCache[key] = address;
      }
    } catch (_) {}

    if (!mounted || request != _request) {
      return;
    }

    setState(() {
      _destinationPlace = address;
    });
  }

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);

    final bool isDark = theme.brightness == Brightness.dark;

    return AppCard(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 12),
      boxShadow: const [],

      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,

            decoration: BoxDecoration(
              color: theme.colorScheme.primary.withValues(
                alpha: isDark ? 0.14 : 0.08,
              ),

              borderRadius: BorderRadius.circular(13),
            ),

            child: AppIcon(
              SwiftIcons.map,
              size: 18,
              color: theme.colorScheme.primary,
            ),
          ),

          const SizedBox(width: 12),

          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,

              children: [
                if (_destinationPlace == null)
                  const AppShimmer(child: ShimmerBlock(width: 160, height: 14))
                else
                  Text(
                    _destinationPlace!,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.titleSmall?.copyWith(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                    ),
                  ),

                const SizedBox(height: 2),

                Text(
                  widget.ride.rideId == null
                      ? 'Past ride'
                      : 'Ride #${widget.ride.rideId}',

                  style: theme.textTheme.bodySmall?.copyWith(fontSize: 10.5),
                ),
              ],
            ),
          ),

          AppIcon(
            SwiftIcons.chevronRight,
            size: 15,
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ],
      ),
    );
  }
}
