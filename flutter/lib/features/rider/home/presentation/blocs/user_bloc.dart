import 'package:swift_app/features/rider/home/domain/usecase/address_create_usecase.dart';
import 'package:swift_app/features/rider/home/domain/usecase/user_details_usecase.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'user_event.dart';
import 'user_state.dart';

class UserBloc extends Bloc<UserEvent, UserState> {
  final AddressCreateUsecase addressCreateUsecase;
  final UserDetailsUsecase userDetailsUsecase;
  final Future<void> Function(Duration) profileRetryDelay;

  UserBloc({
    required this.addressCreateUsecase,
    required this.userDetailsUsecase,
    this.profileRetryDelay = _waitForProfile,
  }) : super(const UserState()) {
    on<FetchUserDetails>(_onFetchUserDetails);
    on<CreateUserAddress>(_onCreateUserAddress);
  }

  Future<void> _onFetchUserDetails(
    FetchUserDetails event,
    Emitter<UserState> emit,
  ) async {
    emit(state.copyWith(status: UserStatus.loading, clearMessage: true));

    try {
      // Signup publishes profile creation asynchronously. A short-lived 404
      // immediately after login means the consumer has not created it yet.
      const delays = [
        Duration(milliseconds: 300),
        Duration(milliseconds: 700),
        Duration(seconds: 1),
        Duration(seconds: 2),
      ];
      var response = await userDetailsUsecase(NoParam());
      for (final delay in delays) {
        if (response.data != null || response.statusCode != 404) break;
        await profileRetryDelay(delay);
        if (emit.isDone) return;
        response = await userDetailsUsecase(NoParam());
      }

      if (response.data != null) {
        emit(
          state.copyWith(
            status: UserStatus.success,
            user: response.data,
            message: response.message,
            statusCode: response.statusCode,
          ),
        );

        return;
      }

      emit(
        state.copyWith(
          status: UserStatus.failure,
          message: response.statusCode == 404
              ? 'Your profile is still being prepared. Please retry.'
              : response.message.isEmpty
              ? 'User data is empty'
              : response.message,
          statusCode: response.statusCode,
        ),
      );
    } catch (e) {
      emit(state.copyWith(status: UserStatus.failure, message: e.toString()));
    }
  }

  static Future<void> _waitForProfile(Duration duration) =>
      Future<void>.delayed(duration);

  Future<void> _onCreateUserAddress(
    CreateUserAddress event,
    Emitter<UserState> emit,
  ) async {
    emit(state.copyWith(status: UserStatus.loading, clearMessage: true));

    try {
      final response = await addressCreateUsecase(event.address);

      if (response.data == null) {
        emit(
          state.copyWith(
            status: UserStatus.failure,
            message: response.message.isEmpty
                ? 'Unable to create address'
                : response.message,
            statusCode: response.statusCode,
          ),
        );

        return;
      }
      final userResponse = await userDetailsUsecase(NoParam());

      if (userResponse.data != null) {
        emit(
          state.copyWith(
            status: UserStatus.success,
            user: userResponse.data,
            createdAddress: response.data,
            message: 'Address added successfully',
            statusCode: response.statusCode,
          ),
        );

        return;
      }

      emit(
        state.copyWith(
          status: UserStatus.success,
          createdAddress: response.data,
          message: 'Address added successfully',
          statusCode: response.statusCode,
        ),
      );
    } catch (e) {
      emit(state.copyWith(status: UserStatus.failure, message: e.toString()));
    }
  }
}
