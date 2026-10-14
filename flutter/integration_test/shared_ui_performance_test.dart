import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:swift_app/core/performance/frame_timing_report.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:swift_app/features/common/shared/widgets/app_loading_page.dart';
import 'package:swift_app/features/common/shared/widgets/vehicle_photo.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/lazy_tab_stack.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/common/shared/widgets/swift_bottom_navigation_bar.dart';
import 'package:swift_app/themes/custom_theme_data.dart';

/// A rendering workload with production shared widgets and local-only data.
/// It deliberately does not initialize the app, Firebase, Maps or any datasource.
void main() {
  final binding = IntegrationTestWidgetsFlutterBinding.ensureInitialized();
  binding.framePolicy = LiveTestWidgetsFlutterBindingFramePolicy.fullyLive;

  testWidgets('profile shared navigation, lazy history and loading surfaces', (
    tester,
  ) async {
    expect(
      kProfileMode,
      isTrue,
      reason:
          'Run with flutter drive --profile; debug timings are not representative.',
    );
    final scenarios = <String, Object?>{};
    final allFrames = <FrameTiming>[];
    double? refreshRate;
    Size? logicalSize;

    for (final brightness in [Brightness.light, Brightness.dark]) {
      final key = GlobalKey<_HarnessState>();
      await tester.pumpWidget(
        MaterialApp(
          theme: brightness == Brightness.light
              ? customThemeData()
              : customDarkThemeData(),
          home: _Harness(key: key),
        ),
      );
      // Warm initial layout/assets before recording; first visits to other tabs
      // remain part of the measured transitions, matching normal lazy mounting.
      await tester.pump(const Duration(seconds: 2));
      final view = View.of(key.currentContext!);
      refreshRate = view.display.refreshRate;
      logicalSize = view.physicalSize / view.devicePixelRatio;

      Future<void> capture(String name, Future<void> Function() action) async {
        final frames = <FrameTiming>[];
        final startWall = DateTime.now().microsecondsSinceEpoch;
        int? endWall;
        final elapsed = Stopwatch()..start();
        void receive(List<FrameTiming> batch) {
          for (final frame in batch) {
            final finishWall = frame.timestampInMicroseconds(
              FramePhase.rasterFinishWallTime,
            );
            final buildWall =
                finishWall -
                (frame.timestampInMicroseconds(FramePhase.rasterFinish) -
                    frame.timestampInMicroseconds(FramePhase.buildStart));
            if (buildWall >= startWall &&
                (endWall == null || finishWall <= endWall)) {
              frames.add(frame);
            }
          }
        }

        binding.addTimingsCallback(receive);
        try {
          await action();
          endWall = DateTime.now().microsecondsSinceEpoch;
          elapsed.stop();
          // Engine callbacks are batched. Keep listening, but filter by the
          // recorded wall-time window so trailing shimmer frames do not leak in.
          await Future<void>.delayed(const Duration(milliseconds: 1200));
        } finally {
          binding.removeTimingsCallback(receive);
        }
        expect(
          frames,
          isNotEmpty,
          reason: '$name did not receive engine FrameTiming samples',
        );
        allFrames.addAll(frames);
        scenarios['${brightness.name}_$name'] = {
          'measuredDurationMs': elapsed.elapsedMicroseconds / 1000,
          ...FrameTimingReport(
            frames: frames,
            displayRefreshRateHz: refreshRate,
          ).toJson(),
        };
      }

      await capture('tab_transitions', () async {
        for (var cycle = 0; cycle < 4; cycle++) {
          for (final tab in [1, 2, 0]) {
            key.currentState!.selectTab(tab);
            await tester.pump(const Duration(milliseconds: 300));
          }
        }
      });

      key.currentState!.selectTab(0);
      await tester.pump(const Duration(milliseconds: 300));
      await capture('lazy_scrolling', () async {
        for (var step = 1; step <= 6; step++) {
          await key.currentState!.history.animateTo(
            step * 500,
            duration: const Duration(milliseconds: 550),
            curve: Curves.easeInOutCubic,
          );
        }
        for (var step = 5; step >= 0; step--) {
          await key.currentState!.history.animateTo(
            step * 500,
            duration: const Duration(milliseconds: 550),
            curve: Curves.easeInOutCubic,
          );
        }
      });

      key.currentState!.selectTab(1);
      await tester.pump(const Duration(milliseconds: 300));
      await capture(
        'loading_shimmer',
        () => tester.pump(const Duration(seconds: 4)),
      );
      await capture('vehicle_photos_first_visit', () async {
        key.currentState!.selectTab(3);
        await tester.pump(const Duration(seconds: 1));
        final gallery = key.currentState!.gallery;
        for (var cycle = 0; cycle < 3; cycle++) {
          await gallery.animateTo(
            gallery.position.maxScrollExtent,
            duration: const Duration(milliseconds: 700),
            curve: Curves.easeInOutCubic,
          );
          await gallery.animateTo(
            0,
            duration: const Duration(milliseconds: 700),
            curve: Curves.easeInOutCubic,
          );
        }
      });
      key.currentState!.showStartup();
      await tester.pump();
      await capture(
        'app_startup_loading',
        () => tester.pump(const Duration(seconds: 4)),
      );
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox.shrink());
    }

    binding.reportData = {
      'shared_ui_performance': {
        'schemaVersion': 1,
        'measuredAtUtc': DateTime.now().toUtc().toIso8601String(),
        'mode': 'profile',
        'platform': defaultTargetPlatform.name,
        'logicalViewport': {
          'width': logicalSize?.width,
          'height': logicalSize?.height,
        },
        'workload':
            'Production shared solid rounded navbar, lazy tab mounting, 200 local history cards, ShimmerBlock loaders, six bundled VehiclePhoto assets and AppLoadingPage in light/dark themes',
        'limitations': [
          'Shared-widget benchmark; excludes native Maps, GPS, network, checkout and complete live application journeys.',
          'Stage budget misses are timing evidence, not a measured presented-FPS or dropped-frame counter.',
          'An emulator or desktop result does not certify mobile-device 120 fps.',
          'Initial page/assets warm for 2 seconds; first secondary-tab mounts are included in transitions.',
        ],
        'scenarios': scenarios,
        'aggregate': FrameTimingReport(
          frames: allFrames,
          displayRefreshRateHz: refreshRate,
        ).toJson(),
      },
    };
  });
}

