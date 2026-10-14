import 'package:swift_app/features/common/fare/domain/repository/fare_repository.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_event.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_state.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

class FareBloc extends Bloc<FareEvent, FareState> {
  final FareRepository repository;
  int _requestGeneration = 0;

  FareBloc({required this.repository}) : super(const FareState()) {
    on<EstimateFare>(_onEstimateFare);
    on<ClearFare>(_onClearFare);
  }

  Future<void> _onEstimateFare(
    EstimateFare event,
    Emitter<FareState> emit,
  ) async {
    final generation = ++_requestGeneration;
    emit(
      state.copyWith(
        status: FareStatus.loading,
        clearFare: true,
        clearMessage: true,
        lastRequest: event.request,
      ),
    );

    try {
      final response = await repository.estimateFare(event.request);
      if (emit.isDone || generation != _requestGeneration) return;
      emit(
        state.copyWith(
          status: response.success && response.data != null
              ? FareStatus.success
              : FareStatus.failure,
          fareEstimate: response.data,
          message: response.message,
        ),
      );
    } catch (_) {
      if (emit.isDone || generation != _requestGeneration) return;
      emit(
        state.copyWith(
          status: FareStatus.failure,
          message: 'Unable to calculate the fare. Please try again.',
        ),
      );
    }
  }

  void _onClearFare(ClearFare event, Emitter<FareState> emit) {
    ++_requestGeneration;
    emit(
      state.copyWith(
        status: FareStatus.initial,
        clearFare: true,
        clearMessage: true,
        clearRequest: true,
      ),
    );
  }
}
