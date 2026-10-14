import 'package:flutter/material.dart';

/// Small, static shadows used by shared surfaces.
///
/// These avoid large blur radii and are safe to use around scrolling content.
class AppShadows {
  const AppShadows._();

  static const List<BoxShadow> none = <BoxShadow>[];

  static const List<BoxShadow> subtle = <BoxShadow>[
    BoxShadow(color: Color(0x0A000000), blurRadius: 12, offset: Offset(0, 4)),
  ];

  static const List<BoxShadow> glass = <BoxShadow>[
    BoxShadow(color: Color(0x0D000000), blurRadius: 16, offset: Offset(0, 5)),
  ];

  static const List<BoxShadow> glassDark = <BoxShadow>[
    BoxShadow(color: Color(0x33000000), blurRadius: 18, offset: Offset(0, 6)),
  ];

  static List<BoxShadow> glassFor(bool isDark) => isDark ? glassDark : glass;
}
