import 'package:dio/dio.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

class NearbyDriverPosition {
  final int driverId;
  final String? vehicleType;
  final DateTime updatedAt;
  final LatLng point;

  const NearbyDriverPosition({
    required this.driverId,
    required this.vehicleType,
    required this.updatedAt,
    required this.point,
  });

  static NearbyDriverPosition? tryParse(Map<dynamic, dynamic> json) {
    final id = int.tryParse('${json['driverId'] ?? ''}');
    final lat = (json['latitude'] as num?)?.toDouble();
    final lng = (json['longitude'] as num?)?.toDouble();
    final updated = DateTime.tryParse(json['updatedAt']?.toString() ?? '');
    if (id == null ||
        lat == null ||
        lng == null ||
        updated == null ||
        !lat.isFinite ||
        !lng.isFinite ||
        lat.abs() > 90 ||
        lng.abs() > 180) {
      return null;
    }
    return NearbyDriverPosition(
      driverId: id,
      vehicleType: json['vehicleType']?.toString(),
      updatedAt: updated.toUtc(),
      point: LatLng(lat, lng),
    );
  }

  bool isFreshAt(DateTime now) {
    final age = now.toUtc().difference(updatedAt);
    return age >= const Duration(seconds: -30) &&
        age <= const Duration(seconds: 60);
  }
}

class NearbyDriverDatasource {
  final Dio dio;

  const NearbyDriverDatasource(this.dio);

  Future<List<NearbyDriverPosition>> fetch({
    required double latitude,
    required double longitude,
    double distanceKm = 5,
  }) async {
    final response = await dio.get<Map<String, dynamic>>(
      'api/v1/location/nearbyDrivers',
      queryParameters: {
        'latitude': latitude,
        'longitude': longitude,
        'distance': distanceKm,
      },
    );
    if (response.statusCode != 200) {
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        type: DioExceptionType.badResponse,
      );
    }
    final items = response.data?['data'];
    if (items is! List) throw const FormatException('Invalid nearby drivers');
    final now = DateTime.now();
    return items
        .whereType<Map>()
        .map(NearbyDriverPosition.tryParse)
        .whereType<NearbyDriverPosition>()
        .where((driver) => driver.isFreshAt(now))
        .take(30)
        .toList();
  }
}
