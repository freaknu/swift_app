import 'dart:async';

import 'package:swift_app/features/driver/data/datasource/driver_realtime_service.dart';
import 'package:swift_app/features/driver/data/datasource/driver_location_publisher.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/common/payment/domain/ride_payment.dart';
import 'package:swift_app/features/driver/data/model/driver_profile_model.dart';
import 'package:swift_app/features/driver/data/model/driver_statistics_model.dart';
import 'package:swift_app/features/driver/data/model/profile_completeness_model.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/driver/data/model/realtime_ride_event.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// Owns the driver's session state: initial data, availability, location and
/// payment receipts. It has no BuildContext and performs no navigation.
class DriverDashboardController extends ChangeNotifier {
  DriverDashboardController({
    required this.datasource,
    required this.realtimeService,
    required this.locationPublisher,
    required this.onMessage,
    required this.onProfileRequired,
    required this.onProfileReady,
  });

  final DriverRemoteDatasource datasource;
  final DriverRealtimeService realtimeService;
  final DriverLocationPublisher locationPublisher;
  final ValueChanged<String> onMessage;
  final VoidCallback onProfileRequired;
  final VoidCallback onProfileReady;
  bool _disposed = false;
  bool _started = false;
  bool get _active => !_disposed;

  StreamSubscription<String>? _locationErrors;
  StreamSubscription<RealtimeRideEvent>? _paymentEvents;
  final Set<int> _paidRideIds = {};
  DriverProfileModel? _profile;
  List<DriverVehicleModel> _vehicles = const [];
  ProfileCompletenessModel? _completeness;
  DriverStatisticsModel? _statistics;
  bool _loadingStatistics = false;
  String? _statisticsError;
  bool _loading = true;
  bool _mapSettled = false;
  bool _initialStatisticsSettled = false;
  bool _initialOffersSettled = false;
  bool _initialRideSettled = false;
  bool _startupComplete = false;
  bool _online = false;
  bool _busyWithRide = false;
  bool _resumingLocation = false;
  int? _activeRideId;
  bool _refreshing = false;
  bool _updatingStatus = false;
  String? _message;

  DriverProfileModel? get profile => _profile;
  ProfileCompletenessModel? get completeness => _completeness;
  DriverStatisticsModel? get statistics => _statistics;
  bool get loadingStatistics => _loadingStatistics;
  String? get statisticsError => _statisticsError;
  bool get loading => _loading;
  bool get startupComplete => _startupComplete;
  bool get homeReady =>
      _profile != null &&
      _message == null &&
      !_loading &&
      _mapSettled &&
      _initialStatisticsSettled &&
      _initialOffersSettled &&
      _initialRideSettled;
  bool get online => _online;
  bool get busyWithRide => _busyWithRide;
  bool get refreshing => _refreshing;
  bool get updatingStatus => _updatingStatus;
  String? get message => _message;
  List<DriverVehicleModel> get vehicles => _vehicles;

  void start() {
    if (_started || _disposed) return;
    _started = true;
    _locationErrors = locationPublisher.errors.listen((message) {
      if (_active) onMessage(message);
    });
    _paymentEvents = realtimeService.rideEvents.listen((event) {
      final driverId = _profile?.driverId;
      if (!_active ||
          driverId == null ||
          event.driverId != driverId ||
          event.raw['eventType'] != 'PAYMENT_COMPLETED' ||
          event.raw['paymentStatus'] != 'COMPLETED') {
        return;
      }
      try {
        final payment = RidePayment.fromJson(event.raw);
        if (!payment.isCompleted || !_paidRideIds.add(payment.rideId)) return;
        if (_paidRideIds.length > 100) _paidRideIds.remove(_paidRideIds.first);
        onMessage(
          'Payment received for ride #${payment.rideId}: ${payment.amountLabel}',
        );
        unawaited(loadStatistics(driverId));
      } on FormatException {
        // Unrelated or malformed messages must not become payment receipts.
      }
    });
    unawaited(loadDriver());
  }

  void _update(VoidCallback mutation) {
    if (_disposed) return;
    mutation();
    _updateStartupState();
    notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    _locationErrors?.cancel();
    _paymentEvents?.cancel();
    unawaited(locationPublisher.stop());
    realtimeService.disconnect();
    super.dispose();
  }

  void _updateStartupState() {
    if (_startupComplete) return;
    if (_message != null ||
        (!_loading &&
            _mapSettled &&
            _initialStatisticsSettled &&
            _initialOffersSettled &&
            _initialRideSettled)) {
      _startupComplete = true;
    }
  }

  void settleStartup({
    bool map = false,
    bool offers = false,
    bool ride = false,
  }) {
    if (!_active) return;
    _update(() {
      _mapSettled = _mapSettled || map;
      _initialOffersSettled = _initialOffersSettled || offers;
      _initialRideSettled = _initialRideSettled || ride;
    });
  }

