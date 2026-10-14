import 'package:flutter/material.dart';

class ScreenSizes {
  final BuildContext context;

  ScreenSizes(this.context);

  Size get size => MediaQuery.sizeOf(context);

  double get width => size.width;

  double get height => size.height;
}
