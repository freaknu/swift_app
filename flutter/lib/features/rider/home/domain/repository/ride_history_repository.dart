import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/rider/home/domain/entity/ride_history_address.dart';

abstract class RideHistoryRepository {
  Future<ApiResponseFormat<List<RideHistoryAddress>>> fetchRideHistory();
}
