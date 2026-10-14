import 'package:swift_app/features/rider/home/domain/entity/offer_request.dart';

abstract class OfferEvent {
  const OfferEvent();
}

// ============================================================
// FETCH OFFERS
// ============================================================

class FetchOffers extends OfferEvent {
  final OfferRequest request;

  const FetchOffers({required this.request});
}
