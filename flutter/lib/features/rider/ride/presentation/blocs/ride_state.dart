import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';

enum RideStatus { initial, loading, success, failure }

class RideState {
  final RideStatus status;
  final RideResponse? ride;
  final String? errorMessage;
  final int? statusCode;

  const RideState({
    this.status = RideStatus.initial,
    this.ride,
    this.errorMessage,
    this.statusCode,
  });

  bool get isLoading => status == RideStatus.loading;

  bool get isSuccess => status == RideStatus.success;

  bool get hasError => status == RideStatus.failure;

  RideState copyWith({
    RideStatus? status,
    RideResponse? ride,
    String? errorMessage,
    int? statusCode,
    bool clearRide = false,
    bool clearError = false,
  }) {
    return RideState(
      status: status ?? this.status,
      ride: clearRide ? null : ride ?? this.ride,
      errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
      statusCode: statusCode ?? this.statusCode,
    );
  }
}
