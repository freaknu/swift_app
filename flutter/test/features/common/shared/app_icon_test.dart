import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/themes/app_theme.dart';

void main() {
  testWidgets('SVG artwork stays small inside a larger circular action', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: const Scaffold(
          body: Center(
            child: SizedBox.square(
              dimension: 60,
              child: AppIcon(SwiftIcons.car, size: 20),
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.getSize(find.byType(AppIcon)), const Size(60, 60));
    expect(tester.getSize(find.byType(SvgPicture)), const Size(20, 20));
    expect(
      tester.getCenter(find.byType(SvgPicture)),
      tester.getCenter(find.byType(AppIcon)),
    );
    expect(tester.takeException(), isNull);
  });

  testWidgets('all bundled icon assets load and render without a network', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.dark(),
        home: Scaffold(
          body: Wrap(
            children: [
              for (final icon in SwiftIcons.values)
                Padding(
                  padding: const EdgeInsets.all(8),
                  child: AppIcon(icon, size: 20),
                ),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(SvgPicture), findsNWidgets(SwiftIcons.values.length));
    expect(tester.takeException(), isNull);
  });
}
