class FareEstimateRequest {
  /// Optional promotion applied when the ride is requested.
  ///
  /// The backend accepts a null offer while still returning a normal estimate,
  /// so riders do not have to wait for offers before seeing a price.
  final int? offerId;
  final double pickupLatitude;
  final double pickupLongitude;
  final double destinationLatitude;
  final double destinationLongitude;

  /// Optional ride type code (e.g. BIKE, AUTO, STANDARD) for per-type pricing.
  final String? rideType;

  const FareEstimateRequest({
    required this.offerId,
    required this.pickupLatitude,
    required this.pickupLongitude,
    required this.destinationLatitude,
    required this.destinationLongitude,
    this.rideType,
  });
}
