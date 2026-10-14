import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/core/notifications/notification_inbox_datasource.dart';
import 'package:swift_app/core/notifications/notification_inbox_page.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/driver/presentation/pages/complete_profile_page.dart';
import 'package:swift_app/features/driver/presentation/pages/driver_home_page.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';

import 'qa_harness.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('driver dashboard, profile forms and inbox use live backend', (
    tester,
  ) async {
    await launchQaApp(tester, 'DRIVER');
    await waitForWidget(tester, find.byType(DriverHomePage));
    final datasource = sl<DriverRemoteDatasource>();
    final profile = await datasource.getProfile();
    final vehicles = await datasource.getVehicles();
    final types = await datasource.getVehicleTypes();
    final documents = await datasource.getDocuments();
    final completeness = await datasource.getProfileCompleteness();
    expect(profile.success, isTrue, reason: profile.message);
    expect(profile.data?.driverId, isNotNull);
    expect(vehicles.success, isTrue, reason: vehicles.message);
    expect(types.success, isTrue, reason: types.message);
    expect(types.data, isNotEmpty);
    expect(documents.success, isTrue, reason: documents.message);
    expect(completeness.success, isTrue, reason: completeness.message);
    await waitForWidget(tester, find.text('DRIVER CONSOLE'));
    expect(find.text('Driver profile unavailable'), findsNothing);

    final notifications = await sl<NotificationInboxDatasource>().fetch(
      'DRIVER',
    );
    expect(notifications, isA<List<InboxNotification>>());
    await tester.tap(find.byType(NotificationBell));
    await waitForWidget(tester, find.byType(NotificationInboxPage));
    expect(find.text('Notifications'), findsOneWidget);
    await tester.pageBack();
    await tester.pump(const Duration(milliseconds: 600));

    // A complete driver can still return to Vehicle and Documents through the
    // active vehicle card. Incomplete drivers use the completion banner.
    if (vehicles.data?.isNotEmpty == true) {
      await tester.dragUntilVisible(
        find.text('Active vehicle'),
        find.byType(CustomScrollView),
        const Offset(0, -350),
      );
      await tester.ensureVisible(find.text('Active vehicle'));
      await tester.tap(
        find
            .ancestor(
              of: find.text('Active vehicle'),
              matching: find.byType(AppCard),
            )
            .first,
      );
    } else {
      await tester.tap(find.text('Finish your profile'));
    }
    await waitForWidget(tester, find.byType(CompleteProfilePage));
    await tester.tap(find.text('Vehicle').first);
    await tester.pump(const Duration(seconds: 1));
    expect(find.text('Vehicle details'), findsWidgets);
    final previousVehicle = vehicles.data!.first;
    final newColor = 'QA Ocean Blue';
    final colorField = find.byWidgetPredicate(
      (widget) =>
          widget is TextField && widget.decoration?.labelText == 'Color',
    );
    await tester.ensureVisible(colorField);
    await tester.enterText(colorField, newColor);
    await tester.ensureVisible(find.text('Save vehicle'));
    await tester.tap(find.text('Save vehicle'));
    await waitForCondition(
      tester,
      () => find.text('Vehicle saved.').evaluate().isNotEmpty,
      description: 'vehicle save confirmation',
    );
    final savedVehicles = await datasource.getVehicles();
    expect(savedVehicles.success, isTrue, reason: savedVehicles.message);
    final savedVehicle = savedVehicles.data!.firstWhere(
      (item) => item.id == previousVehicle.id,
    );
    expect(savedVehicle.id, previousVehicle.id);
    expect(savedVehicle.vehicleType, previousVehicle.vehicleType);
    expect(savedVehicle.color, newColor);

    await tester.tap(find.text('Documents').first);
    await tester.pump(const Duration(seconds: 1));
    expect(find.text('Documents'), findsWidgets);
    await waitForWidget(tester, find.text('Driving licence'));
    final url = 'https://example.invalid/swift/qa-driving-licence';
    final documentAction = find.text(
      documents.data!.any(
            (item) =>
                item.documentType == 'DRIVING_LICENCE' &&
                item.documentUrl.isNotEmpty,
          )
          ? 'Replace'
          : 'Upload',
    );
    await tester.ensureVisible(documentAction.first);
    await tester.tap(documentAction.first);
    await waitForWidget(tester, find.byType(AlertDialog));
    await tester.enterText(
      find.descendant(
        of: find.byType(AlertDialog),
        matching: find.byType(TextField),
      ),
      url,
    );
    await tester.tap(find.text('Save URL'));
    await waitForCondition(
      tester,
      () => find.text(url).evaluate().isNotEmpty,
      description: 'document URL shown after save',
    );
    final savedDocuments = await datasource.getDocuments();
    expect(savedDocuments.success, isTrue, reason: savedDocuments.message);
    final licence = savedDocuments.data!.firstWhere(
      (item) => item.documentType == 'DRIVING_LICENCE',
    );
    expect(licence.documentUrl, url);
    expect(licence.verificationStatus, 'PENDING');
  });
}
