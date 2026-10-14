import 'package:flutter/material.dart';

/// Locally bundled category photography; the catalog's exact code still owns
/// selection and matching. Photos illustrate a category rather than a make.
class VehiclePhoto extends StatelessWidget {
  const VehiclePhoto({
    super.key,
    required this.code,
    this.width,
    this.height,
    this.fit = BoxFit.contain,
  });

  final String code;
  final double? width;
  final double? height;
  final BoxFit fit;

  static String assetFor(String code) {
    final category = code.trim().toUpperCase();
    final name = switch (category) {
      'AUTO' || 'AUTO_RICKSHAW' => 'auto',
      'SCOOTER' || 'ELECTRIC_SCOOTER' => 'scooter',
      'BIKE' || 'ELECTRIC_BIKE' || 'DELIVERY_BIKE' || 'MOTORCYCLE' => 'bike',
      'VAN' || 'DELIVERY' || 'DELIVERY_VAN' || 'MINIVAN' => 'van',
      'SUV' || 'PREMIUM_SUV' || 'LUXURY_SUV' => 'suv',
      _ => 'sedan',
    };
    return 'assets/images/vehicles/$name.webp';
  }

  @override
  Widget build(BuildContext context) => LayoutBuilder(
    builder: (context, constraints) {
      final logicalWidth =
          width ?? (constraints.hasBoundedWidth ? constraints.maxWidth : 128);
      final decodeWidth =
          (logicalWidth * MediaQuery.devicePixelRatioOf(context)).ceil().clamp(
            64,
            640,
          );
      return RepaintBoundary(
        child: Image.asset(
          assetFor(code),
          width: width,
          height: height,
          fit: fit,
          cacheWidth: decodeWidth,
          filterQuality: FilterQuality.medium,
          excludeFromSemantics: true,
          errorBuilder: (context, error, stackTrace) => SizedBox(
            width: width,
            height: height,
            child: Center(
              child: Text(
                'Photo unavailable',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.labelSmall,
              ),
            ),
          ),
        ),
      );
    },
  );
}
