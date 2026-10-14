import 'package:swift_app/features/common/fare/domain/entity/fare_estimate_response.dart';

enum FareStatus { initial, loading, success, failure }

class FareState {
  final FareStatus status;
  final FareEstimateResponse? fareEstimate;
  final String? message;

  const FareState({
    this.status = FareStatus.initial,
    this.fareEstimate,
    this.message,
  });

  FareState copyWith({
    FareStatus? status,
    FareEstimateResponse? fareEstimate,
    String? message,
    bool clearFare = false,
  }) {
    return FareState(
      status: status ?? this.status,
      fareEstimate: clearFare ? null : fareEstimate ?? this.fareEstimate,
      message: message ?? this.message,
    );
  }
}
