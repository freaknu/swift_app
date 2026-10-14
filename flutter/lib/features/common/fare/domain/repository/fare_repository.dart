import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/fare/data/model/fare_estimate_request_model.dart';
import 'package:swift_app/features/common/fare/domain/entity/fare_estimate_response.dart';

abstract class FareRepository {
  Future<ApiResponseFormat<FareEstimateResponse>> estimateFare(
    FareEstimateRequestModel request,
  );
}
