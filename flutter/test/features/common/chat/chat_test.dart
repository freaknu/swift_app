import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/features/common/chat/data/datasource/chat_realtime_service.dart';
import 'package:swift_app/features/common/chat/data/datasource/chat_remote_datasource.dart';
import 'package:swift_app/features/common/chat/domain/chat_availability.dart';
import 'package:swift_app/features/common/chat/domain/entity/chat_message.dart';
import 'package:swift_app/features/common/chat/domain/entity/chat_session.dart';
import 'package:swift_app/features/common/chat/presentation/pages/chat_page.dart';
import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';
import 'package:swift_app/features/rider/ride/domain/repository/ride_repository.dart';
import 'package:swift_app/features/rider/ride/domain/usecases/ride_lifecycle_usecase.dart';
import 'package:swift_app/features/rider/ride/presentation/pages/ride_status_page.dart';
import 'package:swift_app/themes/custom_theme_data.dart';

void main() {
  testWidgets(
    'multiline chat remains aligned above the keyboard with large text',
    (tester) async {
      tester.view.physicalSize = const Size(320, 568);
      tester.view.devicePixelRatio = 1;
      tester.view.viewInsets = const FakeViewPadding(bottom: 240);
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      addTearDown(tester.view.resetViewInsets);
      final realtime = _FakeChatRealtime();
      final remote = _FakeChatRemote(
        const ChatSession(sessionId: 11, rideId: 42, rideStatus: 'STARTED'),
      );
      addTearDown(realtime.close);
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
          home: ChatPage(
            rideId: 42,
            currentUserId: 7,
            role: 'RIDER',
            rideStatus: 'STARTED',
            datasource: remote,
            realtime: realtime,
          ),
        ),
      );
      await tester.pumpAndSettle();
      await tester.enterText(
        find.byType(TextField),
        'First line\nSecond line\nThird line\nFourth line',
      );
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      expect(find.byTooltip('Send message').hitTestable(), findsOneWidget);
      expect(
        tester.getRect(find.byTooltip('Send message')).bottom,
        lessThanOrEqualTo(328),
      );
      await tester.tap(find.byTooltip('Send message'));
      await tester.pumpAndSettle();
      expect(
        remote.sent.single.messageContent,
        'First line\nSecond line\nThird line\nFourth line',
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'long conversations open at latest and incoming events preserve reading position',
    (tester) async {
      final remote = _FakeChatRemote(
        const ChatSession(sessionId: 11, rideId: 42, rideStatus: 'STARTED'),
      );
      remote.history.addAll(
        List.generate(
          100,
          (index) => ChatMessage(
            messageId: '$index',
            sessionId: 11,
            sentById: 9,
            messageContent: 'Message $index',
          ),
        ),
      );
      final realtime = _FakeChatRealtime();
      addTearDown(realtime.close);
      await tester.pumpWidget(
        MaterialApp(
          home: ChatPage(
            rideId: 42,
            currentUserId: 7,
            role: 'RIDER',
            rideStatus: 'STARTED',
            datasource: remote,
            realtime: realtime,
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('Message 99'), findsOneWidget);
      expect(find.text('Message 0'), findsNothing);
      await tester.drag(find.byType(ListView), const Offset(0, 400));
      await tester.pumpAndSettle();
      final controller = tester
          .widget<ListView>(find.byType(ListView))
          .controller!;
      expect(controller.offset, greaterThan(80));
      final anchor = find.textContaining('Message ').hitTestable().first;
      final anchorText = tester.widget<Text>(anchor).data!;
      final anchorTop = tester.getTopLeft(anchor).dy;
      realtime.emit(
        const ChatMessage(
          messageId: '100',
          sessionId: 11,
          sentById: 9,
          messageContent: 'A new incoming message',
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text(anchorText).hitTestable(), findsOneWidget);
      expect(
        tester.getTopLeft(find.text(anchorText)).dy,
        closeTo(anchorTop, 1),
      );
      expect(controller.offset, greaterThan(80));

      final id = anchorText.split(' ').last;
      realtime.emit(
        ChatMessage(
          messageId: id,
          sessionId: 11,
          sentById: 9,
          messageContent: '$anchorText edited',
          edited: true,
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('$anchorText edited').hitTestable(), findsOneWidget);
      expect(controller.offset, greaterThan(80));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'sending preserves a new draft typed while the request is pending',
    (tester) async {
      final remote = _FakeChatRemote(
        const ChatSession(sessionId: 11, rideId: 42, rideStatus: 'STARTED'),
      )..sendResult = Completer<void>();
      final realtime = _FakeChatRealtime();
      addTearDown(realtime.close);
      await tester.pumpWidget(
        MaterialApp(
          home: ChatPage(
            rideId: 42,
            currentUserId: 7,
            role: 'RIDER',
            rideStatus: 'STARTED',
            datasource: remote,
            realtime: realtime,
          ),
        ),
      );
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField), 'First message');
      await tester.tap(find.byTooltip('Send message'));
      await tester.pump();
      await tester.enterText(find.byType(TextField), 'My next draft');
      remote.sendResult!.complete();
      await tester.pumpAndSettle();
      expect(remote.sent.single.messageContent, 'First message');
      expect(
        tester.widget<TextField>(find.byType(TextField)).controller!.text,
        'My next draft',
      );
    },
  );

  testWidgets(
    'chat pauses status polling in background and stops after completion',
    (tester) async {
      _resumeApp(tester);
      final remote = _FakeChatRemote(
        const ChatSession(sessionId: 11, rideId: 42, rideStatus: 'STARTED'),
      );
      final realtime = _FakeChatRealtime();
      var statusCalls = 0;
      var status = 'STARTED';
      addTearDown(() async {
        await tester.pumpWidget(const SizedBox.shrink());
        await realtime.close();
        tester.binding.handleAppLifecycleStateChanged(
          AppLifecycleState.resumed,
        );
      });
      await tester.pumpWidget(
        MaterialApp(
          home: ChatPage(
            rideId: 42,
            currentUserId: 7,
            role: 'RIDER',
            rideStatus: 'STARTED',
            datasource: remote,
            realtime: realtime,
            rideStatusReader: () async {
              statusCalls++;
              return status;
            },
          ),
        ),
      );
      await tester.pumpAndSettle();
      await tester.pump(const Duration(seconds: 6));
      expect(statusCalls, 1);
      _pauseApp(tester);
      await tester.pump(const Duration(seconds: 16));
      expect(statusCalls, 1);
      status = 'COMPLETED';
      _resumeApp(tester);
      await tester.pumpAndSettle();
      expect(statusCalls, 2);
      expect(find.byType(TextField), findsNothing);
      await tester.pump(const Duration(seconds: 16));
      expect(statusCalls, 2);
    },
  );

  test(
    'chat lifecycle is available only after assignment and before finish',
    () {
      expect(ChatAvailability.isAvailable('MATCHING'), isFalse);
      expect(ChatAvailability.isAvailable('DRIVER_ASSIGNED'), isTrue);
      expect(ChatAvailability.isAvailable('DRIVER_ARRIVED'), isTrue);
      expect(ChatAvailability.isAvailable('STARTED'), isTrue);
      expect(ChatAvailability.isAvailable('CANCELLED'), isFalse);
      expect(ChatAvailability.isAvailable('COMPLETED'), isFalse);
      expect(ChatAvailability.isTerminal('COMPLETED'), isTrue);
    },
  );

  test('chat messages use the backend STOMP payload contract', () {
    final message = ChatMessage.create(
      sentById: 7,
      sessionId: 11,
      content: 'I am at the pickup point',
    );
    expect(message.messageId, isNotEmpty);
    expect(message.toJson(), containsPair('sentById', 7));
    expect(message.toJson(), containsPair('sessionId', 11));
    expect(message.toJson(), containsPair('contentType', 'TEXT'));
    expect(message.toJson(), containsPair('messageType', 'MESSAGE'));
  });

  test(
    'notification reply sends a REST chat message with the signed-in user',
    () async {
      final remote = _FakeChatRemote(
        const ChatSession(sessionId: 11, rideId: 42, rideStatus: 'STARTED'),
      );
      await sendChatNotificationReply(
        payload: const {'sessionId': 11, 'messageId': 'incoming-1'},
        text: 'I am outside',
        datasource: remote,
        authenticatedUserId: 7,
      );

      expect(remote.sent.single.sentById, 7);
      expect(remote.sent.single.sessionId, 11);
      expect(remote.sent.single.messageContent, 'I am outside');
      expect(remote.sent.single.replyOfMessageId, 'incoming-1');
    },
  );

  testWidgets('accepted ride opens a usable composer and sends a message', (
    tester,
  ) async {
    final remote = _FakeChatRemote(
      const ChatSession(
        sessionId: 11,
        rideId: 42,
        riderId: 7,
        driverId: 9,
        rideStatus: 'DRIVER_ASSIGNED',
      ),
    );
    final realtime = _FakeChatRealtime();
    addTearDown(realtime.close);
    await tester.pumpWidget(
      MaterialApp(
        theme: customThemeData(),
        home: ChatPage(
          rideId: 42,
          currentUserId: 7,
          role: 'RIDER',
          rideStatus: 'DRIVER_ASSIGNED',
          riderId: 7,
          driverId: 9,
          datasource: remote,
          realtime: realtime,
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Ride chat'), findsOneWidget);
    expect(find.byType(TextField), findsOneWidget);
    await tester.enterText(find.byType(TextField), 'On my way');
    await tester.tap(find.byTooltip('Send message'));
    await tester.pump();

    expect(remote.sent.single.messageContent, 'On my way');
    expect(find.text('On my way'), findsOneWidget);
  });

  testWidgets('resuming chat reloads messages missed while backgrounded', (
    tester,
  ) async {
    final remote = _FakeChatRemote(
      const ChatSession(sessionId: 11, rideId: 42, rideStatus: 'STARTED'),
    );
    remote.history.add(
      const ChatMessage(
        messageId: '1',
        sessionId: 11,
        messageContent: 'Before pause',
      ),
    );
    final realtime = _FakeChatRealtime();
    addTearDown(realtime.close);
    await tester.pumpWidget(
      MaterialApp(
        home: ChatPage(
          rideId: 42,
          currentUserId: 7,
          role: 'RIDER',
          rideStatus: 'STARTED',
          datasource: remote,
          realtime: realtime,
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Before pause'), findsOneWidget);
    expect(remote.getMessagesCalls, 1);

    remote.history.add(
      const ChatMessage(
        messageId: '2',
        sessionId: 11,
        messageContent: 'While backgrounded',
      ),
    );
    _resumeApp(tester);
    await tester.pump();
    await tester.pump();
    expect(find.text('While backgrounded'), findsOneWidget);
    expect(remote.getMessagesCalls, 2);
    expect(realtime.connectCalls, 2);
  });

  testWidgets('cancelled ride does not expose a composer', (tester) async {
    final remote = _FakeChatRemote(null);
    final realtime = _FakeChatRealtime();
    addTearDown(realtime.close);
    await tester.pumpWidget(
      MaterialApp(
        theme: customThemeData(),
        home: ChatPage(
          rideId: 42,
          currentUserId: 7,
          role: 'RIDER',
          rideStatus: 'CANCELLED',
          datasource: remote,
          realtime: realtime,
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.textContaining('Chat becomes available'), findsOneWidget);
    expect(find.byType(TextField), findsNothing);
    expect(remote.getSessionCalls, 0);
  });

  testWidgets(
    'closed backend session disables chat even with a stale ride status',
    (tester) async {
      final remote = _FakeChatRemote(
        const ChatSession(
          sessionId: 11,
          rideId: 42,
          rideStatus: 'STARTED',
          active: false,
        ),
      );
      final realtime = _FakeChatRealtime();
      addTearDown(realtime.close);
      await tester.pumpWidget(
        MaterialApp(
          theme: customThemeData(),
          home: ChatPage(
            rideId: 42,
            currentUserId: 7,
            role: 'RIDER',
            rideStatus: 'STARTED',
            datasource: remote,
            realtime: realtime,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.textContaining('Chat ended'), findsOneWidget);
      expect(find.byType(TextField), findsNothing);
    },
  );

  testWidgets('accepted rider active ride exposes the chat entry point', (
    tester,
  ) async {
    final realtime = _FakeChatRealtime();
    addTearDown(realtime.close);
    final ride = const RideResponse(
      id: 42,
      riderId: 7,
      driverId: 9,
      currentStatus: 'DRIVER_ASSIGNED',
      pickupLatitude: 12.97,
      pickupLongitude: 77.59,
      destinationLatitude: 12.98,
      destinationLongitude: 77.60,
    );
    await tester.pumpWidget(
      MaterialApp(
        theme: customThemeData(),
        home: RideStatusPage(
          ride: ride,
          riderId: 7,
          lifecycle: RideLifecycleUsecase(_FakeRideRepository(ride)),
          realtimeEvents: const Stream<Map<String, dynamic>>.empty(),
          mapOverride: const SizedBox.expand(),
          chatDatasource: _FakeChatRemote(
            const ChatSession(
              sessionId: 11,
              rideId: 42,
              riderId: 7,
              driverId: 9,
              rideStatus: 'DRIVER_ASSIGNED',
            ),
          ),
          chatRealtime: realtime,
        ),
      ),
    );
    await tester.pump();

    expect(find.text('Chat with driver'), findsOneWidget);
  });
}

class _FakeChatRemote implements ChatRemoteService {
  final ChatSession? session;
  int getSessionCalls = 0;
  final sent = <ChatMessage>[];
  final history = <ChatMessage>[];
  int getMessagesCalls = 0;
  Completer<void>? sendResult;

  _FakeChatRemote(this.session);

  @override
  Future<ChatSession?> getSessionByRide(int rideId) async {
    getSessionCalls++;
    return session;
  }

  @override
  Future<List<ChatMessage>> getMessages(int sessionId) async {
    getMessagesCalls++;
    return List.of(history);
  }

  @override
  Future<void> sendMessage(ChatMessage message) async {
    sent.add(message);
    await sendResult?.future;
  }
}

class _FakeChatRealtime implements ChatRealtimeTransport {
  final _events = StreamController<ChatMessage>.broadcast();
  final sent = <ChatMessage>[];
  int connectCalls = 0;

  @override
  Stream<ChatMessage> get messages => _events.stream;

  @override
  void connect(int sessionId) => connectCalls++;

  @override
  void disconnect() {}

  @override
  Future<bool> send(ChatMessage message) async {
    sent.add(message);
    return true;
  }

  Future<void> close() => _events.close();

  void emit(ChatMessage message) => _events.add(message);
}

class _FakeRideRepository implements RideRepository {
  final RideResponse ride;

  _FakeRideRepository(this.ride);

  @override
  Future<RideResponse> getRide(int rideId) async => ride;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
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
