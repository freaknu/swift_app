import 'package:swift_app/features/rider/home/domain/usecase/ride_history_usecase.dart';
import 'package:swift_app/features/rider/home/domain/usecase/user_details_usecase.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'ride_history_event.dart';
import 'ride_history_state.dart';

class RideHistoryBloc extends Bloc<RideHistoryEvent, RideHistoryState> {
  final RideHistoryUsecase rideHistoryUsecase;

  RideHistoryBloc({required this.rideHistoryUsecase})
    : super(const RideHistoryState()) {
    on<FetchRideHistory>(_onFetchRideHistory);
  }

  Future<void> _onFetchRideHistory(
    FetchRideHistory event,
    Emitter<RideHistoryState> emit,
  ) async {
    emit(state.copyWith(status: RideHistoryStatus.loading, clearMessage: true));

    try {
      final response = await rideHistoryUsecase(NoParam());

      if (response.data != null) {
        emit(
          state.copyWith(
            status: RideHistoryStatus.success,
            rides: response.data!,
            message: response.message,
            statusCode: response.statusCode,
          ),
        );

        return;
      }

      emit(
        state.copyWith(
          status: RideHistoryStatus.failure,
          message: response.message,
          statusCode: response.statusCode,
        ),
      );
    } catch (e) {
      emit(
        state.copyWith(
          status: RideHistoryStatus.failure,
          message: e.toString(),
        ),
      );
    }
  }
}
