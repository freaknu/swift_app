import 'package:flutter/material.dart';

/// Shared spacing values used across the Swift rider and driver experiences.
///
/// Keeping layout values here makes it possible to tune density without
/// changing individual screens. Values are deliberately based on a small
/// eight-point scale with a few useful in-between steps.
class AppSpacing {
  const AppSpacing._();

  static const double hairline = 0.5;
  static const double xxs = 4;
  static const double xs = 8;
  static const double sm = 12;
  static const double md = 16;
  static const double lg = 20;
  static const double xl = 24;
  static const double xxl = 32;
  static const double xxxl = 40;

  static const double compactControlHeight = 48;
  static const double controlHeight = 52;
  static const double largeControlHeight = 56;
  static const double outlinedButtonHeight = 54;

  /// Screen gutters stay compact so content uses the available width without
  /// making cards feel edge-to-edge.
  static const EdgeInsets page = EdgeInsets.fromLTRB(md, sm, md, xxl);
  static const EdgeInsets card = EdgeInsets.all(md);
  static const EdgeInsets compactCard = EdgeInsets.symmetric(
    horizontal: sm,
    vertical: sm,
  );
  static const EdgeInsets input = EdgeInsets.symmetric(
    horizontal: md,
    vertical: md,
  );
  static const EdgeInsets control = EdgeInsets.symmetric(
    horizontal: lg,
    vertical: 13,
  );
  static const EdgeInsets primaryButton = EdgeInsets.symmetric(
    horizontal: lg,
    vertical: md,
  );
  static const EdgeInsets compactControl = EdgeInsets.symmetric(
    horizontal: md,
    vertical: sm,
  );
  static const EdgeInsets outlinedControl = EdgeInsets.symmetric(
    horizontal: 18,
    vertical: 13,
  );
  static const EdgeInsets outlinedButton = EdgeInsets.symmetric(
    horizontal: 18,
    vertical: sm,
  );
}
