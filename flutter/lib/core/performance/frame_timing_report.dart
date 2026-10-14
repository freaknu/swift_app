import 'dart:math' as math;
import 'dart:ui';

/// Engine timing statistics, not a claim about displayed FPS.
/// UI and raster work run in a pipeline, so each stage has its own budget.
class FrameTimingReport {
  final List<FrameTiming> frames;
  final double? displayRefreshRateHz;

  FrameTimingReport({
    required Iterable<FrameTiming> frames,
    double? displayRefreshRateHz,
  }) : frames = List.unmodifiable(frames),
       displayRefreshRateHz =
           displayRefreshRateHz != null &&
               displayRefreshRateHz.isFinite &&
               displayRefreshRateHz > 0
           ? displayRefreshRateHz
           : null;

  Map<String, Object?> toJson({bool includeRawSamples = true}) {
    final build = frames
        .map((frame) => frame.buildDuration.inMicroseconds)
        .toList();
    final raster = frames
        .map((frame) => frame.rasterDuration.inMicroseconds)
        .toList();
    final span = frames.map((frame) => frame.totalSpan.inMicroseconds).toList();
    return {
      'frameCount': frames.length,
      'displayRefreshRateHz': displayRefreshRateHz,
      'percentileMethod': 'nearest-rank',
      'build': _statistics(build),
      'raster': _statistics(raster),
      'totalSpan': _statistics(span),
      'displayBudget': displayRefreshRateHz == null
          ? null
          : _budget(build, raster, span, displayRefreshRateHz!),
      'target120HzBudget': _budget(build, raster, span, 120),
      if (includeRawSamples) 'rawBuildMicroseconds': build,
      if (includeRawSamples) 'rawRasterMicroseconds': raster,
      if (includeRawSamples) 'rawTotalSpanMicroseconds': span,
    };
  }

  Map<String, Object?> _statistics(List<int> samples) {
    if (samples.isEmpty) {
      return {
        'p50Ms': null,
        'p95Ms': null,
        'p99Ms': null,
        'meanMs': null,
        'maxMs': null,
      };
    }
    final sorted = [...samples]..sort();
    double percentile(double fraction) =>
        sorted[(fraction * sorted.length).ceil() - 1] / 1000;
    return {
      'p50Ms': percentile(0.50),
      'p95Ms': percentile(0.95),
      'p99Ms': percentile(0.99),
      'meanMs':
          samples.fold<int>(0, (sum, value) => sum + value) /
          samples.length /
          1000,
      'maxMs': sorted.last / 1000,
    };
  }

  Map<String, Object?> _budget(
    List<int> build,
    List<int> raster,
    List<int> span,
    double rate,
  ) {
    final budgetMicroseconds = 1000000 / rate;
    int misses(List<int> samples) =>
        samples.where((value) => value > budgetMicroseconds).length;
    var eitherStage = 0;
    for (var index = 0; index < build.length; index++) {
      if (math.max(build[index], raster[index]) > budgetMicroseconds) {
        eitherStage++;
      }
    }
    return {
      'refreshRateHz': rate,
      'budgetMs': 1000 / rate,
      'buildOverBudgetCount': misses(build),
      'rasterOverBudgetCount': misses(raster),
      'eitherStageOverBudgetCount': eitherStage,
      'eitherStageOverBudgetPercent': build.isEmpty
          ? null
          : eitherStage * 100 / build.length,
      // Total span includes pipeline/wait latency and is not a dropped-frame counter.
      'totalSpanOverBudgetCount': misses(span),
    };
  }
}
