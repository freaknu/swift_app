import 'package:swift_app/features/common/fare/domain/repository/fare_repository.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_event.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_state.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

class FareBloc extends Bloc<FareEvent, FareState> {
  final FareRepository repository;

  FareBloc({required this.repository}) : super(const FareState()) {
    on<EstimateFare>(_onEstimateFare);
    on<ClearFare>(_onClearFare);
  }

  Future<void> _onEstimateFare(
    EstimateFare event,
    Emitter<FareState> emit,
  ) async {
    emit(state.copyWith(status: FareStatus.loading, message: null));

    final response = await repository.estimateFare(event.request);

    if (response.success) {
      emit(
        state.copyWith(
          status: FareStatus.success,
          fareEstimate: response.data,
          message: response.message,
        ),
      );
    } else {
      emit(
        state.copyWith(status: FareStatus.failure, message: response.message),
      );
    }
  }

  void _onClearFare(ClearFare event, Emitter<FareState> emit) {
    emit(
      state.copyWith(
        status: FareStatus.initial,
        clearFare: true,
        message: null,
      ),
    );
  }
}
