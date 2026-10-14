import 'dart:convert';
import 'dart:io';

import 'package:integration_test/integration_test_driver.dart';

Future<void> main() => integrationDriver(
  responseDataCallback: (data) async {
    final report = data?['profiler_smoke'];
    if (report is! Map) {
      throw StateError('Recorder did not return engine timings');
    }
    final file = File(
      Platform.environment['UI_PROFILER_REPORT'] ??
          'build/ui-profiler-smoke.json',
    );
    await file.parent.create(recursive: true);
    await file.writeAsString(
      '${const JsonEncoder.withIndent('  ').convert(report)}\n',
    );
    stdout.writeln('Recorder smoke report saved to ${file.absolute.path}');
  },
);
