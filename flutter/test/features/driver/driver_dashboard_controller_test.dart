import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/driver/data/datasource/driver_location_publisher.dart';
import 'package:swift_app/features/driver/data/datasource/driver_realtime_service.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/driver/data/model/driver_profile_model.dart';
import 'package:swift_app/features/driver/data/model/driver_statistics_model.dart';
import 'package:swift_app/features/driver/data/model/profile_completeness_model.dart';
import 'package:swift_app/features/driver/data/model/realtime_ride_event.dart';
import 'package:swift_app/features/driver/presentation/controllers/driver_dashboard_controller.dart';

ApiResponseFormat<T> _ok<T>(T data) => ApiResponseFormat.success(
  data: data,
  statusCode: 200,
  message: 'OK',
  endpoint: 'fixture',
);

const _profile = DriverProfileModel(
  driverId: 7,
  driverName: 'Test Driver',
  driverStatus: 'ONLINE',
);
const _totals = DriverStatisticsModel(
  completedTrips: 1,
  cancelledTrips: 0,
  lifetimeDistanceKm: 5,
  lifetimeEarning: 100,
  averageRating: 5,
);
const _completeness = ProfileCompletenessModel(
  complete: true,
  personal: CompletenessSection(complete: true, missing: []),
  documents: CompletenessSection(complete: true, missing: []),
  vehicle: CompletenessSection(complete: true, missing: []),
  missing: [],
);

class _Datasource implements DriverRemoteDatasource {
  int profileCalls = 0;
  int statisticsCalls = 0;
  Completer<ApiResponseFormat<DriverProfileModel>>? profile;
  Completer<ApiResponseFormat<DriverStatisticsModel>>? statistics;

  @override
  Future<ApiResponseFormat<DriverProfileModel>> getProfile() async {
    profileCalls++;
    return profile == null ? _ok(_profile) : profile!.future;
  }

  @override
  Future<ApiResponseFormat<List<DriverVehicleModel>>> getVehicles() async =>
      _ok(const [
        DriverVehicleModel(id: 3, active: true, vehicleType: 'SEDAN'),
      ]);

  @override
  Future<ApiResponseFormat<ProfileCompletenessModel>>
  getProfileCompleteness() async => _ok(_completeness);

