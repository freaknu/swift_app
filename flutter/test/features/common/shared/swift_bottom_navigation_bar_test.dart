import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/swift_bottom_navigation_bar.dart';

const _riderDestinations = [
  SwiftNavigationDestination(icon: SwiftIcons.navigation, label: 'Ride'),
  SwiftNavigationDestination(icon: SwiftIcons.bookmark, label: 'Saved'),
  SwiftNavigationDestination(icon: SwiftIcons.clock3, label: 'Activity'),
  SwiftNavigationDestination(icon: SwiftIcons.tag, label: 'Offers'),
  SwiftNavigationDestination(icon: SwiftIcons.userRound, label: 'Account'),
];

class _NavigationHarness extends StatefulWidget {
  final Brightness brightness;
  final bool highContrast;
  final double textScale;
  final List<SwiftNavigationDestination> destinations;

  const _NavigationHarness({
    this.brightness = Brightness.light,
    this.highContrast = false,
    this.textScale = 1,
    this.destinations = _riderDestinations,
  });

  @override
  State<_NavigationHarness> createState() => _NavigationHarnessState();
}

class _NavigationHarnessState extends State<_NavigationHarness> {
  int index = 0;

  @override
  Widget build(BuildContext context) => MaterialApp(
    theme: ThemeData(
      colorScheme: ColorScheme.fromSeed(
        seedColor: Colors.teal,
        brightness: widget.brightness,
      ),
    ),
    builder: (context, child) => MediaQuery(
      data: MediaQuery.of(context).copyWith(
        padding: const EdgeInsets.only(bottom: 34),
        viewPadding: const EdgeInsets.only(bottom: 34),
        highContrast: widget.highContrast,
        textScaler: TextScaler.linear(widget.textScale),
      ),
      child: child!,
    ),
    home: Scaffold(
      extendBody: true,
      body: const ColoredBox(color: Colors.teal, child: SizedBox.expand()),
      bottomNavigationBar: SwiftBottomNavigationBar(
        selectedIndex: index,
        destinations: widget.destinations,
        onDestinationSelected: (value) => setState(() => index = value),
      ),
    ),
  );
}

void main() {
  for (final brightness in Brightness.values) {
    testWidgets('solid navigation fills the bottom inset in $brightness', (
      tester,
    ) async {
      await tester.pumpWidget(_NavigationHarness(brightness: brightness));
      await tester.pumpAndSettle();
      final surface = tester.widget<GlassSurface>(find.byType(GlassSurface));
      expect(surface.borderRadius.topLeft, const Radius.circular(26));
      expect(surface.borderRadius.topRight, const Radius.circular(26));
      expect(surface.borderRadius.bottomLeft, Radius.zero);
      expect(surface.opacity, 1);
      expect(surface.blur, 0);
      expect(find.byType(BackdropFilter), findsNothing);
      expect(
        tester.getSize(find.byType(SwiftBottomNavigationBar)).height,
        closeTo(90, 0.01),
      );
      final icons = tester.widgetList<AppIcon>(find.byType(AppIcon)).toList();
      expect(icons, hasLength(5));
      expect(icons.every((icon) => icon.size == 20), isTrue);
      final scheme = Theme.of(
        tester.element(find.byType(AppIcon).first),
      ).colorScheme;
      expect(icons.first.color, scheme.primary);
      expect(icons.last.color, scheme.onSurfaceVariant);
      expect(tester.takeException(), isNull);
    });
  }

  testWidgets('five tabs remain reachable at narrow width with large text', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(320, 740);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    final semantics = tester.ensureSemantics();
    try {
      await tester.pumpWidget(const _NavigationHarness(textScale: 2));
      await tester.pumpAndSettle();
      for (final destination in _riderDestinations) {
        final target = find.bySemanticsLabel(destination.label);
        final size = tester.getSize(target);
        expect(size.width, greaterThanOrEqualTo(44));
        expect(size.height, greaterThanOrEqualTo(44));
        await tester.tap(find.text(destination.label));
        await tester.pumpAndSettle();
        final data = tester.getSemantics(target).getSemanticsData();
        expect(data.flagsCollection.isButton, isTrue);
        expect(data.flagsCollection.isSelected.toBoolOrNull(), isTrue);
        expect(tester.takeException(), isNull);
      }
    } finally {
      semantics.dispose();
    }
  });

  testWidgets('high contrast driver navigation is opaque and skips blur', (
    tester,
  ) async {
    await tester.pumpWidget(
      const _NavigationHarness(
        highContrast: true,
        brightness: Brightness.dark,
        destinations: [
          SwiftNavigationDestination(icon: SwiftIcons.car, label: 'Drive'),
          SwiftNavigationDestination(
            icon: SwiftIcons.receiptText,
            label: 'Trips',
          ),
          SwiftNavigationDestination(
            icon: SwiftIcons.userRound,
            label: 'Account',
          ),
        ],
      ),
    );
    await tester.pumpAndSettle();
    expect(tester.widget<GlassSurface>(find.byType(GlassSurface)).opacity, 1);
    expect(find.byType(BackdropFilter), findsNothing);
    await tester.tap(find.text('Trips'));
    await tester.pumpAndSettle();
    expect(
      tester
          .widget<SwiftBottomNavigationBar>(
            find.byType(SwiftBottomNavigationBar),
          )
          .selectedIndex,
      1,
    );
    final accountIcon = tester.widgetList<AppIcon>(find.byType(AppIcon)).last;
    final scheme = Theme.of(
      tester.element(find.byType(AppIcon).first),
    ).colorScheme;
    expect(accountIcon.color, scheme.onSurface);
    expect(tester.takeException(), isNull);
  });
}
