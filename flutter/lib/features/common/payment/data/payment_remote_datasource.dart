import 'package:dio/dio.dart';
import 'package:swift_app/features/common/payment/domain/ride_payment.dart';

abstract interface class PaymentRemoteService {
  Future<RidePayment?> getPayment(int rideId);
  Future<RidePayment> createOrder(int rideId);
  Future<RidePayment> verifyPayment({
    required int rideId,
    required String razorpayOrderId,
    required String razorpayPaymentId,
    required String razorpaySignature,
  });
}

class PaymentRemoteDatasource implements PaymentRemoteService {
  final Dio dio;

  const PaymentRemoteDatasource(this.dio);

  String _path(int rideId) => 'api/v1/payments/rides/$rideId';

  @override
  Future<RidePayment?> getPayment(int rideId) async {
    try {
      final response = await dio.get<dynamic>(_path(rideId));
      if (_isPendingPayment(response)) return null;
      return _payment(response);
    } on DioException catch (failure) {
      // Also support Dio clients that reject non-2xx responses themselves.
      if (_isPendingPayment(failure.response)) return null;
      rethrow;
    }
  }

  // A 404 from the gateway or ride lookup is an error. Only a payment-service
  // response explicitly saying the record is pending should keep polling.
  bool _isPendingPayment(Response<dynamic>? response) {
    final body = response?.data;
    return response?.statusCode == 404 &&
        body is Map &&
        body['success'] == false &&
        body['statusCode'] == 404 &&
        body['message'] == 'Payment order has not been created for this ride';
  }

  @override
  Future<RidePayment> createOrder(int rideId) async =>
      _payment(await dio.post<dynamic>('${_path(rideId)}/order'));

  @override
  Future<RidePayment> verifyPayment({
    required int rideId,
    required String razorpayOrderId,
    required String razorpayPaymentId,
    required String razorpaySignature,
  }) async => _payment(
    await dio.post<dynamic>(
      '${_path(rideId)}/verify',
      data: {
        'razorpayOrderId': razorpayOrderId,
        'razorpayPaymentId': razorpayPaymentId,
        'razorpaySignature': razorpaySignature,
      },
    ),
  );

  RidePayment _payment(Response<dynamic> response) {
    final body = response.data;
    final statusCode = response.statusCode ?? 0;
    if (statusCode < 200 ||
        statusCode >= 300 ||
        body is! Map ||
        body['success'] != true) {
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        type: DioExceptionType.badResponse,
      );
    }
    final data = body['data'];
    if (data is! Map) throw const FormatException('Invalid payment response');
    return RidePayment.fromJson(data);
  }
}
