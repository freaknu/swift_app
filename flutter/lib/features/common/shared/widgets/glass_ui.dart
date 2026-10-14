import 'dart:ui';

import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:swift_app/themes/app_shadows.dart';
import 'package:swift_app/themes/app_spacing.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

/// A clean surface with an optional filter for specialized overlays.
///
/// Blur is opt-in, never the default for scrolling cards or controls placed
/// over a native map. When blur is disabled, a translucent color is composited
/// once against the page background instead of allocating a filter layer.
class GlassSurface extends StatelessWidget {
  final Widget child;
  final BorderRadius borderRadius;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final double? width;
  final double? height;
  final double blur;
  final double opacity;
  final Color? tint;
  final Border? border;
  final List<BoxShadow>? boxShadow;
  final Clip clipBehavior;
  final String? semanticLabel;

  const GlassSurface({
    super.key,
    required this.child,
    this.borderRadius = AppRadii.cardRadius,
    this.padding,
    this.margin,
    this.width,
    this.height,
    this.blur = 0,
    this.opacity = 1,
    this.tint,
    this.border,
    this.boxShadow,
    this.clipBehavior = Clip.antiAlias,
    this.semanticLabel,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final highContrast = MediaQuery.highContrastOf(context);
    final useBlur = blur > 0 && !highContrast;
    final resolvedOpacity = opacity.clamp(0.0, 1.0).toDouble();

    final baseTint = tint ?? theme.colorScheme.surface;
    final tintAlpha = (baseTint.a * resolvedOpacity).clamp(0.0, 1.0).toDouble();
    final translucentTint = baseTint.withValues(alpha: tintAlpha);
    final fillColor = useBlur
        ? translucentTint
        : Color.alphaBlend(translucentTint, theme.scaffoldBackgroundColor);
    final defaultBorder = theme.colorScheme.outlineVariant;

    final surface = DecoratedBox(
      decoration: BoxDecoration(
        color: fillColor,
        borderRadius: borderRadius,
        border: border ?? Border.all(color: defaultBorder),
      ),
      child: Padding(padding: padding ?? EdgeInsets.zero, child: child),
    );

    final clippedSurface = ClipRRect(
      borderRadius: borderRadius,
      clipBehavior: clipBehavior,
      child: useBlur
          ? BackdropFilter(
              filter: ImageFilter.blur(sigmaX: blur, sigmaY: blur),
              child: surface,
            )
          : surface,
    );

    Widget result = Padding(
      padding: margin ?? EdgeInsets.zero,
      child: SizedBox(
        width: width,
        height: height,
        child: DecoratedBox(
          decoration: BoxDecoration(
            borderRadius: borderRadius,
            boxShadow: boxShadow ?? AppShadows.none,
          ),
          child: clippedSurface,
        ),
      ),
    );

    if (semanticLabel != null && semanticLabel!.isNotEmpty) {
      result = Semantics(container: true, label: semanticLabel, child: result);
    }
    return result;
  }
}

/// General-purpose name for a glass surface when the widget is used as a
/// layout primitive rather than a visual card.
class GlassContainer extends GlassSurface {
  const GlassContainer({
    super.key,
    required super.child,
    super.borderRadius,
    super.padding,
    super.margin,
    super.width,
    super.height,
    super.blur,
    super.opacity,
    super.tint,
    super.border,
    super.boxShadow,
    super.clipBehavior,
    super.semanticLabel,
  });
}

/// Low-level glass card primitive with optional press handling.
///
/// New feature cards should generally use `AppCard`, which keeps the common
/// card contract in one named component while this remains reusable by the
/// shared glass primitives.
class GlassCard extends StatelessWidget {
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

