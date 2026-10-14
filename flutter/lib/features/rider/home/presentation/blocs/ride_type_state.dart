import 'package:swift_app/features/rider/home/domain/entity/ride_type.dart';

enum RideTypeStatus { initial, loading, success, failure }

class RideTypeState {
  final RideTypeStatus status;
  final List<RideType> rideTypes;
  final int? selectedRideTypeId;
  final String? message;
  final int? statusCode;

  const RideTypeState({
    this.status = RideTypeStatus.initial,
    this.rideTypes = const [],
    this.selectedRideTypeId,
    this.message,
    this.statusCode,
  });

  RideTypeState copyWith({
    RideTypeStatus? status,
    List<RideType>? rideTypes,
    int? selectedRideTypeId,
    String? message,
    int? statusCode,
    bool clearMessage = false,
    bool clearSelectedRideType = false,
  }) {
    return RideTypeState(
      status: status ?? this.status,
      rideTypes: rideTypes ?? this.rideTypes,

      selectedRideTypeId: clearSelectedRideType
          ? null
          : selectedRideTypeId ?? this.selectedRideTypeId,

      message: clearMessage ? null : message ?? this.message,

      statusCode: statusCode ?? this.statusCode,
    );
  }
}
