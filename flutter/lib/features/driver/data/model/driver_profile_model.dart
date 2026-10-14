class DriverProfileModel {
  final int? driverId;
  final String driverName;
  final String email;
  final String phoneNumber;
  final String? gender;
  final String? dateOfBirth;
  final String? profileImage;
  final String? driverStatus;
  final bool acceptingRides;
  final String? driverAvailability;
  final List<DriverVehicleModel> vehicles;

  const DriverProfileModel({
    this.driverId,
    this.driverName = '',
    this.email = '',
    this.phoneNumber = '',
    this.gender,
    this.dateOfBirth,
    this.profileImage,
    this.driverStatus,
    this.acceptingRides = false,
    this.driverAvailability,
    this.vehicles = const [],
  });

  factory DriverProfileModel.fromJson(Map<String, dynamic> json) {
    final rawVehicles = json['allVehicles'];

    return DriverProfileModel(
      driverId: _asInt(json['driverId']),
      driverName: json['driverName']?.toString() ?? '',
      email: json['email']?.toString() ?? '',
      phoneNumber: json['phoneNumber']?.toString() ?? '',
      gender: json['gender']?.toString(),
      dateOfBirth: json['dateOfBirth']?.toString(),
      profileImage: json['profileImage']?.toString(),
      driverStatus: json['driverStatus']?.toString(),
      acceptingRides: _asBool(
        json['acceptingRides'] ?? json['isAcceptingRides'],
      ),
      driverAvailability: json['driverAvailability']?.toString(),
      vehicles: rawVehicles is List
          ? rawVehicles
                .whereType<Map<String, dynamic>>()
                .map(DriverVehicleModel.fromJson)
                .toList()
          : const [],
    );
  }

  static int? _asInt(dynamic value) {
    if (value is int) return value;
    return int.tryParse(value?.toString() ?? '');
  }

  static bool _asBool(dynamic value) {
    if (value is bool) return value;
    return value?.toString().toLowerCase() == 'true';
  }
}

class DriverVehicleModel {
  final int? id;
  final String vehicleType;
  final String brand;
  final String model;
  final String registrationNumber;
  final int? year;
  final String color;
  final bool active;
  final bool? verified;

  const DriverVehicleModel({
    this.id,
    this.vehicleType = '',
    this.brand = '',
    this.model = '',
    this.registrationNumber = '',
    this.year,
    this.color = '',
    this.active = false,
    this.verified,
  });

  String get displayName {
    final name = [
      brand,
      model,
    ].where((part) => part.trim().isNotEmpty).join(' ');
    if (name.isNotEmpty) return name;
    if (vehicleType.isNotEmpty) return vehicleType;
    return 'Vehicle';
  }

  factory DriverVehicleModel.fromJson(Map<String, dynamic> json) {
    return DriverVehicleModel(
      id: DriverProfileModel._asInt(json['id']),
      vehicleType: json['vehicleType']?.toString() ?? '',
      brand: json['brand']?.toString() ?? '',
      model: json['model']?.toString() ?? '',
      registrationNumber: json['registrationNumber']?.toString() ?? '',
      year: DriverProfileModel._asInt(json['year']),
      color: json['color']?.toString() ?? '',
      active: DriverProfileModel._asBool(json['active'] ?? json['isActive']),
      verified: json['isVerified'] == null && json['verified'] == null
          ? null
          : DriverProfileModel._asBool(json['isVerified'] ?? json['verified']),
    );
  }
}
