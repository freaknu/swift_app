import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_request.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_response.dart';

abstract class OfferRepository {
  Future<ApiResponseFormat<List<OfferResponse>>> fetchOffers(
    OfferRequest request,
  );
}
