import 'package:swift_app/features/rider/home/domain/usecase/ride_type_usecase.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'ride_type_event.dart';
import 'ride_type_state.dart';

class RideTypeBloc extends Bloc<RideTypeEvent, RideTypeState> {
  final RideTypeUsecase rideTypeUsecase;

  RideTypeBloc({required this.rideTypeUsecase}) : super(const RideTypeState()) {
    on<FetchRideTypes>(_onFetchRideTypes);

    on<SelectRideType>(_onSelectRideType);
  }

  // ============================================================
  // FETCH
  // ============================================================

  Future<void> _onFetchRideTypes(
    FetchRideTypes event,
    Emitter<RideTypeState> emit,
  ) async {
    emit(state.copyWith(status: RideTypeStatus.loading, clearMessage: true));

    try {
      final response = await rideTypeUsecase(NoParam());

      if (response.data != null) {
        emit(
          state.copyWith(
            status: RideTypeStatus.success,
            rideTypes: response.data!,
            message: response.message,
            statusCode: response.statusCode,
          ),
        );

        return;
      }

      emit(
        state.copyWith(
          status: RideTypeStatus.failure,
          message: response.message,
          statusCode: response.statusCode,
        ),
      );
    } catch (e) {
      emit(
        state.copyWith(status: RideTypeStatus.failure, message: e.toString()),
      );
    }
  }

  // ============================================================
  // SELECT
  // ============================================================

  void _onSelectRideType(SelectRideType event, Emitter<RideTypeState> emit) {
    emit(state.copyWith(selectedRideTypeId: event.rideTypeId));
  }
}
