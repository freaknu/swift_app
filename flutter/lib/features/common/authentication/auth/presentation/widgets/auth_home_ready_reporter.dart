import 'dart:async';

import 'package:flutter/material.dart';

/// Reports a painted, current home screen, then retries a transient failure.
/// The acknowledgement never blocks navigation or changes authentication state.
class AuthHomeReadyReporter extends StatefulWidget {
  const AuthHomeReadyReporter({
    super.key,
    required this.ready,
    required this.child,
    this.onReady,
  });

  final bool ready;
  final Widget child;
  final Future<bool> Function()? onReady;

  @override
  State<AuthHomeReadyReporter> createState() => _AuthHomeReadyReporterState();
}

class _AuthHomeReadyReporterState extends State<AuthHomeReadyReporter>
    with WidgetsBindingObserver {
  bool _reported = false;
  bool _scheduled = false;
  bool _inFlight = false;
  Timer? _retry;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void didUpdateWidget(AuthHomeReadyReporter oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!widget.ready) _retry?.cancel();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _schedule();
      WidgetsBinding.instance.ensureVisualUpdate();
    } else {
      _retry?.cancel();
    }
  }

  bool get _canReport =>
      mounted &&
      widget.ready &&
      !_reported &&
      widget.onReady != null &&
      ModalRoute.of(context)?.isCurrent != false &&
      (WidgetsBinding.instance.lifecycleState == null ||
          WidgetsBinding.instance.lifecycleState == AppLifecycleState.resumed);

  void _schedule() {
    if (_scheduled || _inFlight || _retry?.isActive == true || !_canReport) {
      return;
    }
    _scheduled = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _scheduled = false;
      if (_canReport) unawaited(_report());
    });
  }

  Future<void> _report() async {
    _inFlight = true;
    var success = false;
    try {
      success = await widget.onReady!();
    } catch (_) {
      // The saved challenge remains available when connectivity recovers.
    } finally {
      _inFlight = false;
    }
    if (!mounted) return;
    _reported = success;
    if (!success && _canReport) {
      _retry = Timer(const Duration(seconds: 5), () {
        _schedule();
        WidgetsBinding.instance.ensureVisualUpdate();
      });
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _retry?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    _schedule();
    return widget.child;
  }
}
