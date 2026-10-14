abstract class RideTypeEvent {
  const RideTypeEvent();
}

// ============================================================
// FETCH
// ============================================================

class FetchRideTypes extends RideTypeEvent {
  const FetchRideTypes();
}

// ============================================================
// SELECT
// ============================================================

class SelectRideType extends RideTypeEvent {
  final int rideTypeId;

  const SelectRideType({required this.rideTypeId});
}
