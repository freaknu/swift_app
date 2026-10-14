import 'package:swift_app/features/rider/home/domain/entity/offer_request.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_response.dart';

enum OfferStatus { initial, loading, success, failure }

class OfferState {
  final OfferStatus status;

  final List<OfferResponse> offers;

  final String? message;

  final int? statusCode;

  final OfferRequest? lastRequest;

  const OfferState({
    this.status = OfferStatus.initial,
    this.offers = const [],
    this.message,
    this.statusCode,
    this.lastRequest,
  });

  OfferState copyWith({
    OfferStatus? status,
    List<OfferResponse>? offers,
    String? message,
    int? statusCode,
    OfferRequest? lastRequest,
    bool clearMessage = false,
    bool clearOffers = false,
  }) {
    return OfferState(
      status: status ?? this.status,

      offers: clearOffers ? const [] : offers ?? this.offers,

      message: clearMessage ? null : message ?? this.message,

      statusCode: statusCode ?? this.statusCode,

      lastRequest: lastRequest ?? this.lastRequest,
    );
  }
}