  Future<void> loadDriver() async {
    if (!_active || _refreshing || _updatingStatus) return;
    _update(() {
      _refreshing = true;
      _loading = _profile == null;
      _message = null;
    });

    try {
      // Register every future with the same wait so failures also settle the
      // first load without leaving another request unobserved.
      final responses = await Future.wait<Object>([
        datasource.getProfile(),
        datasource.getVehicles(),
        datasource.getProfileCompleteness(),
      ]);
      final profileResponse =
          responses[0] as ApiResponseFormat<DriverProfileModel>;
      final vehicleResponse =
          responses[1] as ApiResponseFormat<List<DriverVehicleModel>>;
      final completenessResponse =
          responses[2] as ApiResponseFormat<ProfileCompletenessModel>;
      if (!_active) return;

      if (!profileResponse.success || profileResponse.data == null) {
        _update(() {
          _refreshing = false;
          _loading = false;
          if (_profile == null) _message = profileResponse.message;
        });
        if (_profile != null) onMessage(profileResponse.message);
        return;
      }

      final profile = profileResponse.data!;
      onProfileReady();
      final vehicles = vehicleResponse.success
          ? vehicleResponse.data ?? profile.vehicles
          : profile.vehicles;
      final driverId = profile.driverId;

      _update(() {
        _profile = profile;
        _vehicles = List.unmodifiable(
          vehicles.where((vehicle) => vehicle.active),
        );
        _busyWithRide =
            profile.driverStatus == 'BUSY' ||
            profile.driverStatus == 'ON_TRIP' ||
            _activeRideId != null;
        _online = profile.driverStatus == 'ONLINE' && !_busyWithRide;
        _loading = false;
        _refreshing = false;
        if (completenessResponse.success && completenessResponse.data != null) {
          _completeness = completenessResponse.data;
        }
        if (driverId == null) {
          _initialStatisticsSettled = true;
          _initialOffersSettled = true;
          _initialRideSettled = true;
        } else if (!_online) {
          _initialOffersSettled = true;
        }
      });

      if (driverId != null) {
        realtimeService.connect(driverId: driverId);
        if (_online || _busyWithRide) {
          unawaited(_resumeLocation(driverId));
        } else if (locationPublisher.isRunning) {
          unawaited(locationPublisher.stop(publishFinalPosition: true));
        }
        unawaited(loadStatistics(driverId));
      }
    } catch (_) {
      if (!_active) return;
      const message = 'Couldn’t load your driver profile. Please try again.';
      _update(() {
        _refreshing = false;
        _loading = false;
        if (_profile == null) _message = message;
      });
      if (_profile != null) onMessage(message);
    }
  }

  Future<void> loadStatistics(int driverId) async {
    if (!_active) return;
    _update(() {
      _loadingStatistics = true;
      _statisticsError = null;
    });
    try {
      final response = await datasource.getStatistics(driverId);
      if (!_active || _profile?.driverId != driverId) return;
      _update(() {
        if (response.success && response.data != null) {
          _statistics = response.data;
        } else {
          _statisticsError = response.message;
        }
      });
    } catch (_) {
      if (_active && _profile?.driverId == driverId) {
        _update(() => _statisticsError = 'Couldn’t load your trip totals.');
      }
    } finally {
      if (_active && _profile?.driverId == driverId) {
        _update(() {
          _loadingStatistics = false;
          _initialStatisticsSettled = true;
        });
      }
    }
  }

  Future<void> _resumeLocation(int driverId) async {
    if (locationPublisher.isRunning || _resumingLocation || !_active) {
      return;
    }
    _resumingLocation = true;
    _update(() => _updatingStatus = true);
    final error = await locationPublisher.start(
      driverId: driverId,
      rideId: _activeRideId,
    );
    _resumingLocation = false;
    if (!_active) return;
    if (error != null) {
      if (_online) await datasource.updateStatus('OFFLINE');
      await locationPublisher.stop();
      if (!_active) return;
      if (_online) _update(() => _online = false);
      onMessage(error);
    }
    _update(() => _updatingStatus = false);
  }

  Future<void> toggleOnline(bool value) async {
    if (!_active ||
        _updatingStatus ||
        _refreshing ||
        _profile == null ||
        _busyWithRide) {
      return;
    }
    if (value && _vehicles.isEmpty) {
      onMessage('Add an active vehicle before going online.');
      onProfileRequired();
      return;
    }
    if (value && _completeness == null) {
      onMessage('Checking your profile. Please try again.');
      unawaited(loadDriver());
      return;
    }
    if (value && !_completeness!.complete) {
      onMessage('Complete your profile before going online.');
      onProfileRequired();
      return;
    }
    final nextStatus = value ? 'ONLINE' : 'OFFLINE';
    HapticFeedback.selectionClick();
    _update(() => _updatingStatus = true);

    final response = await datasource.updateStatus(nextStatus);
    if (!_active) return;
    if (!response.success) {
      _update(() => _updatingStatus = false);
      onMessage(response.message);
      return;
    }

    if (value) {
      final driverId = _profile?.driverId;
      final error = driverId == null
          ? 'Driver profile is not ready yet'
          : await locationPublisher.start(driverId: driverId);
      if (!_active) return;
      if (error != null) {
        await datasource.updateStatus('OFFLINE');
        await locationPublisher.stop();
        if (!_active) return;
        _update(() => _updatingStatus = false);
        onMessage(error);
        return;
      }
    } else {
      await locationPublisher.stop(publishFinalPosition: true);
    }

    if (!_active) return;
    _update(() {
      _updatingStatus = false;
      _online = value;
    });
  }

  Future<void> activeRideChanged(int? rideId) async {
    if (!_active ||
        (rideId == _activeRideId && !(rideId == null && _busyWithRide))) {
      return;
    }
    _activeRideId = rideId;
    if (rideId != null) {
      _update(() {
        _busyWithRide = true;
        _online = false;
      });
      final driverId = _profile?.driverId;
      if (driverId != null) {
        if (!locationPublisher.isRunning && !_resumingLocation) {
          await _resumeLocation(driverId);
        }
        if (!_active) return;
        await locationPublisher.setRideId(rideId);
      }
    } else {
      await locationPublisher.setRideId(null);
      if (!_active) return;
      if (_busyWithRide) {
        _update(() => _busyWithRide = false);
        // Driver service restores ONLINE after a ride ends; load that status.
        unawaited(loadDriver());
      }
    }
  }
}
