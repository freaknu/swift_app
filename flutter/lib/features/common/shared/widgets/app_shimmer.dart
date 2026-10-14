import 'package:flutter/material.dart';

/// Shares one ticker between loading blocks. Each block paints a gentle color
/// pulse without masking the card or creating a moving gradient shader.
class AppShimmer extends StatefulWidget {
  final Widget child;

  const AppShimmer({super.key, required this.child});

  @override
  State<AppShimmer> createState() => _AppShimmerState();
}

class _AppShimmerState extends State<AppShimmer>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1500),
  );

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final animate =
        !MediaQuery.disableAnimationsOf(context) &&
        TickerMode.valuesOf(context).enabled;
    if (animate && !_controller.isAnimating) {
      _controller.repeat();
    } else if (!animate && _controller.isAnimating) {
      _controller.stop();
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return _ShimmerScope(
      progress: _controller,
      animate: !MediaQuery.disableAnimationsOf(context),
      child: RepaintBoundary(child: widget.child),
    );
  }
}

class _ShimmerScope extends InheritedWidget {
  const _ShimmerScope({
    required this.progress,
    required this.animate,
    required super.child,
  });

  final Animation<double> progress;
  final bool animate;

  @override
  bool updateShouldNotify(_ShimmerScope oldWidget) =>
      progress != oldWidget.progress || animate != oldWidget.animate;
}

class ShimmerBlock extends StatelessWidget {
  final double width;
  final double height;
  final double radius;

  const ShimmerBlock({
    super.key,
    required this.width,
    required this.height,
    this.radius = 10,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final base = theme.colorScheme.onSurface.withValues(
      alpha: isDark ? 0.13 : 0.075,
    );
    final highlight = theme.colorScheme.onSurface.withValues(
      alpha: isDark ? 0.23 : 0.14,
    );
    final scope = context.dependOnInheritedWidgetOfExactType<_ShimmerScope>();
    return SizedBox(
      width: width,
      height: height,
      child: CustomPaint(
        painter: _ShimmerBlockPainter(
          progress: scope?.animate == true ? scope!.progress : null,
          radius: radius,
          base: base,
          highlight: highlight,
        ),
      ),
    );
  }
}

class _ShimmerBlockPainter extends CustomPainter {
  _ShimmerBlockPainter({
    required this.progress,
    required this.radius,
    required this.base,
    required this.highlight,
  }) : super(repaint: progress);

  final Animation<double>? progress;
  final double radius;
  final Color base;
  final Color highlight;

  @override
  void paint(Canvas canvas, Size size) {
    final bounds = Offset.zero & size;
    final paint = Paint()..color = base;
    if (progress != null) {
      final value = progress!.value;
      final pulse = value <= 0.5 ? value * 2 : (1 - value) * 2;
      paint.color = Color.lerp(
        base,
        highlight,
        Curves.easeInOut.transform(pulse),
      )!;
    }
    canvas.drawRRect(
      RRect.fromRectAndRadius(bounds, Radius.circular(radius)),
      paint,
    );
  }

  @override
  bool shouldRepaint(_ShimmerBlockPainter oldDelegate) =>
      progress != oldDelegate.progress ||
      radius != oldDelegate.radius ||
      base != oldDelegate.base ||
      highlight != oldDelegate.highlight;
}
