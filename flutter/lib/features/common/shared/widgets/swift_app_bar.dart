import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:swift_app/features/common/shared/theme_toggle_button.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';

/// A quiet, notch-safe header shared by full pages and map overlays.
/// Inline callers use primary:false inside one SafeArea; Scaffold callers let
/// the AppBar own the status-bar inset. Titles get their own scalable toolbar.
class SwiftAppBar extends StatelessWidget implements PreferredSizeWidget {
  const SwiftAppBar({
    super.key,
    required this.title,
    this.subtitle,
    this.leading,
    this.actions,
    this.bottom,
    this.flexibleSpace,
    this.toolbarHeight,
    this.leadingWidth,
    this.titleSpacing,
    this.backgroundColor,
    this.foregroundColor,
    this.systemOverlayStyle,
    this.automaticallyImplyLeading = true,
    this.centerTitle = false,
    this.primary = true,
  });

  final Widget title;
  final String? subtitle;
  final Widget? leading;
  final List<Widget>? actions;
  final PreferredSizeWidget? bottom;
  final Widget? flexibleSpace;
  final double? toolbarHeight;
  final double? leadingWidth;
  final double? titleSpacing;
  final Color? backgroundColor;
  final Color? foregroundColor;
  final SystemUiOverlayStyle? systemOverlayStyle;
  final bool automaticallyImplyLeading;
  final bool centerTitle;
  final bool primary;

  static const BorderRadius bottomRadius = BorderRadius.vertical(
    bottom: Radius.circular(18),
  );

  /// Reserve actual scaled line heights rather than squeezing accessibility
  /// text into a fixed 56px toolbar. The status-bar inset is added by AppBar.
  static double toolbarHeightFor(
    BuildContext context, {
    bool hasSubtitle = false,
  }) {
    final scaler = MediaQuery.textScalerOf(context);
    final titleHeight = scaler.scale(19) * 1.3;
    final subtitleHeight = hasSubtitle ? scaler.scale(12) * 1.4 + 3 : 0;
    return math.max(hasSubtitle ? 76 : 64, titleHeight + subtitleHeight + 20);
  }

  @override
  Size get preferredSize => Size.fromHeight(
    (toolbarHeight ?? (subtitle == null ? 64 : 76)) +
        (bottom?.preferredSize.height ?? 0),
  );

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final background = backgroundColor ?? theme.scaffoldBackgroundColor;
    final foreground = foregroundColor ?? scheme.onSurface;
    final darkSurface =
        ThemeData.estimateBrightnessForColor(background) == Brightness.dark;
    final overlay =
        systemOverlayStyle ??
        SystemUiOverlayStyle(
          statusBarColor: Colors.transparent,
          statusBarIconBrightness: darkSurface
              ? Brightness.light
              : Brightness.dark,
          statusBarBrightness: darkSurface ? Brightness.dark : Brightness.light,
        );
    final route = ModalRoute.of(context);
    final scaffold = Scaffold.maybeOf(context);
    Widget? resolvedLeading = leading;
    if (resolvedLeading == null && automaticallyImplyLeading) {
      if (scaffold?.hasDrawer == true) {
        resolvedLeading = SwiftAppBarAction(
          icon: SwiftIcons.menu,
          tooltip: MaterialLocalizations.of(context).openAppDrawerTooltip,
          onPressed: scaffold!.openDrawer,
        );
      } else if (route?.impliesAppBarDismissal == true) {
        final close = route is PageRoute && route.fullscreenDialog;
        resolvedLeading = SwiftAppBarAction(
          icon: close ? SwiftIcons.x : SwiftIcons.chevronLeft,
          tooltip: close
              ? MaterialLocalizations.of(context).closeButtonTooltip
              : MaterialLocalizations.of(context).backButtonTooltip,
          onPressed: () => Navigator.of(context).maybePop(),
        );
      }
    }
    final pageActions =
        actions ??
        (scaffold?.hasEndDrawer == true
            ? [
                SwiftAppBarAction(
                  icon: SwiftIcons.menu,
                  tooltip: MaterialLocalizations.of(
                    context,
                  ).openAppDrawerTooltip,
                  onPressed: scaffold!.openEndDrawer,
                ),
              ]
            : null);
    final resolvedActions = <Widget>[
      ...?pageActions,
      const ThemeToggleButton(),
    ];
    // A bounded box also fixes inline bars in an unconstrained Column/ListView.
    // No ClipRRect surrounds the status bar or top of the title.
    return SizedBox(
      height:
          preferredSize.height +
          (primary ? MediaQuery.paddingOf(context).top : 0),
      child: AppBar(
        primary: primary,
        automaticallyImplyLeading: false,
        leading: resolvedLeading,
        leadingWidth: leadingWidth ?? 56,
        titleSpacing: titleSpacing ?? (resolvedLeading == null ? 16 : 4),
        title: DefaultTextStyle.merge(
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          child: subtitle == null
              ? title
              : Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: centerTitle
                      ? CrossAxisAlignment.center
                      : CrossAxisAlignment.start,
                  children: [
                    title,
                    const SizedBox(height: 3),
                    Text(
                      subtitle!,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: foreground.withValues(alpha: 0.65),
                        fontSize: 12,
                        height: 1.4,
                        fontWeight: FontWeight.w400,
                      ),
                    ),
                  ],
                ),
        ),
        titleTextStyle: theme.textTheme.titleLarge?.copyWith(
          color: foreground,
          fontSize: 19,
          height: 1.3,
          fontWeight: FontWeight.w600,
          letterSpacing: -0.35,
        ),
        toolbarHeight: toolbarHeight ?? (subtitle == null ? 64 : 76),
        actions: resolvedActions,
        actionsPadding: const EdgeInsetsDirectional.only(end: 8),
        bottom: bottom,
        flexibleSpace: flexibleSpace,
        centerTitle: centerTitle,
        backgroundColor: background,
        foregroundColor: foreground,
        iconTheme: IconThemeData(color: foreground, size: 19),
        actionsIconTheme: IconThemeData(color: foreground, size: 19),
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        shadowColor: Colors.transparent,
        shape: const RoundedRectangleBorder(borderRadius: bottomRadius),
        systemOverlayStyle: overlay,
      ),
    );
  }
}

/// Small SVG strokes with a full-size, accessible action target.
class SwiftAppBarAction extends StatelessWidget {
  const SwiftAppBarAction({
    super.key,
    required this.icon,
    required this.tooltip,
    required this.onPressed,
    this.color,
  });

  final SwiftIcons icon;
  final String tooltip;
  final VoidCallback? onPressed;
  final Color? color;

  @override
  Widget build(BuildContext context) => IconButton(
    tooltip: tooltip,
    onPressed: onPressed,
    style: IconButton.styleFrom(
      minimumSize: const Size(48, 48),
      padding: const EdgeInsets.all(14),
      foregroundColor: color ?? IconTheme.of(context).color,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
    ),
    icon: AppIcon(
      icon,
      size: 19,
      color: color,
      matchTextDirection: icon == SwiftIcons.chevronLeft,
    ),
  );
}
