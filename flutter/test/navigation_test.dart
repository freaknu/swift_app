import 'dart:async';

import 'package:dio/dio.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/core/notifications/notification_inbox_datasource.dart';
import 'package:swift_app/constants/enums/address_type_enum.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_bloc.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_event.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_state.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_request.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_response.dart';
import 'package:swift_app/features/rider/home/domain/entity/ride_history_address.dart';
import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_state.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_history_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_history_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_history_state.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_state.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_state.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/saved_address_card_widget.dart';
import 'package:swift_app/features/rider/widgets/main_page.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/google_map_widget.dart';
import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';
import 'package:swift_app/features/rider/ride/domain/repository/ride_repository.dart';
import 'package:swift_app/features/rider/ride/domain/usecases/ride_lifecycle_usecase.dart';
import 'package:swift_app/features/common/shared/widgets/app_loading_page.dart';
import 'package:swift_app/features/common/shared/theme_toggle_button.dart';
import 'package:swift_app/features/common/shared/widgets/swift_bottom_navigation_bar.dart';
import 'package:swift_app/themes/custom_theme_data.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets(
    'navigation opens real saved places, activity, offers and account',
    (tester) async {
      await _pumpMainPage(tester);
      expect(find.text('Where would you like to go?'), findsOneWidget);

      await _selectTab(tester, 'Saved');
      expect(find.text('Saved'), findsWidgets);
      await tester.tap(find.widgetWithText(SavedAddressCard, 'Work'));
      await tester.pumpAndSettle();
      expect(find.text('Plan your ride'), findsOneWidget);
      expect(find.text('Work'), findsWidgets);
      await tester.tap(find.byTooltip('Back').first);
      await tester.pumpAndSettle();

      await _selectTab(tester, 'Activity');
      expect(find.text('Activity'), findsWidgets);
      // Activity reads authoritative ride history, including fare and status.
      expect(find.text('Completed'), findsWidgets);
      expect(find.text('₹248'), findsWidgets);
      await tester.drag(
        find.byType(CustomScrollView).last,
        const Offset(0, -400),
      );
      await tester.pumpAndSettle();
      expect(find.text('Ride #14'), findsOneWidget);

      await _selectTab(tester, 'Offers');
      expect(find.text('Weekend savings'), findsOneWidget);
      expect(find.text('WEEKEND20'), findsOneWidget);

      await _selectTab(tester, 'Account');
      expect(find.text('Account'), findsWidgets);
      expect(find.text('Asha Rao'), findsOneWidget);
      expect(find.text('asha@example.com'), findsOneWidget);
      expect(find.text('+91 9000000000'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'collection refreshes use shared blocs and preserve offer request',
    (tester) async {
      final fixture = await _pumpMainPage(tester);

      await _selectTab(tester, 'Saved');
      await tester.tap(find.byTooltip('Refresh'));
      expect(fixture.user.events.single, isA<FetchUserDetails>());

      await _selectTab(tester, 'Activity');
      await tester.tap(find.byTooltip('Refresh'));
      await tester.pumpAndSettle();
      expect(fixture.activityRequests.length, 2);

      await _selectTab(tester, 'Offers');
      await tester.tap(find.byTooltip('Refresh'));
      final event = fixture.offers.events.single as FetchOffers;
      expect(event.request, same(fixture.offerRequest));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('where to opens the route editor with editable fields', (
    tester,
  ) async {
    await _pumpMainPage(tester);

    await tester.tap(find.text('Where would you like to go?'));
    await tester.pumpAndSettle();

    expect(find.text('Plan your ride'), findsOneWidget);
    expect(find.text('From'), findsOneWidget);
    expect(find.text('To'), findsOneWidget);
    expect(find.text('Choose on map'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Next'), findsOneWidget);

    await tester.enterText(find.byType(TextField).last, 'Bengaluru Airport');
    await tester.pump(const Duration(milliseconds: 450));

    expect(find.text('Search "Bengaluru Airport"'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('account can retry missing details and dispatch sign out once', (
    tester,
  ) async {
    final fixture = await _pumpMainPage(tester);
    fixture.user.update(const UserState(status: UserStatus.failure));
    await _selectTab(tester, 'Account');

    expect(find.text('Unable to load account.'), findsOneWidget);
    await tester.tap(find.text('Try again'));
    expect(fixture.user.events.single, isA<FetchUserDetails>());

    await tester.tap(find.text('Sign out'));
    expect(fixture.auth.events.single, isA<AuthLogoutRequested>());

    fixture.auth.update(const AuthLoading(AuthAction.logout));
    await tester.pump();
    final button = tester.widget<OutlinedButton>(
      find.widgetWithText(OutlinedButton, 'Signing out…'),
    );
    expect(button.onPressed, isNull);
    expect(tester.takeException(), isNull);
  });

  testWidgets('navigation and account fit a narrow screen with larger text', (
    tester,
  ) async {
    final fixture = await _pumpMainPage(
      tester,
      size: const Size(320, 740),
      textScale: 1.5,
    );
    fixture.user.update(const UserState(status: UserStatus.success));
    fixture.history.update(
      const RideHistoryState(status: RideHistoryStatus.success),
    );
    fixture.offers.update(const OfferState(status: OfferStatus.success));

    for (final tab in ['Saved', 'Activity', 'Offers', 'Account', 'Ride']) {
      await _selectTab(tester, tab);
      expect(tester.takeException(), isNull, reason: '$tab must not overflow');
    }
  });

  testWidgets('launch covers header and navigation until APIs and map settle', (
    tester,
  ) async {
    final location = Completer<bool>();
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    var acknowledgements = 0;
    late UserState loadedUser;
    final fixture = await _pumpMainPage(
      tester,
      settle: false,
      initialLocationServices: location.future,
      onHomeReady: () async {
        acknowledgements++;
        return true;
      },
      setup: (fixture) {
        loadedUser = fixture.user.state;
        fixture.user.update(const UserState(status: UserStatus.loading));
        fixture.rideTypes.update(
          const RideTypeState(status: RideTypeStatus.loading),
        );
        fixture.offers.update(const OfferState(status: OfferStatus.loading));
        fixture.history.update(
          const RideHistoryState(status: RideHistoryStatus.loading),
        );
      },
    );
    final mapState = tester.state(find.byType(GoogleMapWidget));
    expect(find.byType(AppLoadingPage), findsOneWidget);
    expect(find.text('Account').hitTestable(), findsNothing);
    expect(find.byType(ThemeToggleButton).hitTestable(), findsNothing);
    expect(acknowledgements, 0);

    fixture.user.update(loadedUser);
    fixture.rideTypes.update(
      const RideTypeState(status: RideTypeStatus.success),
    );
    fixture.offers.update(const OfferState(status: OfferStatus.failure));
    fixture.history.update(
      const RideHistoryState(status: RideHistoryStatus.success),
    );
    await _pumpLaunchFrames(tester);
    expect(find.byType(AppLoadingPage), findsOneWidget);
    expect(acknowledgements, 0);
    expect(tester.state(find.byType(GoogleMapWidget)), same(mapState));

    location.complete(false);
    await tester.pumpAndSettle();
    expect(find.byType(AppLoadingPage), findsNothing);
    expect(find.text('Open settings').hitTestable(), findsOneWidget);
    expect(find.text('Account').hitTestable(), findsOneWidget);
    expect(find.byType(ThemeToggleButton).hitTestable(), findsOneWidget);
    expect(acknowledgements, 1);
    expect(tester.state(find.byType(GoogleMapWidget)), same(mapState));
    expect(tester.takeException(), isNull);
  });

  testWidgets('map failure settles launch while a deferred offer still waits', (
    tester,
  ) async {
    final fixture = await _pumpMainPage(
      tester,
      settle: false,
      setup: (fixture) =>
          fixture.offers.update(const OfferState(status: OfferStatus.loading)),
    );
    expect(find.byType(AppLoadingPage), findsOneWidget);
    fixture.offers.update(const OfferState(status: OfferStatus.success));
    await tester.pumpAndSettle();
    expect(find.byType(AppLoadingPage), findsNothing);
    expect(find.text('Turn on Location Services.'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('initial profile failure reveals retry without waiting on map', (
    tester,
  ) async {
    final location = Completer<bool>();
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    var acknowledgements = 0;
    final fixture = await _pumpMainPage(
      tester,
      settle: false,
      initialLocationServices: location.future,
      onHomeReady: () async {
        acknowledgements++;
        return true;
      },
      setup: (fixture) =>
          fixture.user.update(const UserState(status: UserStatus.loading)),
    );
    expect(find.byType(AppLoadingPage), findsOneWidget);
    fixture.user.update(
      const UserState(
        status: UserStatus.failure,
        message: 'Could not load saved places.',
      ),
    );
    await _pumpLaunchFrames(tester);
    expect(find.byType(AppLoadingPage), findsNothing);
    expect(find.text('Could not load saved places.'), findsOneWidget);
    await tester.tap(find.text('Retry').first);
    expect(fixture.user.events.single, isA<FetchUserDetails>());
    location.complete(false);
    await tester.pumpAndSettle();
    expect(find.text('Open settings'), findsOneWidget);
    expect(acknowledgements, 0);
    expect(tester.takeException(), isNull);
  });

  testWidgets('initial catalog failure reveals vehicle retry immediately', (
    tester,
  ) async {
    final location = Completer<bool>();
    final fixture = await _pumpMainPage(
      tester,
      settle: false,
      initialLocationServices: location.future,
      setup: (fixture) => fixture.rideTypes.update(
        const RideTypeState(status: RideTypeStatus.loading),
      ),
    );
    expect(find.byType(AppLoadingPage), findsOneWidget);
    fixture.rideTypes.update(
      const RideTypeState(status: RideTypeStatus.failure),
    );
    await _pumpLaunchFrames(tester);
    expect(find.byType(AppLoadingPage), findsNothing);
    await tester.ensureVisible(find.text('Retry').last);
    await _pumpLaunchFrames(tester);
    expect(find.text('Vehicle types could not load.'), findsOneWidget);
    await tester.tap(find.text('Retry').last);
    expect(fixture.rideTypes.events.single, isA<FetchRideTypes>());
    location.complete(false);
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
  });

  testWidgets('empty terminal profile exposes account recovery', (
    tester,
  ) async {
    final location = Completer<bool>();
    final fixture = await _pumpMainPage(
      tester,
      settle: false,
      initialLocationServices: location.future,
      setup: (fixture) =>
          fixture.user.update(const UserState(status: UserStatus.success)),
    );
    expect(find.byType(AppLoadingPage), findsNothing);
    await _selectTab(tester, 'Account');
    expect(find.text('Unable to load account.'), findsOneWidget);
    await tester.tap(find.text('Try again'));
    expect(fixture.user.events.single, isA<FetchUserDetails>());
    location.complete(false);
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
  });

  testWidgets('launch waits for active ride recovery before showing home', (
    tester,
  ) async {
    final repository = _DeferredActiveRideRepository();
    sl.registerSingleton<RideLifecycleUsecase>(
      RideLifecycleUsecase(repository),
    );
    addTearDown(() => sl.unregister<RideLifecycleUsecase>());
    await _pumpMainPage(tester, settle: false);
    expect(repository.requests, 1);
    expect(find.byType(AppLoadingPage), findsOneWidget);
    repository.response.complete(
      const RideResponse(id: 42, currentStatus: 'MATCHING'),
    );
    await tester.pumpAndSettle();
    expect(find.byType(AppLoadingPage), findsNothing);
    expect(find.text('Finding your driver'), findsOneWidget);
    expect(repository.requests, 1);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'a late failed ride check schedules reduced motion startup exit',
    (tester) async {
      final repository = _DeferredActiveRideRepository();
      sl.registerSingleton<RideLifecycleUsecase>(
        RideLifecycleUsecase(repository),
      );
      addTearDown(() => sl.unregister<RideLifecycleUsecase>());
      await _pumpMainPage(tester, settle: false, disableAnimations: true);
      expect(repository.requests, 1);
      expect(find.byType(AppLoadingPage), findsOneWidget);
      expect(tester.binding.hasScheduledFrame, isFalse);

      repository.response.completeError(StateError('Gateway unavailable'));
      await tester.idle();
      // idle only delivers the failed future; it cannot mask a missing frame
      // request by rendering the queued postframe callback itself.
      expect(tester.binding.hasScheduledFrame, isTrue);
      await tester.pumpAndSettle();
      expect(find.byType(AppLoadingPage), findsNothing);
      expect(find.text('Account').hitTestable(), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('subsequent refresh does not reopen the startup page', (
    tester,
  ) async {
    final fixture = await _pumpMainPage(tester);
    expect(find.byType(AppLoadingPage), findsNothing);
    fixture.user.update(
      fixture.user.state.copyWith(status: UserStatus.loading),
    );
    fixture.rideTypes.update(
      const RideTypeState(status: RideTypeStatus.loading),
    );
    fixture.offers.update(
      fixture.offers.state.copyWith(status: OfferStatus.loading),
    );
    fixture.history.update(
      fixture.history.state.copyWith(status: RideHistoryStatus.loading),
    );
    await _pumpLaunchFrames(tester);
    expect(find.byType(AppLoadingPage), findsNothing);
    expect(find.text('Account').hitTestable(), findsOneWidget);
    await _selectTab(tester, 'Account');
    expect(find.text('Asha Rao'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}

Future<void> _pumpLaunchFrames(WidgetTester tester) async {
  // Initializers report once postframe; keep pumping bounded frames while the
  // indeterminate startup indicator intentionally remains active.
  for (var i = 0; i < 4; i++) {
    await tester.pump(const Duration(milliseconds: 16));
  }
}

Future<void> _selectTab(WidgetTester tester, String label) async {
  await tester.tap(
    find.descendant(
      of: find.byType(SwiftBottomNavigationBar),
      matching: find.text(label),
    ),
  );
  await tester.pumpAndSettle();
}

Future<_NavigationFixture> _pumpMainPage(
  WidgetTester tester, {
  Size size = const Size(390, 844),
  double textScale = 1,
  bool disableAnimations = false,
  bool settle = true,
  Future<bool>? initialLocationServices,
  void Function(_NavigationFixture fixture)? setup,
  Future<bool> Function()? onHomeReady,
}) async {
  tester.view.physicalSize = size;
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
  if (!sl.isRegistered<NotificationInboxDatasource>()) {
    sl.registerSingleton<NotificationInboxDatasource>(_EmptyNotifications());
    addTearDown(() => sl.unregister<NotificationInboxDatasource>());
  }

  // Location is intentionally unavailable: no native map or network is needed.
  const locationChannel = MethodChannel('flutter.baseflow.com/geolocator');
  final messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  messenger.setMockMethodCallHandler(locationChannel, (call) async {
    if (call.method == 'isLocationServiceEnabled') {
      return initialLocationServices ?? false;
    }
    throw PlatformException(
      code: 'unexpected_location_call',
      message: call.method,
    );
  });
  addTearDown(() => messenger.setMockMethodCallHandler(locationChannel, null));

  final fixture = _NavigationFixture();
  setup?.call(fixture);
  final client = DioClient();
  client.dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (options, handler) {
        fixture.activityRequests.add(options);
        handler.resolve(
          Response(
            requestOptions: options,
            statusCode: 200,
            data: {
              'success': true,
              'data': {
                'content': List.generate(
                  5,
                  (index) => {
                    'id': index + 10,
                    'riderId': 1,
                    'currentStatus': 'COMPLETED',
                    'isCompleted': true,
                    'fareDetail': {'totalFare': 248, 'currency': 'INR'},
                  },
                ),
                'page': 0,
                'size': 20,
                'last': true,
              },
            },
          ),
        );
      },
    ),
  );
  sl.registerSingleton<DioClient>(client);
  sl.registerSingleton<RiderApiEndpoints>(RiderApiEndpoints());
  addTearDown(() {
    sl.unregister<DioClient>();
    sl.unregister<RiderApiEndpoints>();
  });
  addTearDown(() async {
    await tester.pumpWidget(const SizedBox.shrink());
    await fixture.close();
  });
  await tester.pumpWidget(
    MultiBlocProvider(
      providers: [
        BlocProvider<AuthBloc>.value(value: fixture.auth),
        BlocProvider<UserBloc>.value(value: fixture.user),
        BlocProvider<RideHistoryBloc>.value(value: fixture.history),
        BlocProvider<OfferBloc>.value(value: fixture.offers),
        BlocProvider<RideTypeBloc>.value(value: fixture.rideTypes),
      ],
      child: MaterialApp(
        theme: customThemeData(),
        builder: (context, child) => MediaQuery(
          data: MediaQuery.of(context).copyWith(
            textScaler: TextScaler.linear(textScale),
            disableAnimations: disableAnimations,
          ),
          child: child!,
        ),
        home: MainPage(onHomeReady: onHomeReady),
      ),
    ),
  );
  if (settle) {
    await tester.pumpAndSettle();
  } else {
    await _pumpLaunchFrames(tester);
  }
  return fixture;
}

class _EmptyNotifications extends NotificationInboxDatasource {
  _EmptyNotifications() : super(Dio());

  @override
  Future<int> unreadCount(String role) async => 0;

  @override
  Future<List<InboxNotification>> fetch(String role) async => const [];

  @override
  Future<void> markRead(int id, String role) async {}
}

class _NavigationFixture {
  final activityRequests = <RequestOptions>[];
  final offerRequest = OfferRequest(
    rideTypeCode: 'BIKE',
    startFrom: DateTime(2026, 9, 12),
  );
  final auth = _AuthBloc(const AuthAuthenticated());
  final rideTypes = _RideTypeBloc(
    const RideTypeState(status: RideTypeStatus.success),
  );
  final user = _UserBloc(
    UserState(
      status: UserStatus.success,
      user: UserReponse(
        id: 1,
        authUserId: 1,
        fullName: 'Asha Rao',
        email: 'asha@example.com',
        phoneNumber: '+91 9000000000',
        profileImageUrl: null,
        active: true,
        deleted: false,
        allAddresses: [
          UserAddress(
            id: 1,
            addressType: AddressTypeEnum.WORK,
            label: 'Work',
            fullAddress: '12 Park Road, Bengaluru',
            landmark: 'Blue entrance',
            city: 'Bengaluru',
            state: 'Karnataka',
            country: 'India',
            pinCode: '560001',
            latitude: 12.97,
            longitude: 77.59,
            defaultAddress: true,
          ),
        ],
      ),
    ),
  );
  final history = _HistoryBloc(
    RideHistoryState(
      status: RideHistoryStatus.success,
      rides: List.generate(
        5,
        (index) => RideHistoryAddress(id: index, rideId: index + 10),
      ),
    ),
  );
  late final offers = _OfferBloc(
    OfferState(
      status: OfferStatus.success,
      lastRequest: offerRequest,
      offers: [
        OfferResponse(
          id: 1,
          title: 'Weekend savings',
          offerCode: 'WEEKEND20',
          description: 'Save on your next journey.',
          discountType: 'PERCENTAGE',
          discountValue: 20,
          maximumDiscount: 100,
          active: true,
        ),
      ],
    ),
  );

  Future<void> close() => Future.wait([
    auth.close(),
    user.close(),
    history.close(),
    offers.close(),
    rideTypes.close(),
  ]);
}

// These test doubles supply state and record UI events; production repositories
// are never instantiated, so navigation tests cannot make backend requests.
class _RecordingCubit<S, E> extends Cubit<S> {
  _RecordingCubit(super.initialState);
  final events = <E>[];

  void add(E event) => events.add(event);
  void update(S state) => emit(state);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _AuthBloc extends _RecordingCubit<AuthState, AuthEvent>
    implements AuthBloc {
  _AuthBloc(super.initialState);
}

class _UserBloc extends _RecordingCubit<UserState, UserEvent>
    implements UserBloc {
  _UserBloc(super.initialState);
}

class _HistoryBloc extends _RecordingCubit<RideHistoryState, RideHistoryEvent>
    implements RideHistoryBloc {
  _HistoryBloc(super.initialState);
}

class _OfferBloc extends _RecordingCubit<OfferState, OfferEvent>
    implements OfferBloc {
  _OfferBloc(super.initialState);
}

class _RideTypeBloc extends _RecordingCubit<RideTypeState, RideTypeEvent>
    implements RideTypeBloc {
  _RideTypeBloc(super.initialState);
}

class _DeferredActiveRideRepository implements RideRepository {
  final response = Completer<RideResponse>();
  var requests = 0;

  @override
  Future<RideResponse> getActiveRide(int riderId) {
    requests++;
    return response.future;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}
