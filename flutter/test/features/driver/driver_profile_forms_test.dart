import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/driver/data/model/driver_document_model.dart';
import 'package:swift_app/features/driver/data/model/driver_profile_model.dart';
import 'package:swift_app/features/driver/data/model/profile_completeness_model.dart';
import 'package:swift_app/features/driver/data/model/vehicle_type_option_model.dart';
import 'package:swift_app/features/driver/presentation/pages/complete_profile_page.dart';

ApiResponseFormat<T> ok<T>(T data) => ApiResponseFormat<T>.success(
  data: data,
  statusCode: 200,
  message: 'OK',
  endpoint: 'fixture',
);

const vehicle = DriverVehicleModel(
  id: 11,
  vehicleType: 'SEDAN',
  brand: 'Swift',
  model: 'One',
  registrationNumber: 'KA01AB1234',
  year: 2022,
  color: 'White',
  active: true,
);

class ProfileFormDatasource extends DriverRemoteDatasource {
  ProfileFormDatasource({
    required super.dioClient,
    required super.endpoints,
    required super.authLocalDatasource,
  });

  List<VehicleTypeOption> types = const [
    VehicleTypeOption(code: 'SEDAN', name: 'Sedan'),
    VehicleTypeOption(code: 'BIKE', name: 'Bike'),
  ];
  List<DriverVehicleModel> vehicles = [vehicle];
  List<DriverDocumentModel> documents = const [
    DriverDocumentModel(
      id: 21,
      documentType: 'DRIVING_LICENCE',
      documentUrl: 'https://example.test/old.pdf',
      verificationStatus: 'VERIFIED',
    ),
  ];
  int addCalls = 0;
  Map<String, dynamic>? addedBody;
  int updateCalls = 0;
  int? updatedId;
  Map<String, dynamic>? updatedBody;
  Completer<ApiResponseFormat<DriverVehicleModel>>? updateGate;
  String? uploadedType;
  String? uploadedUrl;

  @override
  Future<ApiResponseFormat<List<VehicleTypeOption>>> getVehicleTypes() async =>
      ok(types);

  @override
  Future<ApiResponseFormat<List<DriverVehicleModel>>> getVehicles() async =>
      ok(vehicles);

  @override
  Future<ApiResponseFormat<DriverVehicleModel>> addVehicle(
    Map<String, dynamic> data,
  ) async {
    addCalls++;
    addedBody = data;
    return ok(vehicle);
  }

  @override
  Future<ApiResponseFormat<DriverVehicleModel>> updateVehicle(
    int vehicleId,
    Map<String, dynamic> data,
  ) {
    updateCalls++;
    updatedId = vehicleId;
    updatedBody = data;
    return updateGate?.future ?? Future.value(ok(vehicle));
  }

  @override
  Future<ApiResponseFormat<List<DriverDocumentModel>>> getDocuments() async =>
      ok(documents);

  @override
  Future<ApiResponseFormat<DriverDocumentModel>> uploadDocument({
    required String documentType,
    required String documentUrl,
  }) async {
    uploadedType = documentType;
    uploadedUrl = documentUrl;
    final saved = DriverDocumentModel(
      id: 21,
      documentType: documentType,
      documentUrl: documentUrl,
      verificationStatus: 'PENDING',
    );
    documents = [saved];
    return ok(saved);
  }

  @override
  Future<ApiResponseFormat<ProfileCompletenessModel>>
  getProfileCompleteness() async => ok(
    const ProfileCompletenessModel(
      complete: false,
      personal: CompletenessSection(complete: true, missing: []),
      vehicle: CompletenessSection(complete: true, missing: []),
      documents: CompletenessSection(complete: false, missing: ['PAN_CARD']),
      missing: ['PAN_CARD'],
    ),
  );
}

Future<ProfileFormDatasource> fakeDatasource() async {
  SharedPreferences.setMockInitialValues({'auth.userId': 7});
  final prefs = await SharedPreferences.getInstance();
  return ProfileFormDatasource(
    dioClient: DioClient(),
    endpoints: RiderApiEndpoints(),
    authLocalDatasource: AuthLocalDatasource(preferences: prefs),
  );
}

