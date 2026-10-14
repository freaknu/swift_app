import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/rider/home/data/datasource/user_remote_datasource.dart';
import 'package:swift_app/features/rider/home/data/model/address_create_model.dart';
import 'package:swift_app/features/rider/home/domain/entity/address_create.dart';
import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';
import 'package:swift_app/features/rider/home/domain/repository/user_repository.dart';

class UserRepositoryImpl extends UserRepository {
  final UserRemoteDatasource userRemoteDatasource;

  UserRepositoryImpl({required this.userRemoteDatasource});
  @override
  Future<ApiResponseFormat<UserReponse>> fetchUserDetail() {
    return userRemoteDatasource.fetchUserDetail();
  }

  @override
  Future<ApiResponseFormat<UserAddress>> saveNewAddress(AddressCreate data) {
    AddressCreateModel req = AddressCreateModel(
      addressType: data.addressType,
      label: data.label,
      fullAddress: data.fullAddress,
      landmark: data.landmark,
      city: data.city,
      state: data.state,
      country: data.country,
      pinCode: data.pinCode,
      latitude: data.latitude,
      longitude: data.longitude,
      defaultAddress: data.defaultAddress,
    );

    return userRemoteDatasource.addAddress(req);
  }
}
