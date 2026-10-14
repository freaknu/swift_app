import 'package:swift_app/features/common/fare/domain/entity/fare_estimate_response.dart';
import 'package:swift_app/features/common/fare/domain/entity/fare_estimate_request.dart';

enum FareStatus { initial, loading, success, failure }

class FareState {
  final FareStatus status;
  final FareEstimateResponse? fareEstimate;
  final String? message;
  final FareEstimateRequest? lastRequest;

  const FareState({
    this.status = FareStatus.initial,
    this.fareEstimate,
    this.message,
    this.lastRequest,
  });

  /// A successful response belongs only to the route and promotion it quoted.
  bool isQuoteFor(FareEstimateRequest request) {
    final quoted = lastRequest;
    return status == FareStatus.success &&
        fareEstimate != null &&
        quoted != null &&
        quoted.offerId == request.offerId &&
        quoted.rideType == request.rideType &&
        quoted.pickupLatitude == request.pickupLatitude &&
        quoted.pickupLongitude == request.pickupLongitude &&
        quoted.destinationLatitude == request.destinationLatitude &&
        quoted.destinationLongitude == request.destinationLongitude;
  }

  FareState copyWith({
    FareStatus? status,
    FareEstimateResponse? fareEstimate,
    String? message,
    bool clearFare = false,
    bool clearMessage = false,
    FareEstimateRequest? lastRequest,
    bool clearRequest = false,
  }) {
    return FareState(
      status: status ?? this.status,
      fareEstimate: clearFare ? null : fareEstimate ?? this.fareEstimate,
      message: clearMessage ? null : message ?? this.message,
      lastRequest: clearRequest ? null : lastRequest ?? this.lastRequest,
    );
  }
}
