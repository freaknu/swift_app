import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/rider/home/domain/entity/address_create.dart';
import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';
import 'package:swift_app/features/rider/home/domain/repository/user_repository.dart';
import 'package:swift_app/features/rider/home/domain/usecase/address_create_usecase.dart';
import 'package:swift_app/features/rider/home/domain/usecase/user_details_usecase.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_state.dart';

class _ProfileRepository implements UserRepository {
  _ProfileRepository(this.failuresBeforeReady);

  final int failuresBeforeReady;
  int requests = 0;

  @override
  Future<ApiResponseFormat<UserReponse>> fetchUserDetail() async {
    requests++;
    if (requests <= failuresBeforeReady) {
      return ApiResponseFormat.failure(
        statusCode: 404,
        message: 'Profile missing',
        endpoint: '/user',
      );
    }
    return ApiResponseFormat.success(
      data: UserReponse(
        id: 7,
        authUserId: 900,
        fullName: 'Rider',
        email: null,
        phoneNumber: '',
        profileImageUrl: null,
        active: true,
        deleted: false,
        allAddresses: const [],
      ),
      statusCode: 200,
      message: 'Ready',
      endpoint: '/user',
    );
  }

  @override
  Future<ApiResponseFormat<UserAddress>> saveNewAddress(AddressCreate data) =>
      throw UnimplementedError();
}

void main() {
  test('new rider profile retries only the transient 404', () async {
    final repository = _ProfileRepository(2);
    final delays = <Duration>[];
    final bloc = UserBloc(
      addressCreateUsecase: AddressCreateUsecase(userRepository: repository),
      userDetailsUsecase: UserDetailsUsecase(userRepository: repository),
      profileRetryDelay: (duration) async => delays.add(duration),
    );
    addTearDown(bloc.close);

    bloc.add(const FetchUserDetails());
    final state = await bloc.stream.firstWhere(
      (state) => state.status == UserStatus.success,
    );
    expect(state.user?.id, 7);
    expect(repository.requests, 3);
    expect(delays, hasLength(2));
  });

  test(
    'profile 404 retry is bounded and leaves a useful retry message',
    () async {
      final repository = _ProfileRepository(99);
      final bloc = UserBloc(
        addressCreateUsecase: AddressCreateUsecase(userRepository: repository),
        userDetailsUsecase: UserDetailsUsecase(userRepository: repository),
        profileRetryDelay: (_) async {},
      );
      addTearDown(bloc.close);
      // The fake is permanently 404: verify the retry is bounded.
      bloc.add(const FetchUserDetails());
      final state = await bloc.stream.firstWhere(
        (state) => state.status == UserStatus.failure,
      );
      expect(state.message, contains('still being prepared'));
      expect(repository.requests, 5);
    },
  );
}
