import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/app_loading_page.dart';
import 'package:swift_app/features/common/shared/widgets/swift_brand_mark.dart';
import 'package:swift_app/themes/app_theme.dart';

void main() {
  testWidgets('startup gate keeps home mounted and blocks covered input', (
    tester,
  ) async {
    final ready = ValueNotifier(false);
    addTearDown(ready.dispose);
    var mounts = 0;
    var actions = 0;
    await tester.pumpWidget(
      _gateApp(
        ready: ready,
        child: _HomeProbe(onMounted: () => mounts++, onAction: () => actions++),
      ),
    );
    expect(find.byType(AppLoadingPage), findsOneWidget);
    expect(find.text('Swift'), findsOneWidget);
    expect(mounts, 1);
    expect(tester.getSize(find.byType(_HomeProbe)), const Size(800, 600));
    await tester.tap(find.text('Ride home action'), warnIfMissed: false);
    expect(actions, 0);

    ready.value = true;
    await tester.pumpAndSettle();
    expect(find.byType(AppLoadingPage), findsNothing);
    expect(mounts, 1);
    await tester.tap(find.text('Ride home action'));
    expect(actions, 1);
    expect(tester.takeException(), isNull);
  });

  testWidgets('startup completion latches and refresh cannot reopen it', (
    tester,
  ) async {
    final ready = ValueNotifier(false);
    addTearDown(ready.dispose);
    await tester.pumpWidget(
      _gateApp(ready: ready, child: const Text('Ready home')),
    );
    expect(find.byType(AppLoadingPage), findsOneWidget);
    ready.value = true;
    await tester.pumpAndSettle();
    expect(find.byType(AppLoadingPage), findsNothing);
    ready.value = false;
    await tester.pump();
    expect(find.byType(AppLoadingPage), findsNothing);
    expect(find.text('Ready home'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('an already ready session opens its home immediately', (
    tester,
  ) async {
    final ready = ValueNotifier(true);
    addTearDown(ready.dispose);
    await tester.pumpWidget(
      _gateApp(ready: ready, child: const Text('Ready home')),
    );
    expect(find.byType(AppLoadingPage), findsNothing);
    expect(find.text('Ready home'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'loading page supports small notched screens and reduced motion',
    (tester) async {
      tester.view.physicalSize = const Size(320, 280);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      for (final theme in [AppTheme.light(), AppTheme.dark()]) {
        await tester.pumpWidget(
          MaterialApp(
            theme: theme,
            home: Builder(
              builder: (context) => MediaQuery(
                data: MediaQuery.of(context).copyWith(
                  disableAnimations: true,
                  textScaler: TextScaler.linear(2.4),
                  padding: const EdgeInsets.only(top: 44, bottom: 34),
                ),
                child: const AppLoadingPage(),
              ),
            ),
          ),
        );
        await tester.pumpAndSettle();
        expect(find.text('Loading your data and map…'), findsOneWidget);
        expect(find.byType(CircularProgressIndicator), findsNothing);
        expect(
          find.byWidgetPredicate(
            (widget) =>
                widget is AppIcon && widget.icon == SwiftIcons.loaderCircle,
          ),
          findsOneWidget,
        );
        await tester.ensureVisible(find.byType(AppLoadingProgress));
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets('normal loading progress is indeterminate and accessible', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    await tester.pumpWidget(
      const MaterialApp(home: AppLoadingPage(message: 'Preparing Swift')),
    );
    expect(find.text('Preparing Swift'), findsOneWidget);
    final brandImageFinder = find.descendant(
      of: find.byType(SwiftBrandMark),
      matching: find.byType(Image),
    );
    final brandImage = tester.widget<Image>(brandImageFinder);
    await tester.runAsync(
      () => precacheImage(brandImage.image, tester.element(brandImageFinder)),
    );
    // The visible Swift title names the brand without a duplicate icon label.
    expect(brandImage.excludeFromSemantics, isTrue);
    expect(find.bySemanticsLabel('Swift app icon'), findsNothing);
    expect(find.bySemanticsLabel('Loading'), findsOneWidget);
    expect(
      tester
          .widget<CircularProgressIndicator>(
            find.byType(CircularProgressIndicator),
          )
          .value,
      isNull,
    );
    await tester.pump(const Duration(milliseconds: 200));
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox.shrink());
    semantics.dispose();
  });

  testWidgets('covered home semantics remain hidden until ready', (
    tester,
  ) async {
    final semantics = tester.ensureSemantics();
    final ready = ValueNotifier(false);
    addTearDown(ready.dispose);
    await tester.pumpWidget(
      _gateApp(
        ready: ready,
        child: Semantics(
          label: 'Rider home content',
          child: const Text('Ready home'),
        ),
      ),
    );
    expect(find.bySemanticsLabel('Rider home content'), findsNothing);
    expect(find.bySemanticsLabel('Loading your data and map…'), findsOneWidget);
    ready.value = true;
    await tester.pumpAndSettle();
    expect(find.bySemanticsLabel(RegExp('Rider home content')), findsOneWidget);
    expect(tester.takeException(), isNull);
    semantics.dispose();
  });
}

Widget _gateApp({required ValueNotifier<bool> ready, required Widget child}) =>
    MaterialApp(
      theme: AppTheme.light(),
      home: Builder(
        builder: (context) => MediaQuery(
          data: MediaQuery.of(context).copyWith(disableAnimations: true),
          child: ValueListenableBuilder<bool>(
            valueListenable: ready,
            builder: (context, isReady, _) =>
                AppStartupGate(ready: isReady, child: child),
          ),
        ),
      ),
    );

class _HomeProbe extends StatefulWidget {
  const _HomeProbe({required this.onMounted, required this.onAction});
  final VoidCallback onMounted;
  final VoidCallback onAction;

  @override
  State<_HomeProbe> createState() => _HomeProbeState();
}

class _HomeProbeState extends State<_HomeProbe> {
  @override
  void initState() {
    super.initState();
    widget.onMounted();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    body: Center(
      child: TextButton(
        onPressed: widget.onAction,
        child: const Text('Ride home action'),
      ),
    ),
  );
}
