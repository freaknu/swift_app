import 'dart:math' as math;

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';

class SwiftNavigationDestination {
  final SwiftIcons icon;
  final String label;

  const SwiftNavigationDestination({required this.icon, required this.label});
}

/// Shared rider and driver navigation on a crisp, solid rounded surface.
class SwiftBottomNavigationBar extends StatelessWidget {
  final int selectedIndex;
  final ValueChanged<int> onDestinationSelected;
  final List<SwiftNavigationDestination> destinations;

  const SwiftBottomNavigationBar({
    super.key,
    required this.selectedIndex,
    required this.onDestinationSelected,
    required this.destinations,
  }) : assert(destinations.length >= 2),
       assert(selectedIndex >= 0 && selectedIndex < destinations.length);

  static const BorderRadius topRadius = BorderRadius.vertical(
    top: Radius.circular(26),
  );

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final highContrast = MediaQuery.highContrastOf(context);
    final labelHeight = MediaQuery.textScalerOf(context).scale(11) * 1.2;
    final rowHeight = math.max(56.0, 20 + 5 + labelHeight + 16);

    return GlassSurface(
      borderRadius: topRadius,
      blur: 0,
      opacity: 1,
      tint: scheme.surface,
      border: Border.all(
        color: scheme.outlineVariant.withValues(alpha: highContrast ? 1 : 0.7),
        width: highContrast ? 1 : 0.7,
      ),
      boxShadow: const [],
      semanticLabel: 'Main navigation',
      // Keep the home-indicator inset inside the solid surface.
      child: SafeArea(
        top: false,
        child: SizedBox(
          height: rowHeight,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            child: Row(
              children: [
                for (var index = 0; index < destinations.length; index++)
                  Expanded(
                    child: _NavigationButton(
                      destination: destinations[index],
                      selected: index == selectedIndex,
                      height: rowHeight,
                      color: index == selectedIndex
                          ? scheme.primary
                          : highContrast
                          ? scheme.onSurface
                          : scheme.onSurfaceVariant,
                      onPressed: () => onDestinationSelected(index),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _NavigationButton extends StatelessWidget {
  final SwiftNavigationDestination destination;
  final bool selected;
  final double height;
  final Color color;
  final VoidCallback onPressed;

  const _NavigationButton({
    required this.destination,
    required this.selected,
    required this.height,
    required this.color,
    required this.onPressed,
  });

  @override
  Widget build(BuildContext context) => Semantics(
    button: true,
    selected: selected,
    label: destination.label,
    onTap: onPressed,
    child: ExcludeSemantics(
      child: CupertinoButton(
        padding: const EdgeInsets.symmetric(horizontal: 2),
        minimumSize: Size(44, height),
        borderRadius: BorderRadius.circular(12),
        onPressed: onPressed,
        child: AnimatedContainer(
          duration: MediaQuery.disableAnimationsOf(context)
              ? Duration.zero
              : const Duration(milliseconds: 160),
          curve: Curves.easeOutCubic,
          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 6),
          decoration: BoxDecoration(
            color: selected
                ? color.withValues(alpha: 0.075)
                : color.withValues(alpha: 0),
            borderRadius: BorderRadius.circular(13),
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            mainAxisSize: MainAxisSize.min,
            children: [
              AppIcon(destination.icon, size: 20, color: color),
              const SizedBox(height: 5),
              Text(
                destination.label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: color,
                  fontSize: 11,
                  height: 1.2,
                  letterSpacing: 0,
                  fontWeight: selected ? FontWeight.w500 : FontWeight.w400,
                ),
              ),
            ],
          ),
        ),
      ),
    ),
  );
}
