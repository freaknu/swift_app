import 'package:swift_app/features/rider/home/domain/entity/ride_history_address.dart';

enum RideHistoryStatus { initial, loading, success, failure }

class RideHistoryState {
  final RideHistoryStatus status;

  final List<RideHistoryAddress> rides;

  final String? message;

  final int? statusCode;

  const RideHistoryState({
    this.status = RideHistoryStatus.initial,
    this.rides = const [],
    this.message,
    this.statusCode,
  });

  RideHistoryState copyWith({
    RideHistoryStatus? status,
    List<RideHistoryAddress>? rides,
    String? message,
    int? statusCode,
    bool clearMessage = false,
  }) {
    return RideHistoryState(
      status: status ?? this.status,

      rides: rides ?? this.rides,

      message: clearMessage ? null : message ?? this.message,

      statusCode: statusCode ?? this.statusCode,
    );
  }
}
