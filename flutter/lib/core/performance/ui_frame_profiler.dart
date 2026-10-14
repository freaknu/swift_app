import 'dart:async';
import 'dart:collection';
import 'dart:convert';
import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:swift_app/core/performance/frame_timing_report.dart';

/// Explicitly enabled profile builds only. Normal builds register no callbacks,
/// timers, route observers or logging. Reports contain timings, never API data.
class UiFrameProfiler with WidgetsBindingObserver {
  UiFrameProfiler._();

  static final instance = UiFrameProfiler._();
  static const enabled = kProfileMode && bool.fromEnvironment('PROFILE_UI');
  static const _sampleLimit = 2400;

  final _samples = Queue<FrameTiming>();
  final _routes = <String>{};
  late final NavigatorObserver navigatorObserver = _ProfileRoutes(this);
  Timer? _timer;
  DateTime? _windowStarted;
  var _discardedSamples = 0;
  var _sequence = 0;
  var _foreground = true;

  void start() {
    if (!enabled || _timer != null) return;
    WidgetsBinding.instance.addObserver(this);
    WidgetsBinding.instance.addTimingsCallback(_receive);
    _windowStarted = DateTime.now().toUtc();
    _timer = Timer.periodic(
      const Duration(seconds: 10),
      (_) => _flush('window'),
    );
  }

  /// End an opt-in recording session and release its engine/lifecycle hooks.
  void stop() {
    if (!enabled || _timer == null) return;
    _timer?.cancel();
    _timer = null;
    _flush('stop');
    WidgetsBinding.instance.removeTimingsCallback(_receive);
    WidgetsBinding.instance.removeObserver(this);
  }

  void _receive(List<FrameTiming> frames) {
    if (!_foreground) return;
    for (final frame in frames) {
      if (_samples.length == _sampleLimit) {
        _samples.removeFirst();
        _discardedSamples++;
      }
      _samples.addLast(frame);
    }
  }

  void _route(Route<dynamic>? route) {
    // Only fixed app routes are recorded; arbitrary route names can contain IDs.
    final name = route?.settings.name;
    const known = {
      '/',
      '/home',
      '/login',
      '/signup',
      '/otpPage',
      '/forgetPassword',
      '/changePasswordPage',
    };
    _routes.add(known.contains(name) ? name! : 'unnamed');
  }

  void _flush(String reason) {
    final now = DateTime.now().toUtc();
    final started = _windowStarted;
    _windowStarted = now;
    if (_samples.isEmpty || started == null) return;
    final views = PlatformDispatcher.instance.views;
    final view = views.isEmpty ? null : views.first;
    final report = {
      'schemaVersion': 1,
      'mode': 'profile',
      'platform': defaultTargetPlatform.name,
      'window': ++_sequence,
      'reason': reason,
      'startedAtUtc': started.toIso8601String(),
      'endedAtUtc': now.toIso8601String(),
      'routesObserved': _routes.toList(),
      'discardedSamples': _discardedSamples,
      'logicalViewport': view == null
          ? null
          : {
              'width': view.physicalSize.width / view.devicePixelRatio,
              'height': view.physicalSize.height / view.devicePixelRatio,
            },
      ...FrameTimingReport(
        frames: _samples,
        displayRefreshRateHz: view?.display.refreshRate,
      ).toJson(includeRawSamples: false),
    };
    _samples.clear();
    _routes.clear();
    _discardedSamples = 0;
    // A single compact line can be exported by the host-side collector. This is
    // opt-in instrumentation, not a displayed-FPS or native-map-frame counter.
    debugPrint('SWIFT_UI_PROFILE ${jsonEncode(report)}', wrapWidth: null);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _foreground = true;
      _windowStarted = DateTime.now().toUtc();
    } else if (_foreground) {
      _flush('background');
      _foreground = false;
    }
  }
}

class _ProfileRoutes extends NavigatorObserver {
  _ProfileRoutes(this.profiler);
  final UiFrameProfiler profiler;

  @override
  void didPush(Route<dynamic> route, Route<dynamic>? previousRoute) =>
      profiler._route(route);

  @override
  void didPop(Route<dynamic> route, Route<dynamic>? previousRoute) =>
      profiler._route(previousRoute);

  @override
  void didReplace({Route<dynamic>? newRoute, Route<dynamic>? oldRoute}) =>
      profiler._route(newRoute);
}
