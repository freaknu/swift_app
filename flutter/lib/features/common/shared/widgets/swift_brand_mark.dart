import 'package:flutter/material.dart';

/// The same Swift artwork used by the installed app's launcher icon.
class SwiftBrandMark extends StatelessWidget {
  const SwiftBrandMark({
    super.key,
    this.size = 30,
    this.borderRadius = 9,
    this.excludeFromSemantics = false,
  }) : assert(size > 0),
       assert(borderRadius >= 0);

  static const assetPath = 'assets/branding/swift-app-icon.png';

  final double size;
  final double borderRadius;

  /// Use when adjacent visible text already announces the Swift brand.
  final bool excludeFromSemantics;

  @override
  Widget build(BuildContext context) {
    final decodeSize = (size * MediaQuery.devicePixelRatioOf(context)).ceil();
    return ClipRRect(
      borderRadius: BorderRadius.circular(borderRadius),
      child: Image.asset(
        assetPath,
        width: size,
        height: size,
        cacheWidth: decodeSize,
        cacheHeight: decodeSize,
        fit: BoxFit.cover,
        filterQuality: FilterQuality.high,
        semanticLabel: 'Swift app icon',
        excludeFromSemantics: excludeFromSemantics,
      ),
    );
  }
}
