import 'dart:convert';

import 'package:swift_app/features/rider/home/presentation/widgets/google_map_style.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('Google map style follows the app brightness', () {
    expect(AppGoogleMapStyle.forBrightness(Brightness.light), isNull);
    expect(AppGoogleMapStyle.forBrightness(Brightness.dark), isNotNull);
  });

  test('dark Google map style is valid JSON', () {
    final decoded = jsonDecode(AppGoogleMapStyle.dark);

    expect(decoded, isA<List<dynamic>>());
    expect(decoded, isNotEmpty);
  });
}
