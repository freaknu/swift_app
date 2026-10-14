import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/rider/home/data/datasource/ride_history_datasource.dart';
import 'package:swift_app/features/rider/home/domain/entity/ride_history_address.dart';
import 'package:swift_app/features/rider/home/domain/repository/ride_history_repository.dart';

class RideHistoryRepositoryImpl extends RideHistoryRepository {
  final RideHistoryDatasource rideHistoryDatasource;

  RideHistoryRepositoryImpl({required this.rideHistoryDatasource});

  @override
  Future<ApiResponseFormat<List<RideHistoryAddress>>> fetchRideHistory() {
    return rideHistoryDatasource.fetchRideHistory();
  }
}
