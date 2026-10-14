import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/rider/home/domain/entity/ride_type.dart';

abstract class RideTypeRepository {
  Future<ApiResponseFormat<List<RideType>>> fetchRideTypes();
}
