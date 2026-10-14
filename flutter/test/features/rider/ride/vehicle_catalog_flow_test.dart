import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/common/fare/data/model/fare_estimate_request_model.dart';
import 'package:swift_app/features/rider/home/data/datasource/ride_type_datasource.dart';
import 'package:swift_app/features/rider/home/data/model/ride_type_model.dart';
import 'package:swift_app/features/rider/home/presentation/pages/trip_route_map_page.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/ride_type_card_widget.dart';
import 'package:swift_app/features/rider/ride/data/model/ride_request_model.dart';
import 'package:swift_app/features/rider/ride/domain/entity/ride_request.dart';
import 'package:swift_app/features/rider/ride/domain/entity/route.dart'
    as ride_models;

void main() {
  test('parsed vehicle catalog selects preferred type with a model list', () {
    final parsed = [
      RideTypeModel.fromVehicleTypeJson({
        'code': 'HATCHBACK',
        'name': 'Hatchback',
      }, 0),
      RideTypeModel.fromVehicleTypeJson({
        'code': 'AUTO_RICKSHAW',
        'name': 'Auto Rickshaw',
      }, 1),
    ];

    expect(
      selectPreferredRideType(parsed, 'AUTO_RICKSHAW').code,
      'AUTO_RICKSHAW',
    );
    expect(selectPreferredRideType(parsed, 'UNAVAILABLE').code, 'HATCHBACK');
    expect(selectPreferredRideType(parsed, null).code, 'HATCHBACK');
  });

  test('rider catalog reads canonical backend vehicle types', () async {
    SharedPreferences.setMockInitialValues({'auth.userId': 7});
    final prefs = await SharedPreferences.getInstance();
    final client = DioClient(
      accessTokenReader: () async => 'fixture-token',
      authUserIdReader: () async => 7,
    );
    RequestOptions? request;
    client.dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          request = options;
          handler.resolve(
            Response(
              requestOptions: options,
              statusCode: 200,
              data: {
                'success': true,
                'statusCode': 200,
                'data': [
                  {'code': 'HATCHBACK', 'name': 'Hatchback'},
                  {'code': 'AUTO_RICKSHAW', 'name': 'Auto Rickshaw'},
                  {'code': 'ELECTRIC_BIKE', 'name': 'Electric Bike'},
                ],
              },
            ),
          );
        },
      ),
    );
    final datasource = RideTypeDatasource(
      dioClient: client,
      riderApiEndpoints: RiderApiEndpoints(),
      authLocalDatasource: AuthLocalDatasource(preferences: prefs),
    );

    final response = await datasource.fetchRideTypes();

    expect(response.success, isTrue);
    expect(request!.path, 'api/v1/vehicle/types');
    expect(request!.method, 'GET');
    expect(request!.headers['x-user-id'].toString(), '7');
    expect(response.data!.map((type) => type.code), [
      'HATCHBACK',
      'AUTO_RICKSHAW',
      'ELECTRIC_BIKE',
    ]);
    expect(response.data!.every((type) => type.active), isTrue);
    expect(response.data![1].name, 'Auto Rickshaw');
    expect(response.data![0].maxPassengers, 0);
  });

  testWidgets(
    'canonical vehicle choice is selectable without invented capacity',
    (tester) async {
      final type = RideTypeModel.fromVehicleTypeJson({
        'code': 'PREMIUM_SUV',
        'name': 'Premium SUV',
      }, 0);
      var selected = false;
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SizedBox(
              width: 150,
              height: 160,
              child: RideTypeCard(
                rideType: type,
                selected: true,
                onTap: () => selected = true,
              ),
            ),
          ),
        ),
      );

      expect(find.text('Premium SUV'), findsOneWidget);
      expect(find.text('0'), findsNothing);
      expect(find.byType(Image), findsOneWidget);
      expect(
        find.byWidgetPredicate(
          (widget) =>
              widget is AppIcon && widget.icon == SwiftIcons.circleCheck,
        ),
        findsOneWidget,
      );
      await tester.tap(find.text('Premium SUV'));
      expect(selected, isTrue);
      expect(tester.takeException(), isNull);
    },
  );

  test('booking and fare quote carry the selected exact vehicle code', () {
    const code = 'AUTO_RICKSHAW';
    final booking = RideRequestModel.fromEntity(
      const RideRequest(
        authUserId: 7,
        riderId: 12,
        rideType: code,
        route: ride_models.Route(
          pickupLatitude: 12.9716,
          pickupLongitude: 77.5946,
          destinationLatitude: 12.9800,
          destinationLongitude: 77.6000,
        ),
      ),
    ).toJson();
    const estimate = FareEstimateRequestModel(
      offerId: null,
      pickupLatitude: 12.9716,
      pickupLongitude: 77.5946,
      destinationLatitude: 12.9800,
      destinationLongitude: 77.6000,
      rideType: code,
    );

    expect(booking['rideType'], code);
    expect((booking['route'] as Map<String, dynamic>)['rideType'], code);
    expect(estimate.toJson()['rideType'], code);
  });
}
