import 'package:swift_app/constants/api_response/api_response_format.dart';

import 'package:swift_app/features/rider/home/data/datasource/offer_datasource.dart';
import 'package:swift_app/features/rider/home/data/model/offer_request_model.dart';

import 'package:swift_app/features/rider/home/domain/entity/offer_request.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_response.dart';

import 'package:swift_app/features/rider/home/domain/repository/offer_repository.dart';

class OfferRepositoryImpl extends OfferRepository {
  final OfferDatasource offerDatasource;

  OfferRepositoryImpl({required this.offerDatasource});

  @override
  Future<ApiResponseFormat<List<OfferResponse>>> fetchOffers(
    OfferRequest request,
  ) {
    final OfferRequestModel requestModel = OfferRequestModel(
      rideTypeCode: request.rideTypeCode,
      startFrom: request.startFrom,
    );

    return offerDatasource.fetchOffers(requestModel);
  }
}
