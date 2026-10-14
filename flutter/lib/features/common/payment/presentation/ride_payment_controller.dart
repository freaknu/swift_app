import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:swift_app/API/api_error_message.dart';
import 'package:swift_app/features/common/payment/data/payment_remote_datasource.dart';
import 'package:swift_app/features/common/payment/data/razorpay_checkout.dart';
import 'package:swift_app/features/common/payment/domain/ride_payment.dart';

/// Checkout success only submits a verification request. The authoritative
/// REST snapshot or PAYMENT_COMPLETED event is what confirms captured funds.
class RidePaymentController extends ChangeNotifier {
  final int rideId;
  final int riderId;
  final PaymentRemoteService remote;
  final PaymentCheckout checkout;
  RidePayment? payment;
  bool loading = false;
  bool checkoutOpen = false;
  bool verifying = false;
  String? error;
  String? _refreshError;
  bool _disposed = false;
  bool _verifyingRequest = false;
  int _revision = 0;
  DateTime? _lastEventTime;
  final Set<String> _eventIds = {};

  RidePaymentController({
    required this.rideId,
    required this.riderId,
    required this.remote,
    required this.checkout,
  });

  bool get completed => payment?.isCompleted == true;
  bool get canPay => !completed && !loading && !checkoutOpen && !verifying;

  Future<void> refresh() async {
    if (_disposed || loading || completed) return;
    final revision = _revision;
    loading = true;
    _notify();
    try {
      final snapshot = await remote.getPayment(rideId);
      if (_disposed) return;
      if (_refreshError != null && error == _refreshError) error = null;
      _refreshError = null;
      if (snapshot != null && (revision == _revision || snapshot.isCompleted)) {
        _apply(snapshot);
      }
    } catch (failure) {
      if (!_disposed && !completed) {
        final message = _failureMessage(
          failure,
          fallback: 'Payment status unavailable. Check status or retry.',
        );
        error = verifying ? _confirmationMessage(message) : message;
        _refreshError = error;
      }
    } finally {
      loading = false;
      _notify();
    }
  }

  Future<void> pay() async {
    if (_disposed || !canPay) return;
    loading = true;
    error = null;
    _refreshError = null;
    _notify();
    try {
      // The server owns fare, order creation and idempotency. Reuse its order
      // on retries instead of creating an untrusted client-side amount.
      final order = await remote.createOrder(rideId);
      if (_disposed) return;
      _apply(order);
      if (completed) return;
      if (!order.canCheckout) {
        throw const FormatException('Payment order is not ready');
      }
      checkoutOpen = true;
      checkout.open(
        order,
        onSuccess: (result) => unawaited(_verify(result)),
        onFailure: _onFailure,
        onExternalWallet: (_) {
          if (_disposed || completed) return;
          checkoutOpen = false;
          error = 'Complete payment in your wallet, then check payment status.';
          _notify();
          unawaited(refresh());
        },
      );
    } catch (failure) {
      if (!_disposed && !completed) {
        checkoutOpen = false;
        error = failure is UnsupportedError
            ? 'Pay with Razorpay using the Android or iOS app.'
            : _failureMessage(
                failure,
                fallback: 'Could not open payment. Check status or retry.',
              );
      }
    } finally {
      loading = false;
      _notify();
    }
  }

  Future<void> _verify(CheckoutSuccess result) async {
    if (_disposed || completed || _verifyingRequest) return;
    checkoutOpen = false;
    if (result.orderId != payment?.razorpayOrderId ||
        result.paymentId.isEmpty ||
        result.signature.isEmpty) {
      _onFailure('Payment response did not match this ride. Check status.');
      return;
    }
    verifying = true;
    _verifyingRequest = true;
    error = null;
    _notify();
    try {
      final verified = await remote.verifyPayment(
        rideId: rideId,
        razorpayOrderId: result.orderId,
        razorpayPaymentId: result.paymentId,
        razorpaySignature: result.signature,
      );
      if (_disposed) return;
      _apply(verified);
    } catch (failure) {
      if (!_disposed && !completed) {
        final message = _failureMessage(
          failure,
          fallback: 'Payment confirmation is pending.',
        );
        error = _confirmationMessage(message);
      }
    } finally {
      _verifyingRequest = false;
      _notify();
      if (!_disposed && !completed) unawaited(refresh());
    }
  }

