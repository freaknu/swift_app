import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_home_ready_datasource.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/widgets/auth_home_ready_reporter.dart';
import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_response.dart';

void main() {
  test(
    'failed acknowledgement preserves restart state and retry clears it',
    () async {
      var calls = 0;
      final fixture = await _fixture((request, handler) {
        calls++;
        handler.resolve(
          Response(
            requestOptions: request,
            statusCode: calls == 1 ? 503 : 200,
            data: {'success': calls != 1, 'data': calls != 1},
          ),
        );
      });
      await fixture.local.saveAuthUser(_user('pending-token'));
      expect(await fixture.api.acknowledge(), isFalse);
      expect(fixture.local.getSavedUser()?.otpCompletionToken, 'pending-token');

      // Recreating the datasource simulates an app launch with the saved session.
      final restarted = AuthHomeReadyDatasource(
        dioClient: fixture.client,
        endpoints: RiderApiEndpoints(),
        authLocalDatasource: fixture.local,
      );
      expect(await restarted.acknowledge(), isTrue);
      expect(fixture.local.otpCompletionToken, isNull);
      expect(
        fixture.requests.last.headers['Authorization'],
        'Bearer test-access',
      );
      expect(fixture.requests.last.data, {
        'otpCompletionToken': 'pending-token',
      });
      expect(await restarted.acknowledge(), isTrue);
      expect(calls, 2);
    },
  );

  test('a late acknowledgement cannot consume a newer sign-in', () async {
    final release = Completer<void>();
    var calls = 0;
    final fixture = await _fixture((request, handler) async {
      calls++;
      if (calls == 1) await release.future;
      handler.resolve(
        Response(
          requestOptions: request,
          statusCode: 200,
          data: {'success': true, 'data': true},
        ),
      );
    });
    await fixture.local.saveAuthUser(_user('old-token'));
    final first = fixture.api.acknowledge();
    await Future<void>.delayed(Duration.zero);
    final duplicate = fixture.api.acknowledge();
    await fixture.local.saveAuthUser(_user('new-token'));
    final nextSession = fixture.api.acknowledge();
    release.complete();
    expect(await first, isTrue);
    expect(await duplicate, isTrue);
    expect(await nextSession, isTrue);
    expect(fixture.requests.map((request) => request.data), [
      {'otpCompletionToken': 'old-token'},
      {'otpCompletionToken': 'new-token'},
    ]);
    expect(fixture.local.otpCompletionToken, isNull);
  });

  testWidgets('home reports after readiness and retries without blocking UI', (
    tester,
  ) async {
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    final ready = ValueNotifier(false);
    var calls = 0;
    addTearDown(ready.dispose);
    await tester.pumpWidget(
      MaterialApp(
        home: ValueListenableBuilder<bool>(
          valueListenable: ready,
          builder: (context, ready, _) => AuthHomeReadyReporter(
            ready: ready,
            onReady: () async => ++calls > 1,
            child: const Scaffold(body: Text('Home is usable')),
          ),
        ),
      ),
    );
    expect(calls, 0);
    ready.value = true;
    expect(calls, 0);
    await tester.pump();
    expect(calls, 1);
    expect(find.text('Home is usable'), findsOneWidget);
    await tester.pump(const Duration(seconds: 5));
    await tester.pump();
    expect(calls, 2);
    await tester.pump(const Duration(seconds: 15));
    expect(calls, 2);
    await tester.pumpWidget(const SizedBox.shrink());
    expect(tester.takeException(), isNull);
  });

  testWidgets('an authenticated home behind the OTP route cannot acknowledge', (
    tester,
  ) async {
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    final ready = ValueNotifier(false);
    final navigator = GlobalKey<NavigatorState>();
    var calls = 0;
    addTearDown(ready.dispose);
    await tester.pumpWidget(
      MaterialApp(
        navigatorKey: navigator,
        home: ValueListenableBuilder<bool>(
          valueListenable: ready,
          builder: (context, ready, _) => AuthHomeReadyReporter(
            ready: ready,
            onReady: () async {
              calls++;
              return true;
            },
            child: const Scaffold(body: Text('Home')),
          ),
        ),
      ),
    );
    unawaited(
      navigator.currentState!.push(
        MaterialPageRoute<void>(
          builder: (_) => const Scaffold(body: Text('OTP route')),
        ),
      ),
    );
    await tester.pumpAndSettle();
    ready.value = true;
    await tester.pumpAndSettle();
    expect(calls, 0);
    navigator.currentState!.pop();
    await tester.pumpAndSettle();
    expect(calls, 1);
    await tester.pumpWidget(const SizedBox.shrink());
    expect(tester.takeException(), isNull);
  });
}

SignupResponse _user(String completionToken) => SignupResponse(
  userId: 42,
  name: 'Fixture',
  email: 'rider@example.com',
  accountLocked: false,
  deviceLocked: false,
  pinCode: 0,
  accessToken: 'test-access',
  refreshToken: 'test-refresh',
  role: 'RIDER',
  active: true,
  otpCompletionToken: completionToken,
);

Future<_Fixture> _fixture(
  FutureOr<void> Function(RequestOptions, RequestInterceptorHandler) respond,
) async {
  SharedPreferences.setMockInitialValues({});
  final local = AuthLocalDatasource(
    preferences: await SharedPreferences.getInstance(),
  );
  final client = DioClient(accessTokenReader: () async => local.accessToken);
  final requests = <RequestOptions>[];
  client.dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (request, handler) async {
        requests.add(request);
        await respond(request, handler);
      },
    ),
  );
  return _Fixture(
    local,
    client,
    AuthHomeReadyDatasource(
      dioClient: client,
      endpoints: RiderApiEndpoints(),
      authLocalDatasource: local,
    ),
    requests,
  );
}

class _Fixture {
  _Fixture(this.local, this.client, this.api, this.requests);
  final AuthLocalDatasource local;
  final DioClient client;
  final AuthHomeReadyDatasource api;
  final List<RequestOptions> requests;
}
