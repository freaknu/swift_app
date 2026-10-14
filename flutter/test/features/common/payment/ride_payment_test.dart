import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/features/common/payment/data/payment_remote_datasource.dart';
import 'package:swift_app/features/common/payment/data/razorpay_checkout.dart';
import 'package:swift_app/features/common/payment/domain/ride_payment.dart';
import 'package:swift_app/features/common/payment/presentation/ride_payment_card.dart';
import 'package:swift_app/features/common/payment/presentation/ride_payment_controller.dart';
import 'package:swift_app/features/driver/data/model/realtime_ride_event.dart';
import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';
import 'package:swift_app/features/rider/ride/domain/repository/ride_repository.dart';
import 'package:swift_app/features/rider/ride/domain/usecases/ride_lifecycle_usecase.dart';
import 'package:swift_app/features/rider/ride/presentation/pages/ride_status_page.dart';

RidePayment _payment({
  RidePaymentStatus status = RidePaymentStatus.orderCreated,
  int rideId = 17,
  int riderId = 3,
}) => RidePayment(
  rideId: rideId,
  riderId: riderId,
  driverId: 5,
  status: status,
  razorpayKeyId: 'rzp_test_public',
  razorpayOrderId: 'order_17',
  razorpayPaymentId: status == RidePaymentStatus.completed ? 'pay_17' : null,
  amount: 24800,
  currency: 'INR',
);

Map<String, dynamic> _event({
  String status = 'COMPLETED',
  int rideId = 17,
  int riderId = 3,
  String? paymentId = 'pay_17',
}) => {
  'eventId': 'event-$status',
  'eventType': 'PAYMENT_$status',
  'paymentStatus': status,
  'rideId': rideId,
  'riderId': riderId,
  'driverId': 5,
  'razorpayOrderId': 'order_17',
  'razorpayPaymentId': paymentId,
  'amount': 24800,
  'currency': 'INR',
  'eventTime': '2026-10-13T10:00:00Z',
};

DioException _apiFailure(int status, String message) {
  final request = RequestOptions(path: 'api/v1/payments/rides/17');
  return DioException(
    requestOptions: request,
    type: DioExceptionType.badResponse,
    response: Response(
      requestOptions: request,
      statusCode: status,
      data: {'success': false, 'statusCode': status, 'message': message},
    ),
  );
}

class _ResponseAdapter implements HttpClientAdapter {
  final int status;
  final Object body;
  const _ResponseAdapter(this.status, this.body);

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async => ResponseBody.fromString(
    jsonEncode(body),
    status,
    headers: {
      'content-type': ['application/json'],
    },
  );

  @override
  void close({bool force = false}) {}
}

class _Remote implements PaymentRemoteService {
  RidePayment? snapshot = _payment();
  RidePayment order = _payment();
  RidePayment verified = _payment();
  Completer<RidePayment>? verification;
  Completer<RidePayment?>? fetch;
  Object? fetchFailure;
  Object? orderFailure;
  Object? verifyFailure;
  int gets = 0;
  int orders = 0;
  int verifies = 0;
  final List<String> verificationFields = [];

  @override
  Future<RidePayment?> getPayment(int rideId) async {
    gets++;
    if (fetchFailure != null) throw fetchFailure!;
    return fetch?.future ?? snapshot;
  }

  @override
  Future<RidePayment> createOrder(int rideId) async {
    orders++;
    if (orderFailure != null) throw orderFailure!;
    return order;
  }

  @override
  Future<RidePayment> verifyPayment({
    required int rideId,
    required String razorpayOrderId,
    required String razorpayPaymentId,
    required String razorpaySignature,
  }) async {
    verifies++;
    if (verifyFailure != null) throw verifyFailure!;
    verificationFields.addAll([
      razorpayOrderId,
      razorpayPaymentId,
      razorpaySignature,
    ]);
    return verification?.future ?? verified;
  }
}

class _Checkout implements PaymentCheckout {
  void Function(CheckoutSuccess)? success;
  void Function(String)? failure;
  RidePayment? opened;
  bool disposed = false;

