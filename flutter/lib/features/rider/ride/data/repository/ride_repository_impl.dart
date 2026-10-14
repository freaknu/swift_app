import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/rider/ride/data/datasource/ride_datasource.dart';
import 'package:swift_app/features/rider/ride/data/model/ride_request_model.dart';

import 'package:swift_app/features/rider/ride/domain/entity/ride_request.dart';
import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';
import 'package:swift_app/features/rider/ride/domain/repository/ride_repository.dart';

class RideRepositoryImpl implements RideRepository {
  final RideDatasource rideDatasource;

  RideRepositoryImpl({required this.rideDatasource});

  @override
  Future<RideResponse> bookRide(RideRequest data) async {
    final RideRequestModel request = RideRequestModel.fromEntity(data);

    final response = await rideDatasource.bookRide(request);

    if (!response.success || response.data == null) {
      throw RideRepositoryException(
        message: response.message,
        statusCode: response.statusCode,
        errors: response.errors,
      );
    }

    return response.data!;
  }

  @override
  Future<RideResponse> getRide(int rideId) async {
    final response = await rideDatasource.getRide(rideId);
    return _requireData(response);
  }

  @override
  Future<RideResponse> getActiveRide(int riderId) async {
    final response = await rideDatasource.getActiveRide(riderId);
    return _requireData(response);
  }

  @override
  Future<RideResponse> cancelRide({
    required int rideId,
    required int riderId,
    required String reason,
  }) async {
    final response = await rideDatasource.cancelRide(
      rideId: rideId,
      riderId: riderId,
      reason: reason,
    );
    return _requireData(response);
  }

  RideResponse _requireData(ApiResponseFormat<RideResponse> response) {
    if (!response.success || response.data == null) {
      throw RideRepositoryException(
        message: response.message,
        statusCode: response.statusCode,
        errors: response.errors,
      );
    }
    return response.data!;
  }
}

class RideRepositoryException implements Exception {
  final String message;
  final int statusCode;
  final Object? errors;

  const RideRepositoryException({
    required this.message,
    required this.statusCode,
    this.errors,
  });

  @override
  String toString() => message;
}
