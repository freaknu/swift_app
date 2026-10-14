import 'dart:convert';
import 'dart:io';

import 'package:integration_test/integration_test_driver.dart';

Future<void> main() => integrationDriver(
  responseDataCallback: (data) async {
    final report = data?['shared_ui_performance'];
    if (report is! Map) {
      throw StateError('Profile run did not return frame timings');
    }
    final destination =
        Platform.environment['UI_PERFORMANCE_REPORT'] ??
        'build/ui-performance-report.json';
    final file = File(destination);
    await file.parent.create(recursive: true);
    await file.writeAsString(
      '${const JsonEncoder.withIndent('  ').convert({...report, 'deviceId': Platform.environment['UI_PERFORMANCE_DEVICE']})}\n',
    );
    stdout.writeln('Measured UI frame report saved to ${file.absolute.path}');
  },
);