  @override
  void open(
    RidePayment payment, {
    required void Function(CheckoutSuccess) onSuccess,
    required void Function(String) onFailure,
    required void Function(String) onExternalWallet,
  }) {
    opened = payment;
    success = onSuccess;
    failure = onFailure;
  }

  void succeed({String orderId = 'order_17'}) => success!(
    CheckoutSuccess(
      orderId: orderId,
      paymentId: 'pay_17',
      signature: 'signed-result',
    ),
  );

  @override
  void dispose() => disposed = true;
}

class _RideRepository implements RideRepository {
  final RideResponse ride;
  const _RideRepository(this.ride);

  @override
  Future<RideResponse> getRide(int rideId) async => ride;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  test(
    'service errors reach the UI and clear after a successful refresh',
    () async {
      final remote = _Remote()
        ..fetchFailure = _apiFailure(
          503,
          'Service currently unavailable, please try again',
        );
      final controller = RidePaymentController(
        rideId: 17,
        riderId: 3,
        remote: remote,
        checkout: _Checkout(),
      );
      addTearDown(controller.dispose);
      await controller.refresh();
      expect(
        controller.error,
        'Service currently unavailable, please try again',
      );
      remote.fetchFailure = null;
      await controller.refresh();
      expect(controller.error, isNull);
      expect(controller.completed, isFalse);
      remote.fetchFailure = _apiFailure(401, 'JWT rejected');
      await controller.refresh();
      expect(controller.error, 'Session expired. Please sign in again.');
    },
  );

  test(
    'checkout shows Razorpay setup errors and preserves them while polling',
    () async {
      final remote = _Remote()
        ..orderFailure = _apiFailure(
          503,
          'Razorpay API credentials are not configured',
        );
      final checkout = _Checkout();
      final controller = RidePaymentController(
        rideId: 17,
        riderId: 3,
        remote: remote,
        checkout: checkout,
      );
      addTearDown(controller.dispose);
      await controller.pay();
      expect(controller.error, 'Razorpay API credentials are not configured');
      expect(checkout.opened, isNull);
      await controller.refresh();
      expect(controller.error, 'Razorpay API credentials are not configured');
      remote.orderFailure = null;
      await controller.pay();
      expect(controller.error, isNull);
      expect(checkout.opened, isNotNull);
    },
  );

  test(
    'connection errors show a safe message instead of request details',
    () async {
      final remote = _Remote()
        ..fetchFailure = DioException(
          requestOptions: RequestOptions(path: '/internal-payment-route'),
          type: DioExceptionType.connectionError,
          message: 'Internal connection details',
        );
      final controller = RidePaymentController(
        rideId: 17,
        riderId: 3,
        remote: remote,
        checkout: _Checkout(),
      );
      addTearDown(controller.dispose);
      await controller.refresh();
      expect(
        controller.error,
        'Unable to connect. Check your internet connection and try again.',
      );
    },
  );

  test(
    'order processing failure permits retry and waits for captured payment',
    () async {
      final remote = _Remote()
        ..snapshot = _payment(status: RidePaymentStatus.pending)
        ..orderFailure = _apiFailure(
          500,
          'Payment processing failed; please retry',
        );
      final checkout = _Checkout();
      final controller = RidePaymentController(
        rideId: 17,
        riderId: 3,
        remote: remote,
        checkout: checkout,
      );
      addTearDown(controller.dispose);

      await controller.refresh();
      expect(controller.payment?.status, RidePaymentStatus.pending);
      expect(controller.error, isNull);
      await controller.pay();
      expect(controller.error, 'Payment processing failed; please retry');
      expect(controller.payment?.status, RidePaymentStatus.pending);
      expect(controller.canPay, isTrue);
      expect(checkout.opened, isNull);

      // After the backend recovers, retry obtains its persisted order rather
      // than treating the earlier processing error as a failed charge.
      remote.orderFailure = null;
      remote.snapshot = remote.order;
      await controller.pay();
      expect(remote.orders, 2);
      expect(checkout.opened?.razorpayOrderId, remote.order.razorpayOrderId);
      expect(controller.error, isNull);
      checkout.succeed();
      await Future<void>.delayed(Duration.zero);
      expect(remote.verifies, 1);
      expect(controller.verifying, isTrue);
      expect(controller.completed, isFalse);
      expect(controller.canPay, isFalse);

      controller.onEvent(_event());
      expect(controller.completed, isTrue);
      expect(controller.verifying, isFalse);
      expect(controller.error, isNull);
    },
  );

