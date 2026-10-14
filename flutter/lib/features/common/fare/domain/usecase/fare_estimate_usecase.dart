import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/fare/data/model/fare_estimate_request_model.dart';
import 'package:swift_app/features/common/fare/domain/entity/fare_estimate_response.dart';
import 'package:swift_app/features/common/fare/domain/repository/fare_repository.dart';

class FareEstimateUsecase {
  final FareRepository fareRepository;

  FareEstimateUsecase({required this.fareRepository});

  Future<ApiResponseFormat<FareEstimateResponse>> call(
    FareEstimateRequestModel request,
  ) {
    return fareRepository.estimateFare(request);
  }
}
