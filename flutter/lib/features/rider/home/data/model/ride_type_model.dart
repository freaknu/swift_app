import 'package:swift_app/constants/enums/ride_type_enum.dart';
import 'package:swift_app/features/rider/home/domain/entity/ride_type.dart';

class RideTypeModel extends RideType {
  RideTypeModel({
    required super.id,
    required super.code,
    required super.name,
    required super.description,
    required super.rideType,
    required super.active,
    required super.sortOrder,
    required super.maxPassengers,
    required super.scheduledSupported,
    required super.cashSupported,
    required super.onlinePaymentSupported,
  });

  /// The shared vehicle catalog is the source of rider choices. Its `code`
  /// is sent unchanged with the ride request so matching can use the exact
  /// vehicle type selected by the rider. The catalog has no capacity or
  /// payment metadata, so those details remain unspecified in the UI.
  factory RideTypeModel.fromVehicleTypeJson(
    Map<String, dynamic> json,
    int index,
  ) {
    final code = json['code']?.toString().trim().toUpperCase() ?? '';
    return RideTypeModel(
      id: index + 1,
      code: code,
      name: json['name']?.toString().trim().isNotEmpty == true
          ? json['name'].toString().trim()
          : code.replaceAll('_', ' '),
      description: '',
      rideType: switch (code) {
        'BIKE' || 'ELECTRIC_BIKE' || 'DELIVERY_BIKE' => RideTypeEnum.BIKE,
        'SCOOTER' => RideTypeEnum.SCOOTER,
        'AUTO_RICKSHAW' => RideTypeEnum.AUTO,
        'SUV' => RideTypeEnum.SUV,
        'PREMIUM' || 'PREMIUM_SUV' => RideTypeEnum.PREMIUM,
        'LUXURY' => RideTypeEnum.LUXURY,
        'ELECTRIC_CAR' => RideTypeEnum.ELECTRIC,
        'CARPOOL' => RideTypeEnum.POOL,
        'DELIVERY_VAN' => RideTypeEnum.DELIVERY,
        _ => RideTypeEnum.STANDARD,
      },
      active: true,
      sortOrder: index,
      maxPassengers: 0,
      scheduledSupported: false,
      cashSupported: false,
      onlinePaymentSupported: false,
    );
  }

  // ============================================================
  // FROM JSON
  // ============================================================

  factory RideTypeModel.fromJson(Map<String, dynamic> json) {
    final String rideTypeValue =
        json['rideType']?.toString().toUpperCase() ?? '';

    final RideTypeEnum rideType = RideTypeEnum.values.firstWhere(
      (type) => type.name == rideTypeValue,
      orElse: () => RideTypeEnum.STANDARD,
    );

    return RideTypeModel(
      // --------------------------------------------------------
      // ID
      // --------------------------------------------------------
      id: json['id'] is int
          ? json['id'] as int
          : int.tryParse(json['id']?.toString() ?? '') ?? 0,

      // --------------------------------------------------------
      // CODE
      // --------------------------------------------------------
      code: json['code']?.toString() ?? '',

      // --------------------------------------------------------
      // NAME
      // --------------------------------------------------------
      name: json['name']?.toString() ?? '',

      // --------------------------------------------------------
      // DESCRIPTION
      // --------------------------------------------------------
      description: json['description']?.toString() ?? '',

      // --------------------------------------------------------
      // RIDE TYPE
      // --------------------------------------------------------
      rideType: rideType,

      // --------------------------------------------------------
      // ACTIVE
      // --------------------------------------------------------
      active: json['active'] as bool? ?? false,

      // --------------------------------------------------------
      // SORT ORDER
      // --------------------------------------------------------
      sortOrder: json['sortOrder'] is int
          ? json['sortOrder'] as int
          : int.tryParse(json['sortOrder']?.toString() ?? '') ?? 0,

      // --------------------------------------------------------
      // MAX PASSENGERS
      // --------------------------------------------------------
      maxPassengers: json['maxPassengers'] is int
          ? json['maxPassengers'] as int
          : int.tryParse(json['maxPassengers']?.toString() ?? '') ?? 0,

      // --------------------------------------------------------
      // SCHEDULED SUPPORTED
      // --------------------------------------------------------
      scheduledSupported: json['scheduledSupported'] as bool? ?? false,

      // --------------------------------------------------------
      // CASH SUPPORTED
      // --------------------------------------------------------
      cashSupported: json['cashSupported'] as bool? ?? false,

      // --------------------------------------------------------
      // ONLINE PAYMENT SUPPORTED
      // --------------------------------------------------------
      onlinePaymentSupported: json['onlinePaymentSupported'] as bool? ?? false,
    );
  }

  // ============================================================
  // TO JSON
  // ============================================================

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'code': code,
      'name': name,
      'description': description,
      'rideType': rideType.name,
      'active': active,
      'sortOrder': sortOrder,
      'maxPassengers': maxPassengers,
      'scheduledSupported': scheduledSupported,
      'cashSupported': cashSupported,
      'onlinePaymentSupported': onlinePaymentSupported,
    };
  }
}
