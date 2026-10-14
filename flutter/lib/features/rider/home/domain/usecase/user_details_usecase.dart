import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/constants/usecase/usecase.dart';
import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';
import 'package:swift_app/features/rider/home/domain/repository/user_repository.dart';

class UserDetailsUsecase
    extends UseCase<NoParam, ApiResponseFormat<UserReponse>> {
  final UserRepository userRepository;
  UserDetailsUsecase({required this.userRepository});
  @override
  Future<ApiResponseFormat<UserReponse>> call(NoParam request) {
    return userRepository.fetchUserDetail();
  }
}

class NoParam {}