  @override
  Future<ApiResponseFormat<DriverStatisticsModel>> getStatistics(int id) async {
    statisticsCalls++;
    return statistics == null ? _ok(_totals) : statistics!.future;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _Realtime implements DriverRealtimeService {
  final events = StreamController<RealtimeRideEvent>.broadcast();
  int connects = 0;
  int disconnects = 0;

  @override
  Stream<RealtimeRideEvent> get rideEvents => events.stream;

  @override
  void connect({required int driverId}) => connects++;

  @override
  void disconnect() => disconnects++;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _Location implements DriverLocationPublisher {
  final errorsController = StreamController<String>.broadcast();
  final rideIds = <int?>[];
  int stops = 0;

  @override
  Stream<String> get errors => errorsController.stream;

  @override
  bool get isRunning => true;

  @override
  Future<void> stop({bool publishFinalPosition = false}) async => stops++;

  @override
  Future<void> setRideId(int? id) async => rideIds.add(id);

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _Fixture {
  final datasource = _Datasource();
  final realtime = _Realtime();
  final location = _Location();
  final messages = <String>[];
  late final controller = DriverDashboardController(
    datasource: datasource,
    realtimeService: realtime,
    locationPublisher: location,
    onMessage: messages.add,
    onProfileRequired: () {},
    onProfileReady: () {},
  );
  bool disposed = false;

  Future<void> dispose() async {
    if (!disposed) {
      disposed = true;
      controller.dispose();
    }
    await realtime.events.close();
    await location.errorsController.close();
  }
}

Future<void> _flush() => Future<void>.delayed(Duration.zero);

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test(
    'startup waits for statistics and component readiness, then stays open',
    () async {
      final fixture = _Fixture();
      addTearDown(fixture.dispose);
      fixture.datasource.statistics = Completer();
      fixture.controller.start();
      await _flush();

      expect(fixture.controller.profile?.driverId, 7);
      expect(fixture.controller.startupComplete, isFalse);
      fixture.controller.settleStartup(map: true, offers: true, ride: true);
      expect(fixture.controller.startupComplete, isFalse);

      fixture.datasource.statistics!.complete(_ok(_totals));
      await _flush();
      expect(fixture.controller.startupComplete, isTrue);
      expect(fixture.controller.loadingStatistics, isFalse);
      expect(() => fixture.controller.vehicles.clear(), throwsUnsupportedError);

      fixture.datasource.profile = Completer();
      final refresh = fixture.controller.loadDriver();
      expect(fixture.controller.refreshing, isTrue);
      expect(fixture.controller.startupComplete, isTrue);
      fixture.datasource.profile!.complete(_ok(_profile));
      await refresh;
      expect(fixture.controller.startupComplete, isTrue);
    },
  );

  test(
    'starting twice does not duplicate subscriptions or profile requests',
    () async {
      final fixture = _Fixture();
      addTearDown(fixture.dispose);
      fixture.controller.start();
      fixture.controller.start();
      await _flush();
      expect(fixture.datasource.profileCalls, 1);
      expect(fixture.realtime.connects, 1);

      fixture.location.errorsController.add('GPS unavailable');
      await _flush();
      expect(fixture.messages, ['GPS unavailable']);
    },
  );

  test('disposing during profile load ignores the late response', () async {
    final fixture = _Fixture();
    addTearDown(fixture.dispose);
    fixture.datasource.profile = Completer();
    var notifications = 0;
    fixture.controller.addListener(() => notifications++);
    fixture.controller.start();
    expect(notifications, 1);
    fixture.disposed = true;
    fixture.controller.dispose();
    fixture.datasource.profile!.complete(_ok(_profile));
    await _flush();

    expect(notifications, 1);
    expect(fixture.controller.profile, isNull);
    expect(fixture.realtime.connects, 0);
    expect(fixture.realtime.disconnects, 1);
    expect(fixture.location.stops, 1);
    await fixture.controller.loadDriver();
    expect(fixture.datasource.profileCalls, 1);
  });

  test(
    'payment receipt is validated, scoped to the driver and deduplicated',
    () async {
      final fixture = _Fixture();
      addTearDown(fixture.dispose);
      fixture.controller.start();
      await _flush();
      final receipt = <String, dynamic>{
        'eventType': 'PAYMENT_COMPLETED',
        'paymentStatus': 'COMPLETED',
        'driverId': 7,
        'riderId': 20,
        'rideId': 42,
        'amount': 10000,
        'currency': 'INR',
        'razorpayPaymentId': 'pay_test_receipt',
      };
      fixture.realtime.events
        ..add(RealtimeRideEvent.fromJson({...receipt, 'driverId': 99}))
        ..add(RealtimeRideEvent.fromJson({...receipt, 'amount': 'invalid'}))
        ..add(RealtimeRideEvent.fromJson(receipt))
        ..add(RealtimeRideEvent.fromJson(receipt));
      await _flush();

      expect(fixture.messages, ['Payment received for ride #42: ₹100.00']);
      expect(fixture.datasource.statisticsCalls, 2);
    },
  );

  test(
    'active ride locks availability and ending it restores server status',
    () async {
      final fixture = _Fixture();
      addTearDown(fixture.dispose);
      fixture.controller.start();
      await _flush();
      expect(fixture.controller.online, isTrue);

      await fixture.controller.activeRideChanged(42);
      expect(fixture.controller.busyWithRide, isTrue);
      expect(fixture.controller.online, isFalse);
      await fixture.controller.activeRideChanged(null);
      await _flush();

      expect(fixture.location.rideIds, [42, null]);
      expect(fixture.controller.busyWithRide, isFalse);
      expect(fixture.controller.online, isTrue);
      expect(fixture.datasource.profileCalls, 2);
    },
  );
}
