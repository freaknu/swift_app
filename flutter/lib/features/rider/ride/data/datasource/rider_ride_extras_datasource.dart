import 'package:dio/dio.dart';

class AssignedDriverSummary {
  final int driverId;
  final String? driverName;
  final String? profileImage;
  final String? vehicleType;
  final String? brand;
  final String? model;
  final String? color;
  final String? registrationNumber;
  final int? seatCapacity;

  const AssignedDriverSummary({
    required this.driverId,
    this.driverName,
    this.profileImage,
    this.vehicleType,
    this.brand,
    this.model,
    this.color,
    this.registrationNumber,
    this.seatCapacity,
  });

  factory AssignedDriverSummary.fromJson(Map<dynamic, dynamic> json) {
    final driverId = int.tryParse('${json['driverId'] ?? ''}');
    if (driverId == null) throw const FormatException('Invalid driver summary');
    String? value(String key) {
      final text = json[key]?.toString().trim();
      return text == null || text.isEmpty ? null : text;
    }

    return AssignedDriverSummary(
      driverId: driverId,
      driverName: value('driverName'),
      profileImage: value('profileImage'),
      vehicleType: value('vehicleType'),
      brand: value('brand'),
      model: value('model'),
      color: value('color'),
      registrationNumber: value('registrationNumber'),
      seatCapacity: (json['seatCapacity'] as num?)?.toInt(),
    );
  }
}

class MyRideRating {
  final int? id;
  final int rate;

  const MyRideRating({required this.id, required this.rate});

  factory MyRideRating.fromJson(Map<dynamic, dynamic> json) {
    final id = int.tryParse('${json['id'] ?? ''}');
    final rate = (json['rate'] as num?)?.toInt();
    if (id == null || rate == null || rate < 1 || rate > 5) {
      throw const FormatException('Invalid ride rating');
    }
    return MyRideRating(id: id, rate: rate);
  }
}

class RiderRideExtrasDatasource {
  final Dio dio;

  const RiderRideExtrasDatasource(this.dio);

  Future<AssignedDriverSummary?> driverSummary(int rideId) async {
    final payload = await _getData('api/v1/ride/rides/$rideId/driver-summary');
    if (payload == null) return null;
    if (payload is! Map) throw const FormatException('Invalid driver summary');
    return AssignedDriverSummary.fromJson(payload);
  }

  Future<MyRideRating?> myRating(int rideId) async {
    final payload = await _getData('api/v1/rating/rides/$rideId/mine');
    if (payload == null) return null;
    if (payload is! Map) throw const FormatException('Invalid ride rating');
    return MyRideRating.fromJson(payload);
  }

  Future<MyRideRating?> rateDriver({
    required int rideId,
    required int driverId,
    required int rate,
  }) async {
    if (rate < 1 || rate > 5) throw ArgumentError.value(rate, 'rate');
    final response = await dio.post<Map<String, dynamic>>(
      'api/v1/rating/createRating',
      data: {
        'rideId': rideId,
        'driverId': driverId,
        'userType': 'DRIVER',
        'rate': rate,
      },
    );
    _requireSuccess(response);
    // POST returns the driver's aggregate, with the new ride entry when
    // available. A successful POST already confirms persistence; avoid
    // reporting a failed save if a separate lookup goes offline afterward.
    final aggregate = response.data?['data'];
    if (aggregate is Map && aggregate['allRatings'] is List) {
      for (final item in aggregate['allRatings'] as List) {
        if (item is Map && int.tryParse('${item['rideId'] ?? ''}') == rideId) {
          final rating = MyRideRating.fromJson(item);
          return rating;
        }
      }
    }
    return MyRideRating(id: null, rate: rate);
  }

  Future<dynamic> _getData(String path) async {
    final response = await dio.get<Map<String, dynamic>>(path);
    _requireSuccess(response);
    return response.data?['data'];
  }

  void _requireSuccess(Response<Map<String, dynamic>> response) {
    if (response.statusCode != 200 && response.statusCode != 201) {
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        type: DioExceptionType.badResponse,
      );
    }
  }
}
