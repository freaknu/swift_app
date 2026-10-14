import 'package:flutter/widgets.dart';

/// Mount tabs on first visit, retain their state, and silence offscreen tickers.
/// Unlike IndexedStack, unvisited tabs perform no layout or data/UI setup.
class LazyTabStack extends StatefulWidget {
  final int index;
  final List<WidgetBuilder> builders;

  const LazyTabStack({super.key, required this.index, required this.builders})
    : assert(index >= 0 && index < builders.length);

  @override
  State<LazyTabStack> createState() => _LazyTabStackState();
}

class _LazyTabStackState extends State<LazyTabStack> {
  final Set<int> _visited = {};

  @override
  Widget build(BuildContext context) {
    _visited.add(widget.index);
    return Stack(
      fit: StackFit.expand,
      children: [
        for (var index = 0; index < widget.builders.length; index++)
          if (_visited.contains(index))
            Offstage(
              key: ValueKey(index),
              offstage: index != widget.index,
              child: TickerMode(
                enabled: index == widget.index,
                child: RepaintBoundary(
                  child: Builder(builder: widget.builders[index]),
                ),
              ),
            ),
      ],
    );
  }
}
