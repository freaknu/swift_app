import 'package:swift_app/constants/api_response/api_response_format.dart';

import 'package:swift_app/constants/usecase/usecase.dart';

import 'package:swift_app/features/rider/home/domain/entity/offer_request.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_response.dart';

import 'package:swift_app/features/rider/home/domain/repository/offer_repository.dart';

class OfferUsecase
    extends UseCase<OfferRequest, ApiResponseFormat<List<OfferResponse>>> {
  final OfferRepository offerRepository;

  OfferUsecase({required this.offerRepository});

  @override
  Future<ApiResponseFormat<List<OfferResponse>>> call(OfferRequest request) {
    return offerRepository.fetchOffers(request);
  }
}
