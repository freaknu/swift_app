import 'dart:ui';

import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/core/performance/frame_timing_report.dart';

FrameTiming frame(int build, int raster) => FrameTiming(
  vsyncStart: 0,
  buildStart: 0,
  buildFinish: build,
  rasterStart: build,
  rasterFinish: build + raster,
  rasterFinishWallTime: build + raster,
);

void main() {
  test(
    'reports nearest-rank percentiles from actual microsecond durations',
    () {
      final json = FrameTimingReport(
        frames: List.generate(
          100,
          (i) => frame((i + 1) * 100, (100 - i) * 200),
        ),
        displayRefreshRateHz: 60,
      ).toJson();
      final build = json['build'] as Map;
      final raster = json['raster'] as Map;
      expect(build['p50Ms'], 5);
      expect(build['p95Ms'], 9.5);
      expect(build['p99Ms'], 9.9);
      expect(raster['p50Ms'], 10);
      expect(raster['p95Ms'], 19);
      expect(raster['p99Ms'], 19.8);
      expect(json['frameCount'], 100);
    },
  );

  test(
    'separates actual display budget from 120 Hz target and pipeline span',
    () {
      final json = FrameTimingReport(
        frames: [frame(10000, 10000), frame(17000, 1000), frame(1000, 17000)],
        displayRefreshRateHz: 60,
      ).toJson();
      final actual = json['displayBudget'] as Map;
      final target = json['target120HzBudget'] as Map;
      expect(actual['eitherStageOverBudgetCount'], 2);
      expect(actual['buildOverBudgetCount'], 1);
      expect(actual['rasterOverBudgetCount'], 1);
      expect(actual['totalSpanOverBudgetCount'], 3);
      expect(target['eitherStageOverBudgetCount'], 3);
      expect(target['budgetMs'], closeTo(8.333333, 0.000001));
    },
  );

  test('missing samples or invalid refresh rate remain unavailable', () {
    for (final rate in [null, 0.0, double.nan, double.infinity]) {
      final json = FrameTimingReport(
        frames: [],
        displayRefreshRateHz: rate,
      ).toJson();
      expect(json['frameCount'], 0);
      expect(json['displayBudget'], isNull);
      expect((json['build'] as Map)['p95Ms'], isNull);
      expect(
        (json['target120HzBudget'] as Map)['eitherStageOverBudgetPercent'],
        isNull,
      );
    }
  });

  test('exact budget boundary is not counted as over budget', () {
    final json = FrameTimingReport(
      frames: [frame(10000, 10000)],
      displayRefreshRateHz: 100,
    ).toJson();
    expect((json['displayBudget'] as Map)['eitherStageOverBudgetCount'], 0);
    expect((json['build'] as Map)['p99Ms'], 10);
  });
}
