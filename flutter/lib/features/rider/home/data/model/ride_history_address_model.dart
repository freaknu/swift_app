import 'package:swift_app/features/rider/home/domain/entity/ride_history_address.dart';

class RideHistoryAddressModel extends RideHistoryAddress {
  RideHistoryAddressModel({
    super.id,
    super.rideId,
    super.pickUpLatitude,
    super.pickUpLongitude,
    super.destinationLatitude,
    super.destinationLongitude,
  });

  // ============================================================
  // FROM JSON
  // ============================================================

  factory RideHistoryAddressModel.fromJson(Map<String, dynamic> json) {
    return RideHistoryAddressModel(
      id: _parseInt(json['id']),
      rideId: _parseInt(json['rideId']),

      pickUpLatitude: _parseDouble(json['pickUpLatitude']),

      pickUpLongitude: _parseDouble(json['pickUpLongitude']),

      destinationLatitude: _parseDouble(json['destinationLatitude']),

      destinationLongitude: _parseDouble(json['destinationLongitude']),
    );
  }

  // ============================================================
  // TO JSON
  // ============================================================

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'rideId': rideId,
      'pickUpLatitude': pickUpLatitude,
      'pickUpLongitude': pickUpLongitude,
      'destinationLatitude': destinationLatitude,
      'destinationLongitude': destinationLongitude,
    };
  }

  // ============================================================
  // PARSE INT
  // ============================================================

  static int? _parseInt(dynamic value) {
    if (value == null) {
      return null;
    }

    if (value is int) {
      return value;
    }

    return int.tryParse(value.toString());
  }

  // ============================================================
  // PARSE DOUBLE
  // ============================================================

  static double? _parseDouble(dynamic value) {
    if (value == null) {
      return null;
    }

    if (value is num) {
      return value.toDouble();
    }

    return double.tryParse(value.toString());
  }
}
