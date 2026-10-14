import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/rider/home/data/datasource/ride_type_datasource.dart';
import 'package:swift_app/features/rider/home/domain/entity/ride_type.dart';
import 'package:swift_app/features/rider/home/domain/repository/ride_type_repository.dart';

class RideTypeRepositoryImpl extends RideTypeRepository {
  final RideTypeDatasource rideTypeDatasource;

  RideTypeRepositoryImpl({required this.rideTypeDatasource});

  @override
  Future<ApiResponseFormat<List<RideType>>> fetchRideTypes() {
    return rideTypeDatasource.fetchRideTypes();
  }
}