  void _onFailure(String message) {
    if (_disposed || completed) return;
    checkoutOpen = false;
    verifying = false;
    error = message;
    _notify();
    // Cancellation or a network failure can race with a captured webhook.
    unawaited(refresh());
  }

  void onEvent(Map<String, dynamic> event) {
    if (_disposed || completed) return;
    final eventType = event['eventType'];
    final expectedStatus = switch (eventType) {
      'PAYMENT_ORDER_CREATED' => 'ORDER_CREATED',
      'PAYMENT_COMPLETED' => 'COMPLETED',
      'PAYMENT_FAILED' => 'FAILED',
      _ => null,
    };
    if (expectedStatus == null || event['paymentStatus'] != expectedStatus) {
      return;
    }
    final eventId = event['eventId']?.toString();
    if (eventId != null && _eventIds.contains(eventId)) return;
    final eventTime = DateTime.tryParse('${event['eventTime'] ?? ''}');
    if (expectedStatus != 'COMPLETED' &&
        _lastEventTime != null &&
        eventTime != null &&
        eventTime.isBefore(_lastEventTime!)) {
      return;
    }
    try {
      final incoming = RidePayment.fromJson(event);
      if (!_belongsToRide(incoming)) return;
      if (expectedStatus == 'COMPLETED' && !incoming.isCompleted) return;
      _apply(incoming);
      _revision++;
      _lastEventTime = eventTime ?? _lastEventTime;
      if (eventId != null) {
        _eventIds.add(eventId);
        if (_eventIds.length > 100) _eventIds.remove(_eventIds.first);
      }
      _notify();
      // Order events deliberately do not include the checkout key. Fetch the
      // authenticated snapshot rather than treating event data as an order.
      if (!completed) unawaited(refresh());
    } on FormatException {
      // Ignore malformed or unrelated socket messages; REST remains fallback.
    }
  }

  bool _belongsToRide(RidePayment value) =>
      value.rideId == rideId && value.riderId == riderId;

  String _confirmationMessage(String message) =>
      '${message.endsWith('.') ? message : '$message.'} Check payment status before retrying.';

  String _failureMessage(Object failure, {required String fallback}) {
    if (failure is! DioException) return fallback;
    final status = failure.response?.statusCode;
    if (status == 401 || status == 403) return userFacingDioMessage(failure);
    final body = failure.response?.data;
    // Display the API's concise error, never Dio's message with URLs or traces.
    if (body is Map &&
        body['success'] == false &&
        body['statusCode'] == status &&
        body['message'] is String) {
      final message = (body['message'] as String).trim();
      if (message.isNotEmpty &&
          message.length <= 250 &&
          !message.contains('\n') &&
          (status != 404 || message == 'Ride or rider profile not found')) {
        return message;
      }
    }
    if (status == 404) {
      return 'Payment service is unavailable. Please try again.';
    }
    return userFacingDioMessage(failure);
  }

  void _apply(RidePayment value) {
    if (!_belongsToRide(value)) {
      throw const FormatException('Payment belongs to another ride');
    }
    if (completed && !value.isCompleted) return;
    payment = value;
    if (value.isCompleted) {
      verifying = false;
      checkoutOpen = false;
      error = null;
    } else if (value.status == RidePaymentStatus.failed) {
      verifying = false;
      checkoutOpen = false;
      error = 'Payment failed. You can retry.';
    }
  }

  void _notify() {
    if (!_disposed) notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    checkout.dispose();
    super.dispose();
  }
}