  test(
    'verification errors retain the cause while preventing a second charge',
    () async {
      final remote = _Remote()
        ..verifyFailure = _apiFailure(
          502,
          'Razorpay request failed; please retry',
        );
      final checkout = _Checkout();
      final controller = RidePaymentController(
        rideId: 17,
        riderId: 3,
        remote: remote,
        checkout: checkout,
      );
      addTearDown(controller.dispose);
      await controller.pay();
      checkout.succeed();
      await Future<void>.delayed(Duration.zero);
      expect(
        controller.error,
        'Razorpay request failed; please retry. Check payment status before retrying.',
      );
      expect(controller.verifying, isTrue);
      expect(controller.canPay, isFalse);
      remote.snapshot = _payment(status: RidePaymentStatus.completed);
      await controller.refresh();
      expect(controller.completed, isTrue);
      expect(controller.error, isNull);
    },
  );

  test('checkout callback verifies on server and waits for capture', () async {
    final remote = _Remote()..verification = Completer<RidePayment>();
    final checkout = _Checkout();
    final controller = RidePaymentController(
      rideId: 17,
      riderId: 3,
      remote: remote,
      checkout: checkout,
    );
    addTearDown(controller.dispose);
    await controller.pay();
    expect(checkout.opened?.amount, 24800);
    checkout.succeed();
    await Future<void>.delayed(Duration.zero);
    expect(remote.verificationFields, ['order_17', 'pay_17', 'signed-result']);
    expect(controller.verifying, isTrue);
    expect(controller.completed, isFalse);
    expect(controller.canPay, isFalse);
    remote.verification!.complete(_payment());
    await Future<void>.delayed(Duration.zero);
    expect(controller.completed, isFalse);
    expect(controller.verifying, isTrue);
    controller.onEvent(_event());
    expect(controller.completed, isTrue);
    expect(controller.verifying, isFalse);
  });

  test(
    'confirmed REST verification completes without a socket frame',
    () async {
      final remote = _Remote()
        ..verified = _payment(status: RidePaymentStatus.completed);
      final checkout = _Checkout();
      final controller = RidePaymentController(
        rideId: 17,
        riderId: 3,
        remote: remote,
        checkout: checkout,
      );
      addTearDown(controller.dispose);
      await controller.pay();
      checkout.succeed();
      await Future<void>.delayed(Duration.zero);
      expect(controller.completed, isTrue);
      expect(controller.canPay, isFalse);
    },
  );

  test(
    'forged order response and unrelated events cannot confirm a ride',
    () async {
      final remote = _Remote();
      final checkout = _Checkout();
      final controller = RidePaymentController(
        rideId: 17,
        riderId: 3,
        remote: remote,
        checkout: checkout,
      );
      addTearDown(controller.dispose);
      await controller.pay();
      checkout.succeed(orderId: 'another-order');
      controller.onEvent(_event(rideId: 99));
      controller.onEvent(_event(riderId: 99));
      controller.onEvent(_event(paymentId: null));
      controller.onEvent({'rideId': 17, 'rideStatus': 'COMPLETED'});
      await Future<void>.delayed(Duration.zero);
      expect(controller.completed, isFalse);
      expect(remote.verifies, 0);
    },
  );

