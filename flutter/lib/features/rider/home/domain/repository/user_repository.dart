import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/rider/home/domain/entity/address_create.dart';
import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';

abstract class UserRepository {
  Future<ApiResponseFormat<UserAddress>> saveNewAddress(AddressCreate data);
  Future<ApiResponseFormat<UserReponse>> fetchUserDetail();
}