Finder field(String label) => find.byWidgetPredicate(
  (widget) => widget is TextField && widget.decoration?.labelText == label,
);

void main() {
  testWidgets('profile tab taps and swipes release focus from retained forms', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(430, 844);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });
    final datasource = await fakeDatasource();
    await tester.pumpWidget(
      MaterialApp(home: CompleteProfilePage(datasource: datasource)),
    );
    await tester.pumpAndSettle();
    await tester.ensureVisible(field('Full name'));
    await tester.enterText(field('Full name'), 'Retained personal draft');
    final personal = tester.widget<EditableText>(
      find.descendant(
        of: field('Full name'),
        matching: find.byType(EditableText),
      ),
    );
    expect(personal.focusNode.hasFocus, isTrue);

    await tester.tap(find.text('Vehicle').first);
    await tester.pumpAndSettle();
    expect(personal.focusNode.hasFocus, isFalse);
    expect(personal.focusNode.canRequestFocus, isFalse);
    personal.focusNode.requestFocus();
    await tester.pump();
    expect(personal.focusNode.hasFocus, isFalse);

    await tester.ensureVisible(field('Brand *'));
    await tester.enterText(field('Brand *'), 'Retained vehicle draft');
    final vehicle = tester.widget<EditableText>(
      find.descendant(
        of: field('Brand *'),
        matching: find.byType(EditableText),
      ),
    );
    expect(vehicle.focusNode.hasFocus, isTrue);
    await tester.drag(find.byType(TabBarView), const Offset(-430, 0));
    await tester.pumpAndSettle();
    final tabs = DefaultTabController.of(
      tester.element(find.byType(TabBarView)),
    );
    expect(tabs.index, 2);
    expect(vehicle.focusNode.hasFocus, isFalse);
    expect(vehicle.focusNode.canRequestFocus, isFalse);

    await tester.ensureVisible(find.text('Personal').first);
    await tester.tap(find.text('Personal').first);
    await tester.pumpAndSettle();
    expect(
      tester.widget<TextField>(field('Full name')).controller!.text,
      'Retained personal draft',
    );
    await tester.ensureVisible(find.text('Vehicle').first);
    await tester.tap(find.text('Vehicle').first);
    await tester.pumpAndSettle();
    expect(
      tester.widget<TextField>(field('Brand *')).controller!.text,
      'Retained vehicle draft',
    );
    await tester.ensureVisible(field('Brand *'));
    await tester.tap(field('Brand *'));
    await tester.pump();
    expect(vehicle.focusNode.hasFocus, isTrue);
    tabs.animateTo(0);
    await tester.pumpAndSettle();
    expect(vehicle.focusNode.hasFocus, isFalse);
    expect(vehicle.focusNode.canRequestFocus, isFalse);
    expect(
      tester.widget<TextField>(field('Full name')).controller!.text,
      'Retained personal draft',
    );
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'profile header compacts for the keyboard and preserves entered details',
    (tester) async {
      tester.view.physicalSize = const Size(430, 844);
      tester.view.devicePixelRatio = 1;
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
        tester.view.resetViewInsets();
      });
      final datasource = await fakeDatasource();
      await tester.pumpWidget(
        MaterialApp(home: CompleteProfilePage(datasource: datasource)),
      );
      await tester.pumpAndSettle();
      expect(find.text('Profile progress'), findsOneWidget);
      await tester.ensureVisible(field('Full name'));
      await tester.enterText(field('Full name'), 'Updated Driver');
      tester.view.viewInsets = const FakeViewPadding(bottom: 320);
      await tester.pumpAndSettle();
      expect(find.text('Profile progress'), findsNothing);
      await tester.ensureVisible(field('Full name'));
      expect(
        tester.widget<TextField>(field('Full name')).controller!.text,
        'Updated Driver',
      );
      expect(tester.takeException(), isNull);
      tester.view.resetViewInsets();
      await tester.pumpAndSettle();
      expect(find.text('Profile progress'), findsOneWidget);
      expect(
        tester.widget<TextField>(field('Full name')).controller!.text,
        'Updated Driver',
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('existing vehicle is loaded and saved with PUT once', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(430, 1200);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });
    final datasource = await fakeDatasource();
    datasource.updateGate = Completer<ApiResponseFormat<DriverVehicleModel>>();
    await tester.pumpWidget(
      MaterialApp(home: CompleteProfilePage(datasource: datasource)),
    );
    await tester.ensureVisible(find.text('Vehicle').first);
    await tester.tap(find.text('Vehicle').first);
    await tester.pumpAndSettle();

    expect(find.text('Save vehicle'), findsOneWidget);
    expect(
      tester.widget<TextField>(field('Brand *')).controller!.text,
      'Swift',
    );
    expect(tester.widget<TextField>(field('Year *')).controller!.text, '2022');
    expect(find.text('Sedan'), findsWidgets);

    await tester.enterText(field('Model *'), 'Two');
    await tester.ensureVisible(find.text('Save vehicle'));
    await tester.tap(find.text('Save vehicle'));
    await tester.pump();
    expect(find.text('Saving vehicle'), findsOneWidget);
    expect(datasource.updateCalls, 1);
    expect(datasource.updatedId, 11);
    expect(datasource.updatedBody!['model'], 'Two');
    expect(datasource.addCalls, 0);

    datasource.updateGate!.complete(ok(vehicle));
    await tester.pumpAndSettle();
    expect(tester.widget<TextField>(field('Model *')).controller!.text, 'Two');
    expect(find.text('Save vehicle'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('new vehicle uses backend type and POST once', (tester) async {
    tester.view.physicalSize = const Size(430, 1200);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });
    final datasource = await fakeDatasource();
    datasource.vehicles = [];
    await tester.pumpWidget(
      MaterialApp(home: CompleteProfilePage(datasource: datasource)),
    );
    await tester.ensureVisible(find.text('Vehicle').first);
    await tester.tap(find.text('Vehicle').first);
    await tester.pumpAndSettle();
    await tester.tap(find.byType(DropdownButtonFormField<String>));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Sedan').last);
    await tester.pumpAndSettle();
    await tester.enterText(field('Brand *'), 'Swift');
    await tester.enterText(field('Model *'), 'One');
    await tester.enterText(field('Registration number *'), 'KA01AB1234');
    await tester.enterText(field('Year *'), '2022');
    await tester.ensureVisible(find.text('Add vehicle'));
    await tester.tap(find.text('Add vehicle'));
    await tester.pumpAndSettle();

    expect(datasource.addCalls, 1);
    expect(datasource.addedBody!['vehicleType'], 'SEDAN');
    expect(datasource.updateCalls, 0);
    expect(find.text('Save vehicle'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('documents show saved URL and replacement persists across tabs', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(430, 1200);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });
    final datasource = await fakeDatasource();
    await tester.pumpWidget(
      MaterialApp(home: CompleteProfilePage(datasource: datasource)),
    );
    await tester.ensureVisible(find.text('Documents').first);
    await tester.tap(find.text('Documents').first);
    await tester.pumpAndSettle();

    expect(find.text('https://example.test/old.pdf'), findsOneWidget);
    expect(find.text('Verified'), findsOneWidget);
    await tester.ensureVisible(find.text('Use URL').first);
    await tester.tap(find.text('Use URL').first);
    await tester.pumpAndSettle();
    expect(find.text('Replace Driving licence'), findsOneWidget);
    await tester.enterText(
      field('Document URL'),
      'https://example.test/new.pdf',
    );
    await tester.tap(find.text('Save URL'));
    await tester.pumpAndSettle();

    expect(datasource.uploadedType, 'DRIVING_LICENCE');
    expect(datasource.uploadedUrl, 'https://example.test/new.pdf');
    expect(find.text('https://example.test/new.pdf'), findsOneWidget);
    expect(find.text('Uploaded · Pending review'), findsOneWidget);
    await tester.ensureVisible(find.text('Vehicle').first);
    await tester.tap(find.text('Vehicle').first);
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('Documents').first);
    await tester.tap(find.text('Documents').first);
    await tester.pumpAndSettle();
    expect(find.text('https://example.test/new.pdf'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('notification deep link opens the matching profile section', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(430, 1200);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });
    final datasource = await fakeDatasource();
    await tester.pumpWidget(
      MaterialApp(
        home: CompleteProfilePage(datasource: datasource, initialTab: 2),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('https://example.test/old.pdf'), findsOneWidget);
    expect(find.text('Save vehicle'), findsNothing);
  });

  testWidgets('documents remain usable on a narrow large-text phone', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(320, 760);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });
    final datasource = await fakeDatasource();
    await tester.pumpWidget(
      MaterialApp(
        home: MediaQuery(
          data: const MediaQueryData(textScaler: TextScaler.linear(1.45)),
          child: CompleteProfilePage(datasource: datasource, initialTab: 2),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.text('Driving licence'),
      200,
      scrollable: find.byType(Scrollable).last,
    );
    expect(find.text('Driving licence'), findsWidgets);
    expect(find.text('Use URL'), findsWidgets);
    expect(tester.takeException(), isNull);
  });

  test(
    'driver datasource sends typed vehicle and document API methods',
    () async {
      SharedPreferences.setMockInitialValues({'auth.userId': 7});
      final prefs = await SharedPreferences.getInstance();
      final client = DioClient(
        accessTokenReader: () async => 'fixture-token',
        authUserIdReader: () async => 7,
      );
      final requests = <RequestOptions>[];
      client.dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            requests.add(options);
            dynamic data;
            if (options.path.endsWith('/types')) {
              data = [
                {'code': 'SEDAN', 'name': 'Sedan'},
              ];
            } else if (options.path == 'api/v1/document' &&
                options.method == 'GET') {
              data = [
                {
                  'id': 21,
                  'documentType': 'PAN_CARD',
                  'documentUrl': 'https://example.test/pan.pdf',
                  'verificationStatus': 'PENDING',
                },
              ];
            } else if (options.path.endsWith('/upload')) {
              data = {
                'id': 21,
                'documentType': 'PAN_CARD',
                'documentUrl': 'https://example.test/new.pdf',
                'verificationStatus': 'PENDING',
              };
            } else {
              data = {
                'id': 11,
                'vehicleType': 'SEDAN',
                'brand': 'Swift',
                'model': 'Two',
                'registrationNumber': 'KA01AB1234',
                'year': 2022,
              };
            }
            handler.resolve(
              Response(
                requestOptions: options,
                statusCode: 200,
                data: {'success': true, 'statusCode': 200, 'data': data},
              ),
            );
          },
        ),
      );
      final datasource = DriverRemoteDatasource(
        dioClient: client,
        endpoints: RiderApiEndpoints(),
        authLocalDatasource: AuthLocalDatasource(preferences: prefs),
      );

      expect((await datasource.getVehicleTypes()).data!.single.code, 'SEDAN');
      expect(
        (await datasource.getDocuments()).data!.single.documentUrl,
        'https://example.test/pan.pdf',
      );
      expect(
        (await datasource.updateVehicle(11, {'vehicleType': 'SEDAN'})).data!.id,
        11,
      );
      expect(
        (await datasource.uploadDocument(
          documentType: 'PAN_CARD',
          documentUrl: 'https://example.test/new.pdf',
        )).data!.verificationStatus,
        'PENDING',
      );
      expect(requests.map((request) => '${request.method} ${request.path}'), [
        'GET api/v1/vehicle/types',
        'GET api/v1/document',
        'PUT api/v1/vehicle/11',
        'POST api/v1/document/upload',
      ]);
      expect(requests[2].data, {'vehicleType': 'SEDAN'});
      expect(requests[3].data, {
        'documentType': 'PAN_CARD',
        'documentUrl': 'https://example.test/new.pdf',
      });
      expect(
        requests.every(
          (request) => request.headers['x-user-id'].toString() == '7',
        ),
        isTrue,
      );
    },
  );

  testWidgets('personal profile shows the saved gender and birth date', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(440, 1100);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });
    final datasource = await fakeDatasource();
    final profile = DriverProfileModel.fromJson({
      'driverId': 72,
      'driverName': 'Taylor Driver',
      'email': 'taylor@example.test',
      'phoneNumber': '9876543210',
      'gender': 'MALE',
      'dateOfBirth': '1994-03-12',
    });

    await tester.pumpWidget(
      MaterialApp(
        home: CompleteProfilePage(datasource: datasource, profile: profile),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Male'), findsOneWidget);
    expect(find.text('12/3/1994'), findsOneWidget);
  });
}
