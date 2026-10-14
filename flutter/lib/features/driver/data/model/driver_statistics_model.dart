class DriverStatisticsModel {
  final int completedTrips;
  final int cancelledTrips;
  final double lifetimeDistanceKm;
  final double lifetimeEarning;
  final double averageRating;

  const DriverStatisticsModel({
    required this.completedTrips,
    required this.cancelledTrips,
    required this.lifetimeDistanceKm,
    required this.lifetimeEarning,
    required this.averageRating,
  });

  factory DriverStatisticsModel.fromJson(Map<String, dynamic> json) {
    return DriverStatisticsModel(
      completedTrips: _int(json['completedTrips']) ?? 0,
      cancelledTrips: _int(json['cancelledTrips']) ?? 0,
      lifetimeDistanceKm: _double(json['lifetimeDistance']) ?? 0,
      lifetimeEarning: _double(json['lifetimeEarning']) ?? 0,
      averageRating: _double(json['averageRating']) ?? 0,
    );
  }

  static int? _int(Object? value) =>
      value is num ? value.toInt() : int.tryParse(value?.toString() ?? '');

  static double? _double(Object? value) => value is num
      ? value.toDouble()
      : double.tryParse(value?.toString() ?? '');
}
