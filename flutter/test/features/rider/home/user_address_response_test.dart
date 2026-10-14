import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/constants/enums/address_type_enum.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/rider/home/data/datasource/user_remote_datasource.dart';
import 'package:swift_app/features/rider/home/data/model/address_create_model.dart';

Map<String, dynamic> _address(int id, {double latitude = 12.9716}) => {
  'id': id,
  'addressType': 'HOME',
  'label': 'Home',
  'fullAddress': 'MG Road, Bengaluru',
  'landmark': 'Metro',
  'city': 'Bengaluru',
  'state': 'Karnataka',
  'country': 'India',
  'pinCode': '560001',
  'latitude': latitude,
  'longitude': 77.5946,
  'defaultAddress': false,
};

AddressCreateModel _request() => AddressCreateModel(
  addressType: AddressTypeEnum.HOME,
  label: 'Home',
  fullAddress: 'MG Road, Bengaluru',
  landmark: 'Metro',
  city: 'Bengaluru',
  state: 'Karnataka',
  country: 'India',
  pinCode: '560001',
  latitude: 12.9716,
  longitude: 77.5946,
  defaultAddress: false,
);

Future<UserRemoteDatasource> _datasource(
  dynamic responseData,
  List<RequestOptions> requests,
) async {
  SharedPreferences.setMockInitialValues({'auth.userId': 410});
  final prefs = await SharedPreferences.getInstance();
  final client = DioClient(
    accessTokenReader: () async => 'fixture-token',
    authUserIdReader: () async => 410,
  );
  client.dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (request, handler) {
        requests.add(request);
        handler.resolve(
          Response(
            requestOptions: request,
            statusCode: 200,
            data: {'success': true, 'statusCode': 200, 'data': responseData},
          ),
        );
      },
    ),
  );
  return UserRemoteDatasource(
    dioClient: client,
    riderApiEndpoints: RiderApiEndpoints(),
    authLocalDatasource: AuthLocalDatasource(preferences: prefs),
  );
}

void main() {
  test(
    'list response chooses newest exact submitted address, not first item',
    () async {
      final requests = <RequestOptions>[];
      final datasource = await _datasource([
        _address(89),
        _address(91, latitude: 12.98),
        _address(95),
        _address(7),
      ], requests);

      final response = await datasource.addAddress(_request());

      expect(response.success, isTrue);
      expect(response.data?.id, 95);
      expect(response.data?.latitude, 12.9716);
      expect(requests.single.method, 'POST');
      expect(requests.single.path, 'api/v1/user/addAddress');
      expect(requests.single.data['label'], 'Home');
      expect(requests.single.headers['Authorization'], 'Bearer fixture-token');
      expect(requests.single.headers['x-user-id'], '410');
    },
  );

  test('legacy single-address map remains compatible', () async {
    final datasource = await _datasource(_address(42), []);

    final response = await datasource.addAddress(_request());

    expect(response.success, isTrue);
    expect(response.data?.id, 42);
    expect(response.data?.label, 'Home');
  });

  test(
    'list missing the submitted place fails parsing instead of saving a wrong one',
    () async {
      final datasource = await _datasource([_address(9, latitude: 13.0)], []);

      final response = await datasource.addAddress(_request());

      expect(response.success, isFalse);
      expect(response.message, 'Failed to parse response');
      expect(response.data, isNull);
    },
  );
}
