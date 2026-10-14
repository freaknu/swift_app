import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/constants/usecase/usecase.dart';
import 'package:swift_app/features/rider/home/domain/entity/address_create.dart';
import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';
import 'package:swift_app/features/rider/home/domain/repository/user_repository.dart';

class AddressCreateUsecase
    extends UseCase<AddressCreate, ApiResponseFormat<UserAddress>> {
  final UserRepository userRepository;

  AddressCreateUsecase({required this.userRepository});
  @override
  Future<ApiResponseFormat<UserAddress>> call(AddressCreate request) {
    return userRepository.saveNewAddress(request);
  }
}
