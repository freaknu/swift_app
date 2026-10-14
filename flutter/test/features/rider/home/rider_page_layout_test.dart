import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/features/rider/home/data/datasource/rider_account_datasource.dart';
import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_state.dart';
import 'package:swift_app/features/rider/home/presentation/models/location_selection.dart';
import 'package:swift_app/features/rider/home/presentation/pages/account_settings_pages.dart';
import 'package:swift_app/features/rider/home/presentation/pages/rider_activity_page.dart';
import 'package:swift_app/features/rider/home/presentation/pages/trip_location_page.dart';
import 'package:swift_app/themes/app_theme.dart';

void main() {
  for (final brightness in Brightness.values) {
    testWidgets(
      'settings actions remain usable with large text in $brightness',
      (tester) async {
        _phone(tester);
        final api = _AccountApi();
        await tester.pumpWidget(
          _app(NotificationPreferencesPage(datasource: api), brightness),
        );
        await tester.pumpAndSettle();
        await tester.ensureVisible(find.byType(Switch).first);
        await tester.pumpAndSettle();
        await tester.tap(find.byType(Switch).first);
        await tester.scrollUntilVisible(
          find.text('Save preferences'),
          250,
          scrollable: find.byType(Scrollable).first,
        );
        await tester.pumpAndSettle();
        await tester.tap(find.text('Save preferences'));
        await tester.pumpAndSettle();
        expect(api.saved?['pushNotificationEnabled'], isFalse);
        expect(find.text('Preferences saved'), findsOneWidget);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets('activity fares and long statuses wrap in $brightness', (
      tester,
    ) async {
      _phone(tester);
      await sl.reset();
      addTearDown(sl.reset);
      final user = _UserBloc();
      addTearDown(user.close);
      final client = DioClient();
      client.dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (request, handler) => handler.resolve(
            Response(
              requestOptions: request,
              statusCode: 200,
              data: {
                'success': true,
                'data': {
                  'last': true,
                  'content': [
                    {
                      'id': 24,
                      'currentStatus': 'WAITING_FOR_PAYMENT_CONFIRMATION',
                      'createdAt': '2026-10-14T08:15:00',
                      'estimatedDistance': 18.4,
                    },
                  ],
                },
              },
            ),
          ),
        ),
      );
      sl.registerSingleton<DioClient>(client);
      sl.registerSingleton<RiderApiEndpoints>(RiderApiEndpoints());
      await tester.pumpWidget(
        BlocProvider<UserBloc>.value(
          value: user,
          child: _app(const Scaffold(body: RiderActivityPage()), brightness),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('Waiting for payment confirmation'), findsOneWidget);
      expect(find.text('Pickup unavailable'), findsOneWidget);
      await tester.ensureVisible(find.text('Fare unavailable'));
      await tester.pumpAndSettle();
      expect(find.text('18.4 km'), findsOneWidget);
      expect(tester.takeException(), isNull);
    });

    testWidgets('route inputs scroll above the keyboard in $brightness', (
      tester,
    ) async {
      _phone(tester);
      const locationChannel = MethodChannel('flutter.baseflow.com/geolocator');
      final messenger =
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
      messenger.setMockMethodCallHandler(locationChannel, (call) async {
        if (call.method == 'isLocationServiceEnabled') return false;
        if (call.method == 'checkPermission') return 1;
        return null;
      });
      addTearDown(
        () => messenger.setMockMethodCallHandler(locationChannel, null),
      );
      await tester.pumpWidget(
        _app(
          TripLocationPage(
            initialPickup: LocationSelection.fromCoordinates(
              latitude: 12.97,
              longitude: 77.59,
              label: 'A long pickup address in Bengaluru, Karnataka',
            ),
            initialDestination: LocationSelection.fromCoordinates(
              latitude: 12.99,
              longitude: 77.60,
              label: 'A long destination address in Bengaluru, Karnataka',
            ),
          ),
          brightness,
          keyboard: 220,
        ),
      );
      await tester.pumpAndSettle();
      final routeScroll = find.byType(SingleChildScrollView).first;
      expect(
        tester
            .state<ScrollableState>(
              find
                  .descendant(
                    of: routeScroll,
                    matching: find.byType(Scrollable),
                  )
                  .first,
            )
            .position
            .maxScrollExtent,
        greaterThan(0),
      );
      await tester.ensureVisible(find.text('To'));
      await tester.pumpAndSettle();
      final next = tester.getRect(find.text('Next'));
      expect(next.bottom, lessThanOrEqualTo(568 - 220));
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox.shrink());
    });
  }
}

void _phone(WidgetTester tester) {
  tester.view.physicalSize = const Size(320, 568);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
}

Widget _app(Widget child, Brightness brightness, {double keyboard = 0}) =>
    MaterialApp(
      theme: AppTheme.forBrightness(brightness),
      builder: (context, child) => MediaQuery(
        data: MediaQuery.of(context).copyWith(
          textScaler: TextScaler.linear(2),
          padding: const EdgeInsets.only(top: 44, bottom: 34),
          viewPadding: const EdgeInsets.only(top: 44, bottom: 34),
          viewInsets: EdgeInsets.only(bottom: keyboard),
        ),
        child: child!,
      ),
      home: child,
    );

class _AccountApi implements RiderAccountDatasource {
  Map<String, dynamic>? saved;

  @override
  Future<Map<String, dynamic>> preferences() async => {
    'pushNotificationEnabled': true,
    'emailNotificationEnabled': true,
    'smsNotificationEnabled': false,
  };

  @override
  Future<void> savePreferences(Map<String, dynamic> data) async {
    saved = Map.of(data);
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _UserBloc extends Cubit<UserState> implements UserBloc {
  _UserBloc()
    : super(
        UserState(
          status: UserStatus.success,
          user: UserReponse(
            id: 8,
            authUserId: 20,
            fullName: 'Rider',
            email: null,
            phoneNumber: '',
            profileImageUrl: null,
            active: true,
            deleted: false,
            allAddresses: const [],
          ),
        ),
      );

  @override
  void add(UserEvent event) {}

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}
