enum RidePaymentStatus { pending, orderCreated, completed, failed }

class RidePayment {
  final int rideId;
  final int riderId;
  final int? driverId;
  final RidePaymentStatus status;
  final String? razorpayKeyId;
  final String? razorpayOrderId;
  final String? razorpayPaymentId;
  final int amount;
  final String currency;

  const RidePayment({
    required this.rideId,
    required this.riderId,
    this.driverId,
    required this.status,
    this.razorpayKeyId,
    this.razorpayOrderId,
    this.razorpayPaymentId,
    required this.amount,
    required this.currency,
  });

  bool get isCompleted =>
      status == RidePaymentStatus.completed &&
      razorpayPaymentId != null &&
      razorpayPaymentId!.isNotEmpty;

  bool get canCheckout =>
      status == RidePaymentStatus.orderCreated &&
      amount > 0 &&
      razorpayKeyId?.isNotEmpty == true &&
      razorpayOrderId?.isNotEmpty == true;

  String get amountLabel => currency == 'INR'
      ? '₹${(amount / 100).toStringAsFixed(2)}'
      : '$currency ${(amount / 100).toStringAsFixed(2)}';

  factory RidePayment.fromJson(Map<dynamic, dynamic> json) {
    final rideId = int.tryParse('${json['rideId'] ?? ''}');
    final riderId = int.tryParse('${json['riderId'] ?? ''}');
    final amount = int.tryParse('${json['amount'] ?? ''}');
    final status = switch (json['status'] ?? json['paymentStatus']) {
      'PENDING' => RidePaymentStatus.pending,
      'ORDER_CREATED' => RidePaymentStatus.orderCreated,
      'COMPLETED' => RidePaymentStatus.completed,
      'FAILED' => RidePaymentStatus.failed,
      _ => null,
    };
    final currency = json['currency']?.toString().toUpperCase();
    if (rideId == null ||
        riderId == null ||
        amount == null ||
        amount < 0 ||
        status == null ||
        currency != 'INR') {
      throw const FormatException('Invalid ride payment');
    }
    String? value(String key) {
      final value = json[key]?.toString().trim();
      return value == null || value.isEmpty ? null : value;
    }

    return RidePayment(
      rideId: rideId,
      riderId: riderId,
      driverId: int.tryParse('${json['driverId'] ?? ''}'),
      status: status,
      amount: amount,
      currency: currency!,
      razorpayKeyId: value('razorpayKeyId'),
      razorpayOrderId: value('razorpayOrderId'),
      razorpayPaymentId: value('razorpayPaymentId'),
    );
  }
}
