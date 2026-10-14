import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/features/common/shared/widgets/auth_frame.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/themes/app_theme.dart';

void main() {
  testWidgets(
    'shared bar keeps a restrained surface and readable dark/light title',
    (tester) async {
      for (final theme in [AppTheme.light(), AppTheme.dark()]) {
        await tester.pumpWidget(
          MaterialApp(
            theme: theme,
            home: const Scaffold(
              appBar: SwiftAppBar(title: Text('Notifications')),
              body: SizedBox(),
            ),
          ),
        );
        await tester.pumpAndSettle();

        final bar = tester.widget<AppBar>(find.byType(AppBar));
        final shape = bar.shape! as RoundedRectangleBorder;
        expect(shape.borderRadius, SwiftAppBar.bottomRadius);
        expect(bar.foregroundColor, theme.colorScheme.onSurface);
        expect(bar.backgroundColor, theme.scaffoldBackgroundColor);
        expect(find.text('Notifications'), findsOneWidget);
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets('bottom tabs contribute height and remain interactive', (
    tester,
  ) async {
    const bar = SwiftAppBar(
      title: Text('Profile'),
      toolbarHeight: 72,
      bottom: TabBar(
        tabs: [
          Tab(text: 'Vehicle'),
          Tab(text: 'Documents'),
        ],
      ),
    );
    expect(bar.preferredSize.height, 72 + kTextTabBarHeight);

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: const DefaultTabController(
          length: 2,
          child: Scaffold(
            appBar: bar,
            body: TabBarView(
              children: [Text('Vehicle form'), Text('Documents form')],
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('Documents'));
    await tester.pumpAndSettle();
    expect(find.text('Documents form'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('inline map bar fits a narrow page and preserves both actions', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(320, 640);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });
    var backCount = 0;
    var actionCount = 0;

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: SafeArea(
            child: Column(
              children: [
                SwiftAppBar(
                  primary: false,
                  toolbarHeight: 60,
                  automaticallyImplyLeading: false,
                  leading: IconButton(
                    tooltip: 'Back',
                    onPressed: () => backCount++,
                    icon: const AppIcon(SwiftIcons.chevronLeft, size: 19),
                  ),
                  title: const Text(
                    'Set destination point on map',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  actions: [
                    IconButton(
                      tooltip: 'Fit route',
                      onPressed: () => actionCount++,
                      icon: const AppIcon(SwiftIcons.locateFixed, size: 19),
                    ),
                  ],
                ),
                const Expanded(child: Text('Map stays visible')),
              ],
            ),
          ),
        ),
      ),
    );

    expect(tester.getSize(find.byType(SwiftAppBar)).height, 60);
    expect(find.text('Map stays visible'), findsOneWidget);
    await tester.tap(find.byTooltip('Back'));
    await tester.tap(find.byTooltip('Fit route'));
    expect(backCount, 1);
    expect(actionCount, 1);
    expect(tester.takeException(), isNull);
  });

  testWidgets('authentication screen uses shared bar and keeps back action', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Builder(
          builder: (context) => Scaffold(
            body: Center(
              child: FilledButton(
                onPressed: () => Navigator.of(context).push(
                  MaterialPageRoute<void>(
                    builder: (_) => const AuthFrame(
                      header: Text('Passwordless sign in'),
                      child: Text('Request a code'),
                    ),
                  ),
                ),
                child: const Text('Open sign in'),
              ),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('Open sign in'));
    await tester.pumpAndSettle();
    expect(find.byType(SwiftAppBar), findsOneWidget);
    expect(find.text('Passwordless sign in'), findsOneWidget);
    expect(
      find.byWidgetPredicate(
        (widget) => widget is AppIcon && widget.icon == SwiftIcons.chevronLeft,
      ),
      findsOneWidget,
    );

    await tester.tap(find.byTooltip('Back'));
    await tester.pumpAndSettle();
    expect(find.text('Open sign in'), findsOneWidget);
  });

  testWidgets(
    'notched header reserves status inset and scaled title/subtitle',
    (tester) async {
      tester.view.physicalSize = const Size(320, 844);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTheme.light(),
          builder: (context, child) => MediaQuery(
            data: MediaQuery.of(context).copyWith(
              padding: const EdgeInsets.only(top: 44, bottom: 34),
              viewPadding: const EdgeInsets.only(top: 44, bottom: 34),
              textScaler: TextScaler.linear(2.4),
            ),
            child: child!,
          ),
          home: Builder(
            builder: (context) => Scaffold(
              appBar: SwiftAppBar(
                toolbarHeight: SwiftAppBar.toolbarHeightFor(
                  context,
                  hasSubtitle: true,
                ),
                title: const Text('Trip receipt'),
                subtitle: 'Payment confirmed',
                automaticallyImplyLeading: false,
                leading: SwiftAppBarAction(
                  icon: SwiftIcons.chevronLeft,
                  tooltip: 'Back',
                  onPressed: () {},
                ),
              ),
              body: const Text('Receipt content'),
            ),
          ),
        ),
      );
      final title = tester.getRect(find.text('Trip receipt'));
      final subtitle = tester.getRect(find.text('Payment confirmed'));
      final bar = tester.getRect(find.byType(SwiftAppBar));
      expect(title.top, greaterThanOrEqualTo(44));
      expect(title.bottom, lessThanOrEqualTo(subtitle.top));
      expect(subtitle.bottom, lessThanOrEqualTo(bar.bottom));
      expect(
        tester.getRect(find.text('Receipt content')).top,
        greaterThanOrEqualTo(bar.bottom),
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('automatic drawer uses bundled SVG and preserves menu action', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: const Scaffold(
          appBar: SwiftAppBar(title: Text('Swift')),
          drawer: Drawer(child: Text('Your menu')),
        ),
      ),
    );
    expect(
      find.byWidgetPredicate(
        (widget) => widget is AppIcon && widget.icon == SwiftIcons.menu,
      ),
      findsOneWidget,
    );
    expect(find.byType(BackButton), findsNothing);
    await tester.tap(find.byType(SwiftAppBarAction));
    await tester.pumpAndSettle();
    expect(find.text('Your menu'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
