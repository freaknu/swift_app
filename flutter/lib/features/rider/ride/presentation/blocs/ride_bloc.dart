import 'package:swift_app/features/rider/ride/domain/usecases/ridebook_usecase.dart';
import 'package:swift_app/features/rider/ride/presentation/blocs/ride_event.dart';
import 'package:swift_app/features/rider/ride/presentation/blocs/ride_state.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'package:swift_app/features/rider/ride/data/repository/ride_repository_impl.dart';
import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';

class RideBloc extends Bloc<RideEvent, RideState> {
  final RidebookUsecase ridebookUsecase;

  RideBloc({required this.ridebookUsecase}) : super(const RideState()) {
    on<BookRide>(_onBookRide);
    on<ResetRide>(_onResetRide);
  }

  Future<void> _onBookRide(BookRide event, Emitter<RideState> emit) async {
    if (state.status == RideStatus.loading) {
      return;
    }

    emit(state.copyWith(status: RideStatus.loading, clearError: true));

    try {
      final RideResponse response = await ridebookUsecase(event.request);

      emit(
        state.copyWith(
          status: RideStatus.success,
          ride: response,
          clearError: true,
        ),
      );
    } on RideRepositoryException catch (e) {
      emit(
        state.copyWith(
          status: RideStatus.failure,
          errorMessage: e.message,
          statusCode: e.statusCode,
        ),
      );
    } catch (e) {
      emit(
        state.copyWith(status: RideStatus.failure, errorMessage: e.toString()),
      );
    }
  }

  void _onResetRide(ResetRide event, Emitter<RideState> emit) {
    emit(const RideState());
  }
}
