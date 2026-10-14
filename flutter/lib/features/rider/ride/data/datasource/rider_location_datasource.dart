import 'package:dio/dio.dart';
import 'package:swift_app/features/rider/ride/domain/entity/driver_position_fix.dart';

class RiderLocationDatasource {
  final Dio dio;

  const RiderLocationDatasource(this.dio);

  Future<DriverPositionFix?> latestDriverPosition(int rideId) async {
    final response = await dio.get<Map<String, dynamic>>(
      'api/v1/location/rides/$rideId/driver-position',
    );
    if (response.statusCode != 200) {
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        type: DioExceptionType.badResponse,
      );
    }
    final payload = response.data?['data'];
    if (payload == null) return null;
    if (payload is! Map) throw const FormatException('Invalid driver position');
    final fix = DriverPositionFix.tryParse(payload);
    if (fix == null || fix.rideId != rideId) {
      throw const FormatException('Invalid driver position');
    }
    return fix;
  }
}
