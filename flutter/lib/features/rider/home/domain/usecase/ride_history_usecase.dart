import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/constants/usecase/usecase.dart';
import 'package:swift_app/features/rider/home/domain/entity/ride_history_address.dart';
import 'package:swift_app/features/rider/home/domain/repository/ride_history_repository.dart';
import 'package:swift_app/features/rider/home/domain/usecase/user_details_usecase.dart';

class RideHistoryUsecase
    extends UseCase<NoParam, ApiResponseFormat<List<RideHistoryAddress>>> {
  final RideHistoryRepository rideHistoryRepository;

  RideHistoryUsecase({required this.rideHistoryRepository});

  @override
  Future<ApiResponseFormat<List<RideHistoryAddress>>> call(NoParam request) {
    return rideHistoryRepository.fetchRideHistory();
  }
}
