import 'package:swift_app/features/rider/home/presentation/widgets/google_map_widget.dart';
import 'package:swift_app/themes/custom_theme_data.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const channel = MethodChannel('flutter.baseflow.com/geolocator');
  TestWidgetsFlutterBinding.ensureInitialized();
  final messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

  tearDown(() => messenger.setMockMethodCallHandler(channel, null));

  Future<void> pumpMap(WidgetTester tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: customThemeData(),
        home: const Scaffold(body: GoogleMapWidget()),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('disabled location services explain recovery and open settings', (
    tester,
  ) async {
    final calls = <String>[];
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call.method);
      if (call.method == 'isLocationServiceEnabled') return false;
      if (call.method == 'openLocationSettings') return true;
      return null;
    });
    await pumpMap(tester);
    expect(find.text('Turn on Location Services.'), findsOneWidget);
    expect(find.byType(CircularProgressIndicator), findsNothing);
    await tester.tap(find.text('Open settings'));
    await tester.pump();
    expect(calls, contains('openLocationSettings'));
    expect(tester.takeException(), isNull);
  });

  testWidgets('permanently denied permission opens application settings', (
    tester,
  ) async {
    final calls = <String>[];
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call.method);
      if (call.method == 'isLocationServiceEnabled') return true;
      if (call.method == 'checkPermission') return 1;
      if (call.method == 'openAppSettings') return true;
      return null;
    });
    await pumpMap(tester);
    expect(find.text('Allow location access in Settings.'), findsOneWidget);
    await tester.tap(find.text('Open settings'));
    await tester.pump();
    expect(calls, contains('openAppSettings'));
    expect(calls, isNot(contains('getCurrentPosition')));
  });

  testWidgets('denied permission offers a retry without trapping the rider', (
    tester,
  ) async {
    var requests = 0;
    messenger.setMockMethodCallHandler(channel, (call) async {
      if (call.method == 'isLocationServiceEnabled') return true;
      if (call.method == 'checkPermission') return 0;
      if (call.method == 'requestPermission') {
        requests++;
        return 0;
      }
      return null;
    });
    await pumpMap(tester);
    expect(find.text('Location access is off.'), findsOneWidget);
    await tester.tap(find.text('Try again'));
    await tester.pumpAndSettle();
    expect(requests, 2);
    expect(find.byType(CircularProgressIndicator), findsNothing);
    expect(tester.takeException(), isNull);
  });
}
