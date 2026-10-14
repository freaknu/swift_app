import 'dart:async';

import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/themes/app_spacing.dart';
import 'package:flutter/material.dart';
import 'package:geocoding/geocoding.dart';

String? _formatCoordinates(double? latitude, double? longitude) {
  if (latitude == null || longitude == null) return null;
  return '${latitude.toStringAsFixed(5)}, ${longitude.toStringAsFixed(5)}';
}

/// Resolves a short place label once per point, with coordinates immediately
/// available while the platform geocoder is loading or unavailable.
class DriverAddressPoint extends StatefulWidget {
  final SwiftIcons icon;
  final String label;
  final double? latitude;
  final double? longitude;
  final bool isFirst;

  const DriverAddressPoint({
    super.key,
    required this.icon,
    required this.label,
    required this.latitude,
    required this.longitude,
    required this.isFirst,
  });

  @override
  State<DriverAddressPoint> createState() => _DriverAddressPointState();
}

class _DriverAddressPointState extends State<DriverAddressPoint> {
  static final Map<String, String> _addressCache = {};
  String? _address;

  String? get _pointKey {
    final latitude = widget.latitude;
    final longitude = widget.longitude;
    if (latitude == null || longitude == null) return null;
    return '${latitude.toStringAsFixed(5)},${longitude.toStringAsFixed(5)}';
  }

  @override
  void initState() {
    super.initState();
    _resolve();
  }

  @override
  void didUpdateWidget(covariant DriverAddressPoint oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.latitude != widget.latitude ||
        oldWidget.longitude != widget.longitude) {
      _address = null;
      _resolve();
    }
  }

  Future<void> _resolve() async {
    final key = _pointKey;
    final latitude = widget.latitude;
    final longitude = widget.longitude;
    if (key == null || latitude == null || longitude == null) return;
    final cached = _addressCache[key];
    if (cached != null) {
      _address = cached;
      return;
    }
    try {
      final places = await Geocoding().placemarkFromCoordinates(
        latitude,
        longitude,
      );
      if (places.isEmpty) return;
      final place = places.first;
      final parts = <String>[];
      for (final value in [place.street, place.subLocality, place.locality]) {
        final part = value?.trim();
        if (part != null && part.isNotEmpty && !parts.contains(part)) {
          parts.add(part);
        }
        if (parts.length == 2) break;
      }
      if (parts.isEmpty && place.name?.trim().isNotEmpty == true) {
        parts.add(place.name!.trim());
      }
      if (parts.isEmpty) return;
      final address = parts.join(', ');
      if (_addressCache.length >= 100) {
        _addressCache.remove(_addressCache.keys.first);
      }
      _addressCache[key] = address;
      if (mounted && _pointKey == key) {
        setState(() => _address = address);
      }
    } catch (_) {
      // A route remains usable when reverse geocoding is unavailable.
    }
  }

  @override
  Widget build(BuildContext context) {
    return _RoutePoint(
      icon: widget.icon,
      label: widget.label,
      value:
          _address ??
          _formatCoordinates(widget.latitude, widget.longitude) ??
          '${widget.label} details unavailable',
      isFirst: widget.isFirst,
    );
  }
}

class _RoutePoint extends StatelessWidget {
  final SwiftIcons icon;
  final String label;
  final String value;
  final bool isFirst;

  const _RoutePoint({
    required this.icon,
    required this.label,
    required this.value,
    required this.isFirst,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return Semantics(
      container: true,
      label: '$label. $value',
      child: Padding(
        padding: EdgeInsets.only(bottom: isFirst ? 8 : 0),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              width: 30,
              child: Column(
                children: [
                  Container(
                    width: 24,
                    height: 24,
                    decoration: BoxDecoration(
                      color: scheme.primary.withValues(alpha: 0.1),
                      shape: BoxShape.circle,
                    ),
                    child: AppIcon(icon, size: 12, color: scheme.primary),
                  ),
                  if (isFirst)
                    Container(
                      width: 1,
                      height: 20,
                      margin: const EdgeInsets.only(top: 3),
                      color: scheme.primary.withValues(alpha: 0.22),
                    ),
                ],
              ),
            ),
            const SizedBox(width: AppSpacing.sm),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.only(top: 1),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      label,
                      style: theme.textTheme.labelSmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      value,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
