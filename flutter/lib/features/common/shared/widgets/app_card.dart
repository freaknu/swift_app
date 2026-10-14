import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:swift_app/themes/app_shadows.dart';
import 'package:swift_app/themes/app_spacing.dart';
import 'package:flutter/material.dart';

/// Standard content card. Shape, padding and press feedback share one source.
/// Use [AppCard.compact] for rows and [AppCard.flush] for maps or custom layouts.
class AppCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final double? width;
  final double? height;
  final BorderRadius borderRadius;
  final double blur;
  final double opacity;
  final Color? tint;
  final Border? border;
  final List<BoxShadow>? boxShadow;
  final VoidCallback? onTap;
  final String? semanticLabel;
  final Clip clipBehavior;

  const AppCard({
    super.key,
    required this.child,
    this.padding = AppSpacing.card,
    this.margin,
    this.width,
    this.height,
    this.borderRadius = AppRadii.cardRadius,
    this.blur = 0,
    this.opacity = 1,
    this.tint,
    this.border,
    this.boxShadow,
    this.onTap,
    this.semanticLabel,
    this.clipBehavior = Clip.antiAlias,
  });

  const AppCard.compact({
    super.key,
    required this.child,
    this.margin,
    this.width,
    this.height,
    this.opacity = 1,
    this.tint,
    this.border,
    this.onTap,
    this.semanticLabel,
    this.clipBehavior = Clip.antiAlias,
  }) : padding = AppSpacing.compactCard,
       borderRadius = AppRadii.cardRadius,
       blur = 0,
       boxShadow = AppShadows.none;

  const AppCard.flush({
    super.key,
    required this.child,
    this.margin,
    this.width,
    this.height,
    this.borderRadius = AppRadii.cardRadius,
    this.opacity = 1,
    this.tint,
    this.border,
    this.boxShadow,
    this.onTap,
    this.semanticLabel,
    this.clipBehavior = Clip.antiAlias,
  }) : padding = EdgeInsets.zero,
       blur = 0;

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      padding: padding,
      margin: margin,
      width: width,
      height: height,
      borderRadius: borderRadius,
      blur: blur,
      opacity: opacity,
      tint: tint,
      border: border,
      boxShadow: boxShadow,
      onTap: onTap,
      semanticLabel: semanticLabel,
      clipBehavior: clipBehavior,
      child: child,
    );
  }
}

/// Shared selectable card for vehicles and offers. A constant border width
/// keeps the contents still when selection changes.
class AppSelectableCard extends StatelessWidget {
  final Widget child;
  final bool selected;
  final VoidCallback? onTap;
  final EdgeInsetsGeometry padding;
  final Color? backgroundColor;
  final String? semanticLabel;

  const AppSelectableCard({
    super.key,
    required this.child,
    required this.selected,
    this.onTap,
    this.padding = AppSpacing.compactCard,
    this.backgroundColor,
    this.semanticLabel,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Semantics(
      selected: selected,
      child: AppCard(
        onTap: onTap,
        semanticLabel: semanticLabel,
        padding: padding,
        tint: selected
            ? scheme.primary.withValues(alpha: 0.08)
            : backgroundColor,
        border: Border.all(
          color: selected ? scheme.primary : scheme.outlineVariant,
          width: 1.25,
        ),
        boxShadow: AppShadows.none,
        child: child,
      ),
    );
  }
}