class _Harness extends StatefulWidget {
  const _Harness({super.key});
  @override
  State<_Harness> createState() => _HarnessState();
}

class _HarnessState extends State<_Harness> {
  int selected = 0;
  final history = ScrollController();
  final gallery = ScrollController();
  bool startup = false;
  void showStartup() => setState(() => startup = true);
  void selectTab(int tab) {
    if (tab != selected) setState(() => selected = tab);
  }

  @override
  void dispose() {
    history.dispose();
    gallery.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => startup
      ? const AppLoadingPage()
      : Scaffold(
          extendBody: true,
          bottomNavigationBar: SwiftBottomNavigationBar(
            selectedIndex: selected,
            onDestinationSelected: selectTab,
            destinations: const [
              SwiftNavigationDestination(
                icon: SwiftIcons.receiptText,
                label: 'History',
              ),
              SwiftNavigationDestination(
                icon: SwiftIcons.refreshCw,
                label: 'Loading',
              ),
              SwiftNavigationDestination(
                icon: SwiftIcons.userRound,
                label: 'Account',
              ),
              SwiftNavigationDestination(
            icon: SwiftIcons.car,
                label: 'Vehicles',
              ),
            ],
          ),
          body: AppGlassBackground(
            child: SafeArea(
              bottom: false,
              child: LazyTabStack(
                index: selected,
                builders: [
                  (_) => ListView.builder(
                    controller: history,
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 110),
                    itemCount: 201,
                    itemBuilder: (context, index) => index == 0
                        ? SwiftAppBar(
                            primary: false,
                            automaticallyImplyLeading: false,
                            toolbarHeight: SwiftAppBar.toolbarHeightFor(
                              context,
                            ),
                            title: const Text('Trip history'),
                          )
                        : AppCard(
                            margin: const EdgeInsets.only(bottom: 12),
                            child: Row(
                              children: [
                                AppIcon(
                                  SwiftIcons.route,
                                  color: Theme.of(context).colorScheme.primary,
                                ),
                                const SizedBox(width: 14),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        'Trip $index',
                                        style: Theme.of(
                                          context,
                                        ).textTheme.titleMedium,
                                      ),
                                      const SizedBox(height: 8),
                                      const Text(
                                        'Completed · 6.8 km · City centre',
                                      ),
                                    ],
                                  ),
                                ),
                                const Text('₹250'),
                              ],
                            ),
                          ),
                  ),
                  (_) => AppShimmer(
                    child: ListView.builder(
                      padding: const EdgeInsets.fromLTRB(16, 24, 16, 110),
                      itemCount: 12,
                      itemBuilder: (_, index) => const AppCard(
                        margin: EdgeInsets.only(bottom: 14),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            ShimmerBlock(width: 180, height: 18),
                            SizedBox(height: 16),
                            ShimmerBlock(width: double.infinity, height: 14),
                            SizedBox(height: 12),
                            ShimmerBlock(width: 130, height: 14),
                          ],
                        ),
                      ),
                    ),
                  ),
                  (_) => ListView(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 110),
                    children: [
                      SwiftAppBar(
                        primary: false,
                        automaticallyImplyLeading: false,
                        toolbarHeight: SwiftAppBar.toolbarHeightFor(context),
                        title: const Text('Account'),
                      ),
                      const AppCard(
                        child: ListTile(
                          leading: AppIcon(SwiftIcons.userRound),
                          title: Text('Profile'),
                          subtitle: Text('Local benchmark data'),
                        ),
                      ),
                      const SizedBox(height: 14),
                      const AppCard(
                        child: ListTile(
                          leading: AppIcon(SwiftIcons.bell),
                          title: Text('Notifications'),
                          subtitle: Text('No backend connections'),
                        ),
                      ),
                    ],
                  ),
                  (_) => ListView.builder(
                    controller: gallery,
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 110),
                    itemCount: 6,
                    itemBuilder: (_, index) => AppCard(
                      margin: const EdgeInsets.only(bottom: 14),
                      child: Row(
                        children: [
                          VehiclePhoto(
                            code: const [
                              'SEDAN',
                              'SUV',
                              'BIKE',
                              'SCOOTER',
                              'AUTO',
                              'VAN',
                            ][index],
                            width: 160,
                            height: 100,
                          ),
                          const SizedBox(width: 16),
                          Text(
                            const [
                              'Sedan',
                              'SUV',
                              'Bike',
                              'Scooter',
                              'Auto',
                              'Van',
                            ][index],
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
}
