import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/lazy_tab_stack.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:swift_app/themes/custom_theme_data.dart';
import 'package:swift_app/themes/theme_controller.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('loading paints without rebuilding content or masking the card', (
    tester,
  ) async {
    var builds = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: Center(
            child: AppShimmer(
              child: Builder(
                builder: (_) {
                  builds++;
                  return const ShimmerBlock(width: 180, height: 24);
                },
              ),
            ),
          ),
        ),
      ),
    );
    final initialBuilds = builds;
    for (var frame = 0; frame < 20; frame++) {
      await tester.pump(const Duration(microseconds: 8333));
    }
    expect(builds, initialBuilds);
    expect(find.byType(ShaderMask), findsNothing);
    expect(tester.binding.transientCallbackCount, greaterThan(0));
    await tester.pumpWidget(const SizedBox.shrink());
  });

  testWidgets('loading stops its ticker for reduced motion and hidden tabs', (
    tester,
  ) async {
    final reduced = ValueNotifier(false);
    final visible = ValueNotifier(true);
    addTearDown(reduced.dispose);
    addTearDown(visible.dispose);
    await tester.pumpWidget(
      MaterialApp(
        home: ValueListenableBuilder<bool>(
          valueListenable: reduced,
          builder: (_, disableAnimations, _) => MediaQuery(
            data: MediaQueryData(disableAnimations: disableAnimations),
            child: ValueListenableBuilder<bool>(
              valueListenable: visible,
              builder: (_, enabled, _) => TickerMode(
                enabled: enabled,
                child: const Center(
                  child: AppShimmer(
                    child: ShimmerBlock(width: 100, height: 20),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
    await tester.pump(const Duration(milliseconds: 50));
    expect(tester.binding.transientCallbackCount, greaterThan(0));
    reduced.value = true;
    await tester.pumpAndSettle();
    expect(tester.binding.transientCallbackCount, 0);
    reduced.value = false;
    await tester.pump(const Duration(milliseconds: 50));
    expect(tester.binding.transientCallbackCount, greaterThan(0));
    visible.value = false;
    await tester.pumpAndSettle();
    expect(tester.binding.transientCallbackCount, 0);
    visible.value = true;
    await tester.pump(const Duration(milliseconds: 50));
    expect(tester.binding.transientCallbackCount, greaterThan(0));
    await tester.pumpWidget(const SizedBox.shrink());
  });

  testWidgets('tabs mount on demand, keep state and disable hidden tickers', (
    tester,
  ) async {
    final index = ValueNotifier(0);
    final mounts = <int>[];
    addTearDown(index.dispose);
    await tester.pumpWidget(
      MaterialApp(
        home: ValueListenableBuilder<int>(
          valueListenable: index,
          builder: (_, selected, _) => LazyTabStack(
            index: selected,
            builders: [
              for (var tab = 0; tab < 3; tab++)
                (_) => _TabProbe(tab: tab, onMount: mounts.add),
            ],
          ),
        ),
      ),
    );
    expect(mounts, [0]);
    await tester.tap(find.text('Tab 0: 0'));
    index.value = 1;
    await tester.pump();
    expect(mounts, [0, 1]);
    final hidden = tester.element(find.text('Tab 0: 1', skipOffstage: false));
    expect(TickerMode.valuesOf(hidden).enabled, isFalse);
    index.value = 0;
    await tester.pump();
    expect(find.text('Tab 0: 1'), findsOneWidget);
    expect(mounts, [0, 1]);
    expect(
      TickerMode.valuesOf(tester.element(find.text('Tab 0: 1'))).enabled,
      isTrue,
    );
  });

  testWidgets(
    'shared surfaces and backgrounds do not allocate blur layers by default',
    (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          theme: customDarkThemeData(),
          home: const Scaffold(
            body: AppGlassBackground(
              child: Center(child: GlassSurface(child: Text('Smooth surface'))),
            ),
          ),
        ),
      );
      expect(find.byType(BackdropFilter), findsNothing);
      expect(find.byType(ImageFiltered), findsNothing);
    },
  );

  testWidgets('high contrast disables explicitly requested blur', (
    tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: MediaQuery(
          data: MediaQueryData(highContrast: true),
          child: GlassSurface(blur: 20, child: Text('High contrast')),
        ),
      ),
    );
    expect(find.byType(BackdropFilter), findsNothing);
  });

  test('Apple theme uses system typography and no Material ink splash', () {
    debugDefaultTargetPlatformOverride = TargetPlatform.iOS;
    addTearDown(() => debugDefaultTargetPlatformOverride = null);
    final theme = customThemeData();
    expect(theme.platform, TargetPlatform.iOS);
    expect(theme.textTheme.bodyMedium!.fontFamily, isNot('Poppins'));
    expect(theme.splashFactory, NoSplash.splashFactory);
  });

  testWidgets(
    'system appearance remains live after returning from a manual override',
    (tester) async {
      final controller = ThemeController();
      expect(controller.themeMode, ThemeMode.system);
      tester.platformDispatcher.platformBrightnessTestValue = Brightness.dark;
      expect(controller.isDarkMode, isTrue);
      controller.setLightMode();
      expect(controller.isDarkMode, isFalse);
      controller.useSystemTheme();
      expect(controller.themeMode, ThemeMode.system);
      expect(controller.isDarkMode, isTrue);
      tester.platformDispatcher.platformBrightnessTestValue = Brightness.light;
      expect(controller.isDarkMode, isFalse);
      tester.platformDispatcher.clearPlatformBrightnessTestValue();
    },
  );
}

class _TabProbe extends StatefulWidget {
  final int tab;
  final ValueChanged<int> onMount;
  const _TabProbe({required this.tab, required this.onMount});

  @override
  State<_TabProbe> createState() => _TabProbeState();
}

class _TabProbeState extends State<_TabProbe> {
  int count = 0;
  @override
  void initState() {
    super.initState();
    widget.onMount(widget.tab);
  }

  @override
  Widget build(BuildContext context) => TextButton(
    onPressed: () => setState(() => count++),
    child: Text('Tab ${widget.tab}: $count'),
  );
}
