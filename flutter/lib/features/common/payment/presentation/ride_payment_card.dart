import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:swift_app/constants/widgets/primary_button_widget.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/payment/data/payment_remote_datasource.dart';
import 'package:swift_app/features/common/payment/data/razorpay_checkout.dart';
import 'package:swift_app/features/common/payment/domain/ride_payment.dart';
import 'package:swift_app/features/common/payment/presentation/ride_payment_controller.dart';

class RidePaymentCard extends StatefulWidget {
  final int rideId;
  final int riderId;
  final PaymentRemoteService datasource;
  final PaymentCheckout? checkout;
  final Stream<Map<String, dynamic>>? events;
  final ValueListenable<bool>? connected;

  const RidePaymentCard({
    super.key,
    required this.rideId,
    required this.riderId,
    required this.datasource,
    this.checkout,
    this.events,
    this.connected,
  });

  @override
  State<RidePaymentCard> createState() => _RidePaymentCardState();
}

class _RidePaymentCardState extends State<RidePaymentCard>
    with WidgetsBindingObserver {
  late final RidePaymentController _controller;
  StreamSubscription<Map<String, dynamic>>? _events;
  Timer? _timer;
  bool _foreground = true;
  bool _tickerEnabled = true;
  bool _dependenciesReady = false;

  bool get _pollingActive =>
      _foreground && _tickerEnabled && !_controller.completed;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    final lifecycle = WidgetsBinding.instance.lifecycleState;
    _foreground = lifecycle == null || lifecycle == AppLifecycleState.resumed;
    _controller = RidePaymentController(
      rideId: widget.rideId,
      riderId: widget.riderId,
      remote: widget.datasource,
      checkout: widget.checkout ?? RazorpayCheckout(),
    )..addListener(_changed);
    _events = widget.events?.listen(_controller.onEvent);
    widget.connected?.addListener(_onConnectionChanged);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final wasActive = _dependenciesReady && _pollingActive;
    _tickerEnabled = TickerMode.valuesOf(context).enabled;
    _dependenciesReady = true;
    _syncPolling(refresh: !wasActive);
  }

  void _syncPolling({bool refresh = false}) {
    if (!_pollingActive) {
      _timer?.cancel();
      _timer = null;
      return;
    }
    _timer ??= Timer.periodic(const Duration(seconds: 5), (_) {
      if (_pollingActive) unawaited(_controller.refresh());
    });
    if (refresh) unawaited(_controller.refresh());
  }

  void _changed() {
    if (mounted) setState(() {});
    _syncPolling();
  }

  void _onConnectionChanged() {
    if (_pollingActive && widget.connected?.value == true) {
      unawaited(_controller.refresh());
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    final wasActive = _pollingActive;
    _foreground = state == AppLifecycleState.resumed;
    _syncPolling(refresh: !wasActive);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    widget.connected?.removeListener(_onConnectionChanged);
    _timer?.cancel();
    _events?.cancel();
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final payment = _controller.payment;
    final completed = _controller.completed;
    final title = completed
        ? 'Payment successful'
        : _controller.verifying
        ? 'Verifying payment…'
        : payment?.status == RidePaymentStatus.failed
        ? 'Payment failed'
        : 'Payment pending';
    final description = completed
        ? 'Your ride payment is confirmed.'
        : _controller.verifying
        ? 'Waiting for payment confirmation. This page updates automatically.'
        : _controller.checkoutOpen
        ? 'Complete your payment in Razorpay.'
        : payment?.status == RidePaymentStatus.orderCreated
        ? 'Your Razorpay order is ready. Pay securely below.'
        : 'Pay for your completed ride with Razorpay.';
    return Semantics(
      liveRegion: true,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: theme.colorScheme.surface,
          border: Border.all(color: theme.dividerColor),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                AppIcon(
                  completed ? SwiftIcons.circleCheck : SwiftIcons.creditCard,
                  size: 20,
                  color: theme.colorScheme.primary,
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(title, style: theme.textTheme.titleMedium),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(description, style: theme.textTheme.bodySmall),
            if (payment != null) ...[
              const SizedBox(height: 8),
              Text(payment.amountLabel, style: theme.textTheme.titleMedium),
            ],
            if (completed) ...[
              const SizedBox(height: 6),
              Text(
                'Payment ID: ${payment!.razorpayPaymentId}',
                style: theme.textTheme.bodySmall,
              ),
            ] else ...[
              if (_controller.error != null) ...[
                const SizedBox(height: 8),
                Text(
                  _controller.error!,
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: theme.colorScheme.error,
                  ),
                ),
              ],
              const SizedBox(height: 12),
              if (!_controller.verifying && !_controller.checkoutOpen)
                AppPrimaryButton(
                  text: payment?.status == RidePaymentStatus.failed
                      ? 'Retry payment'
                      : 'Pay with Razorpay',
                  onPressed: _controller.canPay ? _controller.pay : null,
                  loading: _controller.loading,
                ),
              TextButton(
                onPressed: _controller.loading ? null : _controller.refresh,
                child: const Text('Check payment status'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
