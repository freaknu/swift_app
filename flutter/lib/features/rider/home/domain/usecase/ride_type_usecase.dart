import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/constants/usecase/usecase.dart';
import 'package:swift_app/features/rider/home/domain/entity/ride_type.dart';
import 'package:swift_app/features/rider/home/domain/repository/ride_type_repository.dart';

class RideTypeUsecase
    extends UseCase<NoParam, ApiResponseFormat<List<RideType>>> {
  final RideTypeRepository rideTypeRepository;

  RideTypeUsecase({required this.rideTypeRepository});

  @override
  Future<ApiResponseFormat<List<RideType>>> call(NoParam request) {
    return rideTypeRepository.fetchRideTypes();
  }
}

class NoParam {}
