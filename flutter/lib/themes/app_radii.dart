import 'package:flutter/material.dart';

/// Shared corner radii for controls, cards, and sheets.
class AppRadii {
  const AppRadii._();

  static const double sm = 8;
  static const double control = 12;
  static const double textButton = 10;
  static const double iconButton = 12;
  static const double card = 16;
  static const double dialog = 20;
  static const double sheet = 22;
  static const double hero = 22;

  static const BorderRadius small = BorderRadius.all(Radius.circular(sm));
  static const BorderRadius button = BorderRadius.all(Radius.circular(control));
  static const BorderRadius textButtonRadius = BorderRadius.all(
    Radius.circular(textButton),
  );
  static const BorderRadius iconButtonRadius = BorderRadius.all(
    Radius.circular(iconButton),
  );
  static const BorderRadius cardRadius = BorderRadius.all(
    Radius.circular(card),
  );
  static const BorderRadius dialogRadius = BorderRadius.all(
    Radius.circular(dialog),
  );
  static const BorderRadius sheetRadius = BorderRadius.vertical(
    top: Radius.circular(sheet),
  );
  static const BorderRadius heroRadius = BorderRadius.all(
    Radius.circular(hero),
  );
}

/// Friendly token names for feature code that only needs the core radius
/// scale. [AppRadii] remains available for the more explicit variants above.
class AppRadius {
  const AppRadius._();

  static const double small = AppRadii.sm;
  static const double medium = AppRadii.control;
  static const double large = AppRadii.card;
  static const double xLarge = AppRadii.hero;
  static const double pill = 999;
}
