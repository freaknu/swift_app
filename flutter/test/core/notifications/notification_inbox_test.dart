import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/core/notifications/notification_inbox_datasource.dart';
import 'package:swift_app/core/notifications/notification_inbox_page.dart';
import 'package:swift_app/core/notifications/push_notification_service.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/themes/custom_theme_data.dart';

class _FakeInbox extends NotificationInboxDatasource {
  _FakeInbox() : super(Dio());

  final completer = Completer<List<InboxNotification>>();
  int unread = 1;
  int unreadCalls = 0;
  Completer<int>? unreadResult;
  int? readId;
  String? readRole;

  @override
  Future<List<InboxNotification>> fetch(String role) => completer.future;

  @override
  Future<int> unreadCount(String role) {
    unreadCalls++;
    return unreadResult?.future ?? Future.value(unread);
  }

  @override
  Future<void> markRead(int id, String role) async {
    readId = id;
    readRole = role;
    unread = 0;
  }
}

void main() {
  testWidgets(
    'notification settings and inbox stay reachable in a short large-text viewport',
    (tester) async {
      tester.view.physicalSize = const Size(320, 480);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      sl.registerSingleton<PushNotificationService>(_DeniedPush());
      addTearDown(sl.unregister<PushNotificationService>);
      final datasource = _FakeInbox();
      await tester.pumpWidget(
        MaterialApp(
          theme: customThemeData(),
          builder: (context, child) => MediaQuery(
            data: MediaQuery.of(context).copyWith(
              textScaler: const TextScaler.linear(2),
              padding: const EdgeInsets.only(top: 32, bottom: 24),
            ),
            child: child!,
          ),
          home: NotificationInboxPage(role: 'RIDER', datasource: datasource),
        ),
      );
      datasource.completer.complete(const [
        InboxNotification(
          id: 1,
          type: 'RIDE_UPDATE',
          title: 'Your ride is ready',
          body: 'Your driver has arrived at your pickup point.',
          read: false,
        ),
      ]);
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      await tester.scrollUntilVisible(
        find.text('Open settings').hitTestable(),
        150,
      );
      expect(find.text('Open settings').hitTestable(), findsOneWidget);
      await tester.scrollUntilVisible(
        find.text('Your ride is ready').hitTestable(),
        150,
      );
      expect(find.text('Your ride is ready').hitTestable(), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('changing bell roles discards the previous unread request', (
    tester,
  ) async {
    final previous = _FakeInbox()..unreadResult = Completer<int>();
    final next = _FakeInbox()..unread = 2;
    addTearDown(() => tester.pumpWidget(const SizedBox.shrink()));
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: NotificationBell(role: 'RIDER', datasource: previous),
        ),
      ),
    );
    expect(previous.unreadCalls, 1);
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: NotificationBell(role: 'DRIVER', datasource: next),
        ),
      ),
    );
    await tester.pump();
    expect(find.text('2'), findsOneWidget);
    previous.unreadResult!.complete(9);
    await tester.pump();
    await tester.pump();
    expect(find.text('2'), findsOneWidget);
    expect(find.text('9'), findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'inbox builds visible rows lazily and keeps them during refresh',
    (tester) async {
      final datasource = _RefreshingInbox();
      final items = List.generate(
        100,
        (index) => InboxNotification(
          id: index,
          type: 'RIDE_UPDATE',
          title: 'Notice $index',
          body: 'A ride update is ready to view.',
          read: false,
        ),
      );
      await tester.pumpWidget(
        MaterialApp(
          home: NotificationInboxPage(role: 'RIDER', datasource: datasource),
        ),
      );
      datasource.loads.first.complete(items);
      await tester.pumpAndSettle();
      expect(find.byType(ListTile).evaluate().length, lessThan(20));
      expect(find.text('Notice 99'), findsNothing);

      await tester.scrollUntilVisible(
        find.text('Notice 20'),
        350,
        scrollable: find.byType(Scrollable).first,
      );
      final scrollable = tester.state<ScrollableState>(
        find.byType(Scrollable).first,
      );
      final offset = scrollable.position.pixels;
      final refresh = tester
          .widget<RefreshIndicator>(find.byType(RefreshIndicator))
          .onRefresh();
      await tester.pump();
      expect(datasource.loads, hasLength(2));
      expect(find.text('Notice 20'), findsOneWidget);
      expect(scrollable.position.pixels, offset);
      datasource.loads.last.complete(items);
      await refresh;
      await tester.pumpAndSettle();
      expect(scrollable.position.pixels, offset);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'bell avoids overlapping requests and stops polling in background',
    (tester) async {
      _resumeApp(tester);
      final datasource = _FakeInbox()..unreadResult = Completer<int>();
      addTearDown(() async {
        await tester.pumpWidget(const SizedBox.shrink());
        tester.binding.handleAppLifecycleStateChanged(
          AppLifecycleState.resumed,
        );
      });
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: NotificationBell(role: 'RIDER', datasource: datasource),
          ),
        ),
      );
      expect(datasource.unreadCalls, 1);
      await tester.pump(const Duration(seconds: 31));
      expect(datasource.unreadCalls, 1);
      datasource.unreadResult!.complete(2);
      await tester.pump();
      await tester.pump();
      expect(find.text('2'), findsOneWidget);

      _pauseApp(tester);
      await tester.pump(const Duration(seconds: 61));
      expect(datasource.unreadCalls, 1);
      _resumeApp(tester);
      await tester.pump();
      expect(datasource.unreadCalls, 2);
    },
  );

  testWidgets(
    'a hidden bell starts polling only when its tab becomes visible',
    (tester) async {
      final visible = ValueNotifier(false);
      final datasource = _FakeInbox();
      addTearDown(() async {
        await tester.pumpWidget(const SizedBox.shrink());
        visible.dispose();
      });
      await tester.pumpWidget(
        MaterialApp(
          home: ValueListenableBuilder<bool>(
            valueListenable: visible,
            builder: (context, enabled, child) =>
                TickerMode(enabled: enabled, child: child!),
            child: Scaffold(
              body: NotificationBell(role: 'RIDER', datasource: datasource),
            ),
          ),
        ),
      );
      await tester.pump(const Duration(seconds: 31));
      expect(datasource.unreadCalls, 0);
      visible.value = true;
      await tester.pump();
      expect(datasource.unreadCalls, 1);
      visible.value = false;
      await tester.pump();
      await tester.pump(const Duration(seconds: 61));
      expect(datasource.unreadCalls, 1);
    },
  );

  testWidgets('connected push status wraps on narrow screens with large text', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: MediaQuery(
          data: const MediaQueryData(textScaler: TextScaler.linear(3)),
          child: Scaffold(
            body: SizedBox(
              width: 240,
              child: PushStatusBanner(
                status: const PushDeliveryStatus(PushDeliveryState.ready),
                onRetry: () {},
                onOpenSettings: () {},
              ),
            ),
          ),
        ),
      ),
    );
    expect(find.text('Push alerts connected'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  test(
    'inbox datasource uses owned role endpoints and parses response',
    () async {
      final requests = <RequestOptions>[];
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost/'));
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (request, handler) {
            requests.add(request);
            final data = request.path.endsWith('/unread-count')
                ? 2
                : request.path.endsWith('/read')
                ? {'id': 11}
                : [
                    {
                      'id': 11,
                      'rideId': 42,
                      'type': 'DRIVER_ASSIGNED',
                      'title': 'Driver found',
                      'body': 'On the way',
                      'read': false,
                      'createdAt': '2026-10-10T09:00:00',
                    },
                  ];
            handler.resolve(
              Response(
                requestOptions: request,
                statusCode: 200,
                data: {'success': true, 'data': data},
              ),
            );
          },
        ),
      );
      final datasource = NotificationInboxDatasource(dio);

      final items = await datasource.fetch('RIDER');
      expect(items.single.rideId, 42);
      expect(items.single.read, isFalse);
      expect(await datasource.unreadCount('RIDER'), 2);
      await datasource.markRead(11, 'RIDER');
      expect(requests.map((request) => request.method), [
        'GET',
        'GET',
        'PATCH',
      ]);
      expect(requests[0].queryParameters, {'role': 'RIDER', 'limit': 50});
      expect(requests[2].path, contains('/11/read'));
      expect(requests[2].queryParameters['role'], 'RIDER');
    },
  );

  testWidgets('bell loads unread badge and inbox opens a marked ride item', (
    tester,
  ) async {
    final datasource = _FakeInbox();
    InboxNotification? opened;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: NotificationBell(
            role: 'DRIVER',
            datasource: datasource,
            onOpenNotification: (item) => opened = item,
          ),
        ),
      ),
    );
    await tester.pump();
    expect(find.text('1'), findsOneWidget);

    await tester.tap(find.byType(IconButton));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));
    expect(tester.takeException(), isNull);
    expect(find.byType(NotificationInboxPage), findsOneWidget);
    expect(find.text('Notifications'), findsOneWidget);

    datasource.completer.complete(const [
      InboxNotification(
        id: 11,
        rideId: 42,
        type: 'RIDE_OFFER',
        title: 'New ride request',
        body: 'Pickup nearby',
        read: false,
      ),
    ]);
    await tester.pumpAndSettle();
    expect(find.text('New ride request'), findsOneWidget);
    await tester.tap(find.text('New ride request'));
    await tester.pumpAndSettle();

    expect(datasource.readId, 11);
    expect(datasource.readRole, 'DRIVER');
    expect(opened?.rideId, 42);
    expect(find.text('1'), findsNothing);
  });

  testWidgets('denied push permission explains recovery without hiding inbox', (
    tester,
  ) async {
    var openedSettings = false;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: PushStatusBanner(
            status: const PushDeliveryStatus(
              PushDeliveryState.permissionDenied,
            ),
            onRetry: () => fail('Retry is not the denied-permission action'),
            onOpenSettings: () => openedSettings = true,
          ),
        ),
      ),
    );
    expect(find.text('Push alerts are off'), findsOneWidget);
    expect(
      find.textContaining('Updates remain available here'),
      findsOneWidget,
    );
    await tester.tap(find.text('Open settings'));
    expect(openedSettings, isTrue);
  });

  testWidgets('registration failure offers retry and bounded code', (
    tester,
  ) async {
    var retried = false;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: PushStatusBanner(
            status: const PushDeliveryStatus(
              PushDeliveryState.failed,
              code: 'registration_http_503',
            ),
            onRetry: () => retried = true,
            onOpenSettings: () => fail('Settings is not the failure action'),
          ),
        ),
      ),
    );
    expect(find.text('Push alerts are not connected'), findsOneWidget);
    expect(find.text('Code: registration_http_503'), findsOneWidget);
    await tester.tap(find.text('Retry'));
    expect(retried, isTrue);
  });

  testWidgets('registered device reports connected without exposing a token', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: PushStatusBanner(
            status: const PushDeliveryStatus(PushDeliveryState.ready),
            onRetry: () => fail('A connected device should not retry'),
            onOpenSettings: () =>
                fail('A connected device should not open settings'),
          ),
        ),
      ),
    );
    expect(find.text('Push alerts connected'), findsOneWidget);
    expect(find.text('Retry'), findsNothing);
  });
}

class _DeniedPush implements PushNotificationService {
  @override
  PushDeliveryStatus get status =>
      const PushDeliveryStatus(PushDeliveryState.permissionDenied);

  @override
  Stream<PushDeliveryStatus> get statusChanges => const Stream.empty();

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _RefreshingInbox extends NotificationInboxDatasource {
  _RefreshingInbox() : super(Dio());

  final loads = <Completer<List<InboxNotification>>>[];

  @override
  Future<List<InboxNotification>> fetch(String role) {
    final load = Completer<List<InboxNotification>>();
    loads.add(load);
    return load.future;
  }
}

void _pauseApp(WidgetTester tester) {
  for (final state in [
    AppLifecycleState.inactive,
    AppLifecycleState.hidden,
    AppLifecycleState.paused,
  ]) {
    tester.binding.handleAppLifecycleStateChanged(state);
  }
}

void _resumeApp(WidgetTester tester) {
  if (tester.binding.lifecycleState == AppLifecycleState.paused) {
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
  }
  tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
}
