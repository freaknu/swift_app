import 'package:swift_app/constants/usecase/usecase.dart';
import 'package:swift_app/features/rider/ride/domain/entity/ride_request.dart';
import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';
import 'package:swift_app/features/rider/ride/domain/repository/ride_repository.dart';

class RidebookUsecase extends UseCase<RideRequest, RideResponse> {
  final RideRepository repository;

  RidebookUsecase(this.repository);

  @override
  Future<RideResponse> call(RideRequest request) {
    return repository.bookRide(request);
  }
}
