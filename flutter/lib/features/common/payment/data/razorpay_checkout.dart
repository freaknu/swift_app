import 'package:flutter/foundation.dart';
import 'package:razorpay_flutter/razorpay_flutter.dart';
import 'package:swift_app/features/common/payment/domain/ride_payment.dart';
import 'package:swift_app/themes/app_colors.dart';

class CheckoutSuccess {
  final String orderId;
  final String paymentId;
  final String signature;

  const CheckoutSuccess({
    required this.orderId,
    required this.paymentId,
    required this.signature,
  });
}

abstract interface class PaymentCheckout {
  void open(
    RidePayment payment, {
    required void Function(CheckoutSuccess) onSuccess,
    required void Function(String) onFailure,
    required void Function(String) onExternalWallet,
  });
  void dispose();
}

class RazorpayCheckout implements PaymentCheckout {
  Razorpay? _razorpay;

  @override
  void open(
    RidePayment payment, {
    required void Function(CheckoutSuccess) onSuccess,
    required void Function(String) onFailure,
    required void Function(String) onExternalWallet,
  }) {
    if (kIsWeb ||
        (defaultTargetPlatform != TargetPlatform.android &&
            defaultTargetPlatform != TargetPlatform.iOS)) {
      throw UnsupportedError('Razorpay checkout requires Android or iOS.');
    }
    _razorpay?.clear();
    final razorpay = _razorpay = Razorpay();
    razorpay.on(Razorpay.EVENT_PAYMENT_SUCCESS, (
      PaymentSuccessResponse response,
    ) {
      final orderId = response.orderId;
      final paymentId = response.paymentId;
      final signature = response.signature;
      if (orderId == null || paymentId == null || signature == null) {
        onFailure('Payment response incomplete. Check payment status.');
        return;
      }
      onSuccess(
        CheckoutSuccess(
          orderId: orderId,
          paymentId: paymentId,
          signature: signature,
        ),
      );
    });
    razorpay.on(
      Razorpay.EVENT_PAYMENT_ERROR,
      (PaymentFailureResponse response) => onFailure(
        response.code == Razorpay.PAYMENT_CANCELLED
            ? 'Payment cancelled. You can retry.'
            : 'Payment could not be completed. Check status or retry.',
      ),
    );
    razorpay.on(
      Razorpay.EVENT_EXTERNAL_WALLET,
      (ExternalWalletResponse response) =>
          onExternalWallet(response.walletName ?? 'wallet'),
    );
    razorpay.open({
      'key': payment.razorpayKeyId,
      'order_id': payment.razorpayOrderId,
      'amount': payment.amount,
      'currency': payment.currency,
      'name': 'Swift',
      'description': 'Ride ${payment.rideId}',
      'notes': {'rideId': payment.rideId.toString()},
      'theme': {
        'color':
            '#${AppColors.primaryColor.toARGB32().toRadixString(16).substring(2).toUpperCase()}',
      },
    });
  }

  @override
  void dispose() {
    _razorpay?.clear();
    _razorpay = null;
  }
}