  test(
    'late REST and duplicated or older events cannot undo paid status',
    () async {
      final remote = _Remote()..fetch = Completer<RidePayment?>();
      final controller = RidePaymentController(
        rideId: 17,
        riderId: 3,
        remote: remote,
        checkout: _Checkout(),
      );
      addTearDown(controller.dispose);
      final refresh = controller.refresh();
      controller.onEvent(_event());
      remote.fetch!.complete(_payment(status: RidePaymentStatus.failed));
      await refresh;
      controller.onEvent(_event());
      controller.onEvent(_event(status: 'FAILED'));
      await controller.pay();
      expect(controller.completed, isTrue);
      expect(controller.error, isNull);
      expect(remote.orders, 0);
    },
  );

  test('cancellation permits retry with the backend order', () async {
    final remote = _Remote();
    final checkout = _Checkout();
    final controller = RidePaymentController(
      rideId: 17,
      riderId: 3,
      remote: remote,
      checkout: checkout,
    );
    await controller.pay();
    checkout.failure!('Payment cancelled. You can retry.');
    await Future<void>.delayed(Duration.zero);
    expect(controller.completed, isFalse);
    expect(controller.canPay, isTrue);
    await controller.pay();
    expect(remote.orders, 2);
    expect(checkout.opened?.razorpayOrderId, 'order_17');
    controller.dispose();
    expect(checkout.disposed, isTrue);
    checkout.succeed();
    expect(remote.verifies, 0);
  });

