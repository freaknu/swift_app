import 'package:dio/dio.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

class RiderRoadRoute {
  final List<LatLng> points;

  const RiderRoadRoute(this.points);
}

class RiderRouteDatasource {
  final Dio dio;

  const RiderRouteDatasource(this.dio);

  Future<RiderRoadRoute?> fetch({
    required int rideId,
    required int driverId,
    required LatLng origin,
    required LatLng destination,
    String? rideTypeCode,
  }) async {
    final request = <String, dynamic>{
      'rideId': '$rideId',
      'driverId': '$driverId',
      'driverLatitude': origin.latitude,
      'driverLongitude': origin.longitude,
      'destinationLatitude': destination.latitude,
      'destinationLongitude': destination.longitude,
    };
    if (rideTypeCode != null) request['rideType'] = rideTypeCode;
    final response = await dio.post<Map<String, dynamic>>(
      'api/v1/location/eta',
      data: request,
    );
    if (response.statusCode != 200) {
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        type: DioExceptionType.badResponse,
      );
    }
    final payload = response.data?['data'];
    if (payload is! Map) return null;
    final polyline = payload['polyline']?.toString() ?? '';
    if (polyline.isEmpty) return null;
    final points = _decodePolyline(polyline);
    return points.length < 2 ? null : RiderRoadRoute(points);
  }

  List<LatLng> _decodePolyline(String encoded) {
    final points = <LatLng>[];
    var index = 0;
    var latitude = 0;
    var longitude = 0;
    try {
      while (index < encoded.length) {
        int nextValue() {
          var shift = 0;
          var result = 0;
          int byte;
          do {
            byte = encoded.codeUnitAt(index++) - 63;
            result |= (byte & 0x1f) << shift;
            shift += 5;
          } while (byte >= 0x20);
          return (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
        }

        latitude += nextValue();
        longitude += nextValue();
        points.add(LatLng(latitude / 1e5, longitude / 1e5));
      }
    } on RangeError {
      return const [];
    }
    return points;
  }
}
