import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/fare/data/datasource/fare_remote_datasource.dart';
import 'package:swift_app/features/common/fare/data/model/fare_estimate_request_model.dart';
import 'package:swift_app/features/common/fare/domain/entity/fare_estimate_response.dart';
import 'package:swift_app/features/common/fare/domain/repository/fare_repository.dart';

class FareRepositoryImpl implements FareRepository {
  final FareRemoteDatasource datasource;

  FareRepositoryImpl({required this.datasource});

  @override
  Future<ApiResponseFormat<FareEstimateResponse>> estimateFare(
    FareEstimateRequestModel request,
  ) async {
    return datasource.estimateFare(request);
  }
}