  test(
    'payment remote validates envelope and preserves paise and proof',
    () async {
      final requests = <RequestOptions>[];
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost/'));
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (request, handler) {
            requests.add(request);
            handler.resolve(
              Response(
                requestOptions: request,
                statusCode: 200,
                data: {
                  'success': true,
                  'statusCode': 200,
                  'data': {..._event(), 'status': 'COMPLETED'},
                },
              ),
            );
          },
        ),
      );
      final datasource = PaymentRemoteDatasource(dio);
      final snapshot = await datasource.getPayment(17);
      await datasource.createOrder(17);
      final verified = await datasource.verifyPayment(
        rideId: 17,
        razorpayOrderId: 'order_17',
        razorpayPaymentId: 'pay_17',
        razorpaySignature: 'signed-result',
      );
      expect(snapshot?.amount, 24800);
      expect(verified.isCompleted, isTrue);
      expect(requests.map((r) => r.path), [
        'api/v1/payments/rides/17',
        'api/v1/payments/rides/17/order',
        'api/v1/payments/rides/17/verify',
      ]);
      expect(requests.last.data, {
        'razorpayOrderId': 'order_17',
        'razorpayPaymentId': 'pay_17',
        'razorpaySignature': 'signed-result',
      });
    },
  );

  test(
    'a missing payment record is pending and an error envelope is rejected',
    () async {
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost/'));
      var status = 404;
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (request, handler) {
            handler.resolve(
              Response(
                requestOptions: request,
                statusCode: status,
                data: {
                  'success': false,
                  'statusCode': status,
                  'message': 'Payment order has not been created for this ride',
                  'data': _event(),
                },
              ),
            );
          },
        ),
      );
      final datasource = PaymentRemoteDatasource(dio);
      expect(await datasource.getPayment(17), isNull);
      status = 200;
      await expectLater(
        datasource.getPayment(17),
        throwsA(isA<DioException>()),
      );
    },
  );

  for (final acceptErrors in [false, true]) {
    test('pending 404 works with Dio validateStatus = $acceptErrors', () async {
      final dio = Dio(BaseOptions(baseUrl: 'http://localhost/'));
      if (acceptErrors) dio.options.validateStatus = (_) => true;
      dio.httpClientAdapter = const _ResponseAdapter(404, {
        'success': false,
        'statusCode': 404,
        'message': 'Payment order has not been created for this ride',
      });
      addTearDown(dio.close);
      expect(await PaymentRemoteDatasource(dio).getPayment(17), isNull);
    });
  }

  test(
    'gateway and ride lookup 404 responses are errors, not pending payments',
    () async {
      final dio = Dio(
        BaseOptions(baseUrl: 'http://localhost/', validateStatus: (_) => true),
      );
      addTearDown(dio.close);
      final datasource = PaymentRemoteDatasource(dio);
      for (final body in [
        {
          'status': 404,
          'error': 'Not Found',
          'path': '/api/v1/payments/rides/17',
        },
        {
          'success': false,
          'statusCode': 404,
          'message': 'Ride or rider profile not found',
        },
      ]) {
        dio.httpClientAdapter = _ResponseAdapter(404, body);
        await expectLater(
          datasource.getPayment(17),
          throwsA(isA<DioException>()),
        );
      }
      final controller = RidePaymentController(
        rideId: 17,
        riderId: 3,
        remote: datasource,
        checkout: _Checkout(),
      );
      addTearDown(controller.dispose);
      await controller.refresh();
      expect(controller.error, 'Ride or rider profile not found');
    },
  );

  testWidgets('the payment card surfaces an unavailable gateway route', (
    tester,
  ) async {
    final remote = _Remote()
      ..fetchFailure = _apiFailure(
        404,
        'No static resource api/v1/payments/rides/17',
      );
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: RidePaymentCard(
            rideId: 17,
            riderId: 3,
            datasource: remote,
            checkout: _Checkout(),
          ),
        ),
      ),
    );
    await tester.pump();
    expect(
      find.text('Payment service is unavailable. Please try again.'),
      findsOneWidget,
    );
    expect(find.text('Payment successful'), findsNothing);
    await tester.pumpWidget(const SizedBox());
  });

  test('driver payment payload keeps lifecycle status empty', () {
    final event = RealtimeRideEvent.fromJson(_event());
    expect(event.rideStatus, isEmpty);
    expect(event.isFinished, isFalse);
    expect(RidePayment.fromJson(event.raw).isCompleted, isTrue);
  });

  testWidgets('completed receipt keeps receiving payment success events', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(800, 1000);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    const ride = RideResponse(
      id: 17,
      riderId: 3,
      driverId: 5,
      currentStatus: 'COMPLETED',
      fareDetail: FareDetail(totalFare: 248, currency: 'INR'),
    );
    final events = StreamController<Map<String, dynamic>>.broadcast();
    final checkout = _Checkout();
    addTearDown(events.close);
    await tester.pumpWidget(
      MaterialApp(
        home: RideStatusPage(
          ride: ride,
          riderId: 3,
          lifecycle: RideLifecycleUsecase(_RideRepository(ride)),
          realtimeEvents: events.stream,
          paymentDatasource: _Remote(),
          paymentCheckout: checkout,
          mapOverride: const SizedBox.expand(),
        ),
      ),
    );
    await tester.pump();
    expect(find.text('Trip receipt'), findsOneWidget);
    await tester.scrollUntilVisible(
      find.text('Pay with Razorpay'),
      180,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.tap(find.text('Pay with Razorpay'));
    await tester.pump();
    checkout.succeed();
    await tester.pump();
    expect(find.text('Verifying payment…'), findsOneWidget);
    expect(find.text('Payment successful'), findsNothing);
    events.add(_event());
    await tester.pump();
    expect(find.text('Payment successful'), findsOneWidget);
    expect(find.text('Payment ID: pay_17'), findsOneWidget);
    expect(find.text('Trip receipt'), findsOneWidget);
    await tester.pumpWidget(const SizedBox());
    expect(checkout.disposed, isTrue);
    expect(tester.takeException(), isNull);
  });

  testWidgets('receipt recovery refreshes snapshot on socket reconnect', (
    tester,
  ) async {
    final remote = _Remote();
    final connected = ValueNotifier(false);
    addTearDown(connected.dispose);
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: RidePaymentCard(
            rideId: 17,
            riderId: 3,
            datasource: remote,
            checkout: _Checkout(),
            connected: connected,
          ),
        ),
      ),
    );
    await tester.pump();
    expect(find.text('Payment pending'), findsOneWidget);
    remote.snapshot = _payment(status: RidePaymentStatus.completed);
    connected.value = true;
    await tester.pump();
    expect(find.text('Payment successful'), findsOneWidget);
    expect(remote.gets, 2);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets(
    'payment polling pauses, recovers on resume and stops when paid',
    (tester) async {
      final remote = _Remote();
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: RidePaymentCard(
              rideId: 17,
              riderId: 3,
              datasource: remote,
              checkout: _Checkout(),
            ),
          ),
        ),
      );
      await tester.pump();
      expect(remote.gets, 1);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      await tester.pump(const Duration(seconds: 16));
      expect(remote.gets, 1);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await tester.pump();
      expect(remote.gets, 2);
      await tester.pump(const Duration(seconds: 5));
      expect(remote.gets, 3, reason: 'Resuming starts only one polling timer.');

      remote.snapshot = _payment(status: RidePaymentStatus.completed);
      await tester.pump(const Duration(seconds: 5));
      await tester.pump();
      expect(find.text('Payment successful'), findsOneWidget);
      expect(remote.gets, 4);
      await tester.pump(const Duration(seconds: 16));
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
      tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await tester.pump();
      expect(remote.gets, 4, reason: 'Paid receipts never restart polling.');
      await tester.pumpWidget(const SizedBox());
    },
  );

  testWidgets('hidden receipt defers polling and retains payment events', (
    tester,
  ) async {
    final remote = _Remote();
    final visible = ValueNotifier(false);
    final connected = ValueNotifier(false);
    final events = StreamController<Map<String, dynamic>>.broadcast();
    addTearDown(visible.dispose);
    addTearDown(connected.dispose);
    addTearDown(events.close);
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: ValueListenableBuilder<bool>(
            valueListenable: visible,
            child: RidePaymentCard(
              rideId: 17,
              riderId: 3,
              datasource: remote,
              checkout: _Checkout(),
              connected: connected,
              events: events.stream,
            ),
            builder: (_, enabled, child) =>
                TickerMode(enabled: enabled, child: child!),
          ),
        ),
      ),
    );
    connected.value = true;
    await tester.pump(const Duration(seconds: 16));
    expect(remote.gets, 0);
    visible.value = true;
    await tester.pump();
    await tester.pump();
    expect(remote.gets, 1);
    visible.value = false;
    await tester.pump();
    await tester.pump(const Duration(seconds: 16));
    expect(remote.gets, 1);
    visible.value = true;
    await tester.pump();
    await tester.pump();
    expect(remote.gets, 2);
    await tester.pump(const Duration(seconds: 5));
    expect(remote.gets, 3);

    visible.value = false;
    await tester.pump();
    events.add(_event());
    await tester.pump();
    await tester.pump();
    visible.value = true;
    await tester.pump();
    await tester.pump(const Duration(seconds: 16));
    expect(find.text('Payment successful'), findsOneWidget);
    expect(remote.gets, 3);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('checkout verification continues while polling is paused', (
    tester,
  ) async {
    final remote = _Remote()
      ..verified = _payment(status: RidePaymentStatus.completed);
    final checkout = _Checkout();
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: RidePaymentCard(
            rideId: 17,
            riderId: 3,
            datasource: remote,
            checkout: checkout,
          ),
        ),
      ),
    );
    await tester.pump();
    await tester.tap(find.text('Pay with Razorpay'));
    await tester.pump();
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    checkout.succeed();
    await tester.pump();
    expect(remote.verifies, 1);
    expect(find.text('Payment successful'), findsOneWidget);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump(const Duration(seconds: 16));
    expect(remote.gets, 1);
    expect(remote.orders, 1);
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('reopening a paid receipt recovers success without checkout', (
    tester,
  ) async {
    final remote = _Remote()
      ..snapshot = _payment(status: RidePaymentStatus.completed);
    final checkout = _Checkout();
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: RidePaymentCard(
            rideId: 17,
            riderId: 3,
            datasource: remote,
            checkout: checkout,
          ),
        ),
      ),
    );
    await tester.pump();
    expect(find.text('Payment successful'), findsOneWidget);
    expect(find.text('Pay with Razorpay'), findsNothing);
    expect(checkout.opened, isNull);
    await tester.pumpWidget(const SizedBox());
  });
}
