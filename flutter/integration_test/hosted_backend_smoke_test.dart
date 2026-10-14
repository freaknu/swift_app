import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:swift_app/API/api_config.dart';
import 'package:swift_app/API/dio_client.dart';

/// Read-only hosted transport validation on a real Android/iOS Flutter runtime.
///
/// Supply the five-minute synthetic JWT using a private JSON file:
/// flutter test integration_test/hosted_backend_smoke_test.dart -d emulator-5554
///   --dart-define-from-file=/private/path/hosted-smoke-defines.json
///
/// Uses the application's endpoint resolver, Dio authentication interceptor, and
/// STOMP dependency. It never starts normal app lifecycle/location services,
/// creates accounts/rides, sends chat messages, or invokes payment mutations.
void main() {
  final binding = IntegrationTestWidgetsFlutterBinding.ensureInitialized();
  const token = String.fromEnvironment('HOSTED_SMOKE_TOKEN');
  final outcomes = <String, Object>{};

  Future<void> display(WidgetTester tester, String status) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          backgroundColor: const Color(0xFF06141B),
          body: Center(
            child: Text(
              status,
              textDirection: TextDirection.ltr,
              style: const TextStyle(color: Color(0xFFCCD0CF), fontSize: 18),
            ),
          ),
        ),
      ),
    );
    await tester.pump();
  }

  tearDownAll(() {
    binding.reportData = {'hostedBackendSmoke': outcomes};
  });

  testWidgets('Android hosted HTTPS uses app endpoints and protects the API', (
    tester,
  ) async {
    await display(tester, 'Checking Swift HTTPS');
    expect(ApiConfig.baseUrl, 'https://swift-app.duckdns.org/');
    expect(ApiConfig.webSocketUrl, 'wss://swift-app.duckdns.org/ws-native');
    final dio = DioClient().dio;
    try {
      final health = await dio.get<Map<String, dynamic>>('actuator/health');
      expect(health.statusCode, 200);
      expect(health.data?['status'], 'UP');
      final config = await dio.get<Map<String, dynamic>>(
        'v3/api-docs/swagger-config',
      );
      expect(config.statusCode, 200);
      expect(config.data?['urls'], isA<List<dynamic>>());
      final documents = config.data!['urls'] as List<dynamic>;
      expect(documents.length, greaterThanOrEqualTo(11));
      final docs = await dio.get<Map<String, dynamic>>('driver/v3/api-docs');
      expect(docs.statusCode, 200);
      expect(docs.data?['openapi'], isA<String>());
      expect((docs.data?['servers'] as List<dynamic>).first['url'], '/');
      expect(
        docs.data?['components']['securitySchemes']['gatewayBearer']['scheme'],
        'bearer',
      );
      final anonymous = await dio.get<dynamic>('api/v1/vehicle/types');
      expect(anonymous.statusCode, 401);
      outcomes['https'] = true;
      outcomes['health'] = 'UP';
      outcomes['swaggerServiceCount'] = documents.length;
      outcomes['anonymousApiRejected'] = true;
    } finally {
      dio.close(force: true);
    }
  });

  testWidgets('Android native WSS rejects unauthenticated STOMP CONNECT', (
    tester,
  ) async {
    await display(tester, 'Checking Swift WSS authentication');
    final rejected = Completer<void>();
    final client = StompClient(
      config: StompConfig(
        url: ApiConfig.webSocketUrl,
        reconnectDelay: Duration.zero,
        heartbeatIncoming: Duration.zero,
        heartbeatOutgoing: Duration.zero,
        connectionTimeout: const Duration(seconds: 15),
        onConnect: (_) {
          if (!rejected.isCompleted) {
            rejected.completeError(StateError('Anonymous STOMP was accepted'));
          }
        },
        onStompError: (_) {
          if (!rejected.isCompleted) rejected.complete();
        },
        onWebSocketError: (_) {
          if (!rejected.isCompleted) {
            rejected.completeError(StateError('WSS transport failed'));
          }
        },
        onDebugMessage: (_) {},
      ),
    );
    try {
      client.activate();
      await rejected.future.timeout(const Duration(seconds: 20));
      outcomes['unauthenticatedStompRejected'] = true;
    } finally {
      client.deactivate();
    }
  });

  testWidgets(
    'Android app Dio and STOMP accept the short-lived synthetic JWT',
    (tester) async {
      await display(tester, 'Checking authenticated Swift connection');
      expect(
        token.isNotEmpty,
        isTrue,
        reason: 'A synthetic JWT define is required',
      );
      final dio = DioClient(
        accessTokenReader: () async => token,
        authUserIdReader: () async => 987654321,
      ).dio;
      final connected = Completer<void>();
      final errors = <String>[];
      final client = StompClient(
        config: StompConfig(
          url: ApiConfig.webSocketUrl,
          stompConnectHeaders: const {'Authorization': 'Bearer $token'},
          webSocketConnectHeaders: const {'Authorization': 'Bearer $token'},
          reconnectDelay: Duration.zero,
          heartbeatIncoming: Duration.zero,
          heartbeatOutgoing: Duration.zero,
          connectionTimeout: const Duration(seconds: 15),
          onConnect: (_) {
            if (!connected.isCompleted) connected.complete();
          },
          onStompError: (_) {
            errors.add('STOMP authentication or subscription failed');
            if (!connected.isCompleted) {
              connected.completeError(
                StateError('STOMP authentication failed'),
              );
            }
          },
          onWebSocketError: (_) {
            errors.add('WSS transport failed');
            if (!connected.isCompleted) {
              connected.completeError(StateError('WSS transport failed'));
            }
          },
          onDebugMessage: (_) {},
        ),
      );
      try {
        final catalog = await dio.get<Map<String, dynamic>>(
          'api/v1/vehicle/types',
        );
        expect(catalog.statusCode, 200);
        expect(catalog.data?['success'], isTrue);
        expect(catalog.data?['data'], isA<List<dynamic>>());
        client.activate();
        await connected.future.timeout(const Duration(seconds: 20));
        final unsubscribe = client.subscribe(
          destination: '/user/queue/deployment-smoke',
          callback: (_) => errors.add('Unexpected deployment queue message'),
        );
        await Future<void>.delayed(const Duration(seconds: 1));
        expect(client.connected, isTrue);
        expect(errors, isEmpty);
        unsubscribe();
        await Future<void>.delayed(const Duration(milliseconds: 500));
        expect(errors, isEmpty);
        outcomes['authenticatedCatalog'] = true;
        outcomes['authenticatedStompConnected'] = true;
        outcomes['privateSubscribeUnsubscribe'] = true;
        outcomes['applicationSendFrames'] = 0;
        await display(tester, 'Swift backend checks passed');
      } finally {
        dio.close(force: true);
        client.deactivate();
      }
    },
  );
}