  const GlassCard({
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

  @override
  Widget build(BuildContext context) {
    if (onTap == null) {
      return GlassContainer(
        width: width,
        height: height,
        margin: margin,
        padding: padding,
        borderRadius: borderRadius,
        blur: blur,
        opacity: opacity,
        tint: tint,
        border: border,
        boxShadow: boxShadow,
        semanticLabel: semanticLabel,
        clipBehavior: clipBehavior,
        child: child,
      );
    }

    final interactive = GlassContainer(
      width: width,
      height: height,
      margin: margin,
      padding: EdgeInsets.zero,
      borderRadius: borderRadius,
      blur: blur,
      opacity: opacity,
      tint: tint,
      border: border,
      boxShadow: boxShadow,
      clipBehavior: clipBehavior,
      child: Material(
        type: MaterialType.transparency,
        child: InkWell(
          onTap: onTap,
          borderRadius: borderRadius,
          child: Padding(padding: padding ?? EdgeInsets.zero, child: child),
        ),
      ),
    );

    return Semantics(
      button: true,
      enabled: true,
      label: semanticLabel,
      child: interactive,
    );
  }
}

/// A bottom-sheet surface with an optional, decorative grab handle.
class GlassBottomSheet extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry padding;
  final BorderRadius borderRadius;
  final double blur;
  final double opacity;
  final Color? tint;
  final Border? border;
  final List<BoxShadow>? boxShadow;
  final bool showHandle;
  final String? semanticLabel;

  const GlassBottomSheet({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.fromLTRB(
      AppSpacing.lg,
      AppSpacing.sm,
      AppSpacing.lg,
      AppSpacing.xl,
    ),
    this.borderRadius = AppRadii.sheetRadius,
    this.blur = 0,
    this.opacity = 1,
    this.tint,
    this.border,
    this.boxShadow,
    this.showHandle = true,
    this.semanticLabel,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final content = Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        if (showHandle) ...[
          Semantics(
            excludeSemantics: true,
            child: Container(
              width: 36,
              height: 4,
              decoration: BoxDecoration(
                color: theme.colorScheme.onSurface.withValues(alpha: 0.2),
                borderRadius: BorderRadius.circular(999),
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.md),
        ],
        child,
      ],
    );

    return GlassContainer(
      padding: padding,
      borderRadius: borderRadius,
      blur: blur,
      opacity: opacity,
      tint: tint,
      border: border,
      boxShadow: boxShadow,
      semanticLabel: semanticLabel,
      child: content,
    );
  }
}

/// A reusable shell for a bottom navigation control.
///
/// The navigation implementation stays with the caller (Cupertino or
/// Material), while this primitive standardizes its surface, safe area, and
/// accessibility label.
class GlassNavigationBar extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry margin;
  final EdgeInsetsGeometry padding;
  final BorderRadius borderRadius;
  final double blur;
  final double opacity;
  final Color? tint;
  final Border? border;
  final List<BoxShadow>? boxShadow;
  final bool useSafeArea;
  final String? semanticLabel;

  const GlassNavigationBar({
    super.key,
    required this.child,
    this.margin = EdgeInsets.zero,
    this.padding = EdgeInsets.zero,
    this.borderRadius = const BorderRadius.vertical(top: Radius.circular(26)),
    this.blur = 0,
    this.opacity = 1,
    this.tint,
    this.border,
    this.boxShadow,
    this.useSafeArea = true,
    this.semanticLabel,
  });

  @override
  Widget build(BuildContext context) {
    return GlassContainer(
      margin: margin,
      padding: padding,
      borderRadius: borderRadius,
      blur: blur,
      opacity: opacity,
      tint: tint,
      border: border,
      boxShadow: boxShadow,
      semanticLabel: semanticLabel ?? 'Navigation bar',
      child: useSafeArea ? SafeArea(top: false, child: child) : child,
    );
  }
}

/// A compact glass button for actions that do not need a screen-specific
/// button widget.
class GlassButton extends StatelessWidget {
  final String? label;
  final Widget? child;
  final Widget? icon;
  final VoidCallback? onPressed;
  final String? semanticLabel;
  final EdgeInsetsGeometry padding;
  final BorderRadius borderRadius;
  final double minHeight;
  final double blur;
  final bool filled;
  final double? width;

  const GlassButton({
    super.key,
    this.label,
    this.child,
    this.icon,
    required this.onPressed,
    this.semanticLabel,
    this.padding = AppSpacing.control,
    this.borderRadius = AppRadii.button,
    this.minHeight = AppSpacing.controlHeight,
    this.blur = 0,
    this.filled = false,
    this.width,
  }) : assert(label != null || child != null, 'Provide label or child');

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final foreground = filled
        ? theme.colorScheme.onPrimary
        : theme.colorScheme.onSurface;
    final content =
        child ??
        Row(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (icon != null) ...[
              IconTheme(
                data: IconThemeData(color: foreground),
                child: icon!,
              ),
              if (label != null) const SizedBox(width: AppSpacing.sm),
            ],
            Flexible(
              child: Text(
                label ?? '',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.labelLarge?.copyWith(
                  color: foreground,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        );

    return Semantics(
      button: true,
      enabled: onPressed != null,
      label: semanticLabel ?? label,
      child: GlassContainer(
        width: width,
        borderRadius: borderRadius,
        padding: EdgeInsets.zero,
        blur: blur,
        opacity: filled ? 0.96 : 0.84,
        tint: filled ? theme.colorScheme.primary : null,
        child: SizedBox(
          height: minHeight,
          child: Material(
            type: MaterialType.transparency,
            child: InkWell(
              onTap: onPressed,
              borderRadius: borderRadius,
              child: Padding(
                padding: padding,
                child: Center(child: content),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// One solid page color shared by rider, driver and account screens.
class AppGlassBackground extends StatelessWidget {
  final Widget child;
  final bool includeBottomGlow;

  const AppGlassBackground({
    super.key,
    required this.child,
    this.includeBottomGlow = true,
  });

  @override
  Widget build(BuildContext context) => ColoredBox(
    color: Theme.of(context).scaffoldBackgroundColor,
    child: child,
  );
}

class GlassIconButton extends StatelessWidget {
  final SwiftIcons icon;
  final VoidCallback? onPressed;
  final String tooltip;
  final String? semanticLabel;
  final Color? iconColor;
  final double size;
  final double iconSize;

  const GlassIconButton({
    super.key,
    required this.icon,
    required this.onPressed,
    required this.tooltip,
    this.semanticLabel,
    this.iconColor,
    this.size = 44,
    this.iconSize = 20,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final targetSize = size < 44 ? 44.0 : size;
    final label = semanticLabel ?? tooltip;
    return Semantics(
      button: true,
      enabled: onPressed != null,
      label: label,
      child: Tooltip(
        message: tooltip,
        child: CupertinoButton(
          padding: EdgeInsets.zero,
          minimumSize: Size(targetSize, targetSize),
          onPressed: onPressed,
          child: GlassContainer(
            width: targetSize,
            height: targetSize,
            padding: EdgeInsets.zero,
            borderRadius: BorderRadius.circular(targetSize / 2),
            boxShadow: AppShadows.none,
            child: ExcludeSemantics(
              child: AppIcon(
                icon,
                size: iconSize,
                color: onPressed == null
                    ? theme.disabledColor
                    : iconColor ?? theme.colorScheme.onSurface,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
