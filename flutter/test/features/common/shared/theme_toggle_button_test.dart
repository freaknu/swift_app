import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';
import 'package:swift_app/features/common/shared/theme_toggle_button.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/auth_frame.dart';
import 'package:swift_app/features/common/shared/widgets/auth_scaffold.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/common/shared/widgets/swift_brand_mark.dart';
import 'package:swift_app/features/common/splash/welcome/presentation/pages/welcome_page.dart';
import 'package:swift_app/themes/app_theme.dart';
import 'package:swift_app/themes/theme_controller.dart';

void main() {
  setUp(() => Get.testMode = true);
  tearDown(() async => Get.reset());

  testWidgets(
    'header switch updates every route and preserves actions on a narrow page',
    (tester) async {
      tester.view.physicalSize = const Size(320, 740);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final controller = Get.put(ThemeController()..setLightMode());
      var notificationsOpened = 0;

      await tester.pumpWidget(
        _ThemedApp(
          controller: controller,
          textScale: 2.8,
          home: Builder(
            builder: (context) => Scaffold(
              appBar: SwiftAppBar(
                toolbarHeight: SwiftAppBar.toolbarHeightFor(context),
                title: const SwiftBrand(onBar: true),
                actions: [
                  SwiftAppBarAction(
                    icon: SwiftIcons.bell,
                    tooltip: 'Notifications',
                    onPressed: () => notificationsOpened++,
                  ),
                  SwiftAppBarAction(
                    icon: SwiftIcons.userRound,
                    tooltip: 'Account',
                    onPressed: () => Navigator.of(context).push(
                      MaterialPageRoute<void>(
                        builder: (context) => Scaffold(
                          appBar: SwiftAppBar(
                            toolbarHeight: SwiftAppBar.toolbarHeightFor(
                              context,
                            ),
                            title: const Text('Account'),
                          ),
                          body: const _BrightnessLabel('account'),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              body: const _BrightnessLabel('home'),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byType(ThemeToggleButton), findsOneWidget);
      expect(find.byType(SwiftBrandMark), findsOneWidget);
      expect(find.text('home light'), findsOneWidget);
      final toggle = tester.getSize(find.byType(ThemeToggleButton));
      expect(toggle.width, greaterThanOrEqualTo(48));
      expect(toggle.height, greaterThanOrEqualTo(48));
      await tester.tap(find.byTooltip('Notifications'));
      expect(notificationsOpened, 1);

      await tester.tap(find.byTooltip('Switch to dark mode'));
      await tester.pumpAndSettle();
      expect(controller.themeMode, ThemeMode.dark);
      expect(find.text('home dark'), findsOneWidget);
      expect(find.byTooltip('Switch to light mode'), findsOneWidget);
      expect(
        find.byWidgetPredicate(
          (widget) => widget is AppIcon && widget.icon == SwiftIcons.sun,
        ),
        findsOneWidget,
      );

      await tester.tap(find.byTooltip('Account'));
      await tester.pumpAndSettle();
      expect(find.text('account dark'), findsOneWidget);
      expect(find.byType(ThemeToggleButton), findsOneWidget);
      await tester.tap(find.byTooltip('Switch to light mode'));
      await tester.pumpAndSettle();
      expect(find.text('account light'), findsOneWidget);
      await tester.tap(find.byTooltip('Back'));
      await tester.pumpAndSettle();
      expect(find.text('home light'), findsOneWidget);
      expect(find.byTooltip('Notifications'), findsOneWidget);
      expect(find.byTooltip('Account'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('system dark appearance switches to a manual light preference', (
    tester,
  ) async {
    tester.platformDispatcher.platformBrightnessTestValue = Brightness.dark;
    addTearDown(tester.platformDispatcher.clearPlatformBrightnessTestValue);
    final controller = Get.put(ThemeController());
    await tester.pumpWidget(
      _ThemedApp(
        controller: controller,
        home: const Scaffold(
          appBar: SwiftAppBar(title: Text('Notifications')),
          body: _BrightnessLabel('page'),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('page dark'), findsOneWidget);
    await tester.tap(find.byTooltip('Switch to light mode'));
    await tester.pumpAndSettle();
    expect(controller.themeMode, ThemeMode.light);
    expect(find.text('page light'), findsOneWidget);
    expect(find.byTooltip('Switch to dark mode'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('welcome and authentication headers each have one switch', (
    tester,
  ) async {
    final controller = Get.put(ThemeController()..setLightMode());
    for (final page in [
      const WelcomePage(),
      const AuthScaffold(
        leading: SizedBox.shrink(),
        child: Text('Sign in form'),
      ),
    ]) {
      await tester.pumpWidget(_ThemedApp(controller: controller, home: page));
      await tester.pumpAndSettle();
      expect(find.byType(ThemeToggleButton), findsOneWidget);
      expect(find.byTooltip('Switch to dark mode'), findsOneWidget);
      expect(tester.takeException(), isNull);
    }
  });
}

class _ThemedApp extends StatelessWidget {
  const _ThemedApp({
    required this.controller,
    required this.home,
    this.textScale = 1,
  });

  final ThemeController controller;
  final Widget home;
  final double textScale;

  @override
  Widget build(BuildContext context) => Obx(
    () => MaterialApp(
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: controller.themeMode,
      builder: (context, child) => MediaQuery(
        data: MediaQuery.of(
          context,
        ).copyWith(textScaler: TextScaler.linear(textScale)),
        child: child!,
      ),
      home: home,
    ),
  );
}

class _BrightnessLabel extends StatelessWidget {
  const _BrightnessLabel(this.page);

  final String page;

  @override
  Widget build(BuildContext context) =>
      Text('$page ${Theme.of(context).brightness.name}');
}
