import 'package:flutter/material.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/swift_brand_mark.dart';

/// One quiet startup screen while the session's data and map initialize.
class AppLoadingPage extends StatelessWidget {
  const AppLoadingPage({
    super.key,
    this.message = 'Loading your data and map…',
  });

  final String message;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 360),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const SwiftBrandMark(
                    size: 64,
                    borderRadius: 22,
                    excludeFromSemantics: true,
                  ),
                  const SizedBox(height: 20),
                  Text(
                    'Swift',
                    style: theme.textTheme.headlineMedium?.copyWith(
                      color: scheme.onSurface,
                      fontSize: 32,
                      fontWeight: FontWeight.w600,
                      letterSpacing: -0.8,
                    ),
                  ),
                  const SizedBox(height: 10),
                  Semantics(
                    liveRegion: true,
                    child: Text(
                      message,
                      textAlign: TextAlign.center,
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: scheme.onSurfaceVariant,
                        height: 1.5,
                      ),
                    ),
                  ),
                  const SizedBox(height: 28),
                  const AppLoadingProgress(),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// An indeterminate indicator with a still glyph for reduced motion.
class AppLoadingProgress extends StatelessWidget {
  const AppLoadingProgress({super.key});

  @override
  Widget build(BuildContext context) => Semantics(
    label: 'Loading',
    child: ExcludeSemantics(
      child: SizedBox.square(
        dimension: 24,
        child: MediaQuery.disableAnimationsOf(context)
            ? AppIcon(
                SwiftIcons.loaderCircle,
                size: 24,
                color: Theme.of(context).colorScheme.primary,
              )
            : CircularProgressIndicator(
                strokeWidth: 2,
                strokeCap: StrokeCap.round,
                color: Theme.of(context).colorScheme.primary,
              ),
      ),
    ),
  );
}

/// Keeps the real screen mounted and laid out beneath one opaque startup page.
/// Completion is latched for this session; refresh cannot reopen the page.
class AppStartupGate extends StatefulWidget {
  const AppStartupGate({super.key, required this.ready, required this.child});

  final bool ready;
  final Widget child;

  @override
  State<AppStartupGate> createState() => _AppStartupGateState();
}

class _AppStartupGateState extends State<AppStartupGate> {
  late bool _finished = widget.ready;

  @override
  void didUpdateWidget(AppStartupGate oldWidget) {
    super.didUpdateWidget(oldWidget);
    _finished = _finished || widget.ready;
  }

  @override
  Widget build(BuildContext context) => Stack(
    fit: StackFit.expand,
    children: [
      ExcludeSemantics(
        excluding: !_finished,
        child: AbsorbPointer(
          absorbing: !_finished,
          child: ExcludeFocus(excluding: !_finished, child: widget.child),
        ),
      ),
      if (!_finished) const AppLoadingPage(),
    ],
  );
}
