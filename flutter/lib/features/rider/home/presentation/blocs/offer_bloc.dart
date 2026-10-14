import 'package:swift_app/features/rider/home/domain/usecase/offer_usecase.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'offer_event.dart';
import 'offer_state.dart';

class OfferBloc extends Bloc<OfferEvent, OfferState> {
  final OfferUsecase offerUsecase;
  int _requestGeneration = 0;

  OfferBloc({required this.offerUsecase}) : super(const OfferState()) {
    on<FetchOffers>(_onFetchOffers);
  }

  // ============================================================
  // FETCH OFFERS
  // ============================================================

  Future<void> _onFetchOffers(
    FetchOffers event,
    Emitter<OfferState> emit,
  ) async {
    final generation = ++_requestGeneration;
    // ----------------------------------------------------------
    // Keep existing offers visible during refresh; only start from an
    // empty list when there is nothing to show yet.
    // ----------------------------------------------------------

    emit(
      state.copyWith(
        status: OfferStatus.loading,

        clearOffers: state.offers.isEmpty,

        clearMessage: true,

        lastRequest: event.request,
      ),
    );

    try {
      final response = await offerUsecase(event.request);
      if (emit.isDone || generation != _requestGeneration) return;

      // ========================================================
      // SUCCESS
      // ========================================================

      if (response.success && response.data != null) {
        emit(
          state.copyWith(
            status: OfferStatus.success,

            offers: response.data!,

            message: response.message,

            statusCode: response.statusCode,

            lastRequest: event.request,
          ),
        );

        return;
      }

      // ========================================================
      // FAILURE
      // ========================================================

      emit(
        state.copyWith(
          status: OfferStatus.failure,

          message: response.message,

          statusCode: response.statusCode,

          lastRequest: event.request,
        ),
      );
    } catch (e) {
      if (emit.isDone || generation != _requestGeneration) return;
      emit(
        state.copyWith(
          status: OfferStatus.failure,

          message: e.toString(),

          lastRequest: event.request,
        ),
      );
    }
  }
}
