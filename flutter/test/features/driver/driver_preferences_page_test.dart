import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/driver/data/model/driver_notification_preferences_model.dart';
import 'package:swift_app/features/driver/presentation/pages/driver_notification_preferences_page.dart';

const _saved = DriverNotificationPreferencesModel(
  pushEnabled: true,
  emailEnabled: true,
  smsEnabled: false,
  rideAlertsEnabled: true,
  surgeAlertsEnabled: false,
);

class _PreferenceDatasource extends DriverRemoteDatasource {
  _PreferenceDatasource({
    required super.dioClient,
    required super.endpoints,
    required super.authLocalDatasource,
  });

  final load =
      Completer<ApiResponseFormat<DriverNotificationPreferencesModel>>();
  DriverNotificationPreferencesModel? updated;

  @override
  Future<ApiResponseFormat<DriverNotificationPreferencesModel>>
  getNotificationPreferences() => load.future;

  @override
  Future<ApiResponseFormat<DriverNotificationPreferencesModel>>
  updateNotificationPreferences(
    DriverNotificationPreferencesModel preferences,
  ) async {
    updated = preferences;
    return ApiResponseFormat.success(
      data: preferences,
      statusCode: 200,
      message: 'OK',
      endpoint: 'fixture',
    );
  }
}

void main() {
  testWidgets('loading and error preferences fit a short landscape viewport', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(740, 360);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });
    SharedPreferences.setMockInitialValues({'auth.userId': 7});
    final prefs = await SharedPreferences.getInstance();
    final datasource = _PreferenceDatasource(
      dioClient: DioClient(),
      endpoints: RiderApiEndpoints(),
      authLocalDatasource: AuthLocalDatasource(preferences: prefs),
    );
    await tester.pumpWidget(
      MaterialApp(
        home: MediaQuery(
          data: const MediaQueryData(textScaler: TextScaler.linear(1.5)),
          child: DriverNotificationPreferencesPage(datasource: datasource),
        ),
      ),
    );
    await tester.pump();
    expect(tester.takeException(), isNull);
    datasource.load.complete(
      ApiResponseFormat.failure(
        statusCode: 503,
        message:
            'Notification preferences are temporarily unavailable. Please check your connection and try again.',
        endpoint: 'fixture',
      ),
    );
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('Retry'));
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'preferences load on narrow large-text screen and save real switches',
    (tester) async {
      tester.view.physicalSize = const Size(320, 760);
      tester.view.devicePixelRatio = 1;
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });
      SharedPreferences.setMockInitialValues({'auth.userId': 7});
      final prefs = await SharedPreferences.getInstance();
      final datasource = _PreferenceDatasource(
        dioClient: DioClient(),
        endpoints: RiderApiEndpoints(),
        authLocalDatasource: AuthLocalDatasource(preferences: prefs),
      );
      await tester.pumpWidget(
        MaterialApp(
          home: MediaQuery(
            data: const MediaQueryData(textScaler: TextScaler.linear(1.45)),
            child: DriverNotificationPreferencesPage(datasource: datasource),
          ),
        ),
      );
      await tester.pump();
      expect(tester.takeException(), isNull);

      datasource.load.complete(
        ApiResponseFormat.success(
          data: _saved,
          statusCode: 200,
          message: 'OK',
          endpoint: 'fixture',
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('Push notifications'), findsOneWidget);
      expect(find.text('Ride requests'), findsOneWidget);
      expect(find.text('SMS'), findsNothing);
      expect(find.text('Surge alerts'), findsNothing);
      expect(tester.takeException(), isNull);

      final firstSwitch = find.byType(SwitchListTile).first;
      await tester.ensureVisible(firstSwitch);
      await tester.tap(firstSwitch);
      await tester.ensureVisible(find.text('Save preferences'));
      await tester.tap(find.text('Save preferences'));
      await tester.pumpAndSettle();
      expect(datasource.updated?.pushEnabled, isFalse);
      expect(datasource.updated?.emailEnabled, isTrue);
      expect(datasource.updated?.smsEnabled, isFalse);
      expect(tester.takeException(), isNull);
    },
  );
}
