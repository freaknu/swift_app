import 'dart:async';

import 'package:swift_app/features/driver/data/datasource/driver_realtime_service.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/driver/data/model/driver_profile_model.dart';
import 'package:swift_app/features/driver/data/model/realtime_ride_event.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:flutter/material.dart';

import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_dashboard_states.dart';
import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_active_ride_card.dart';

/// Commands supported by the active ride feed, independent of its State type.
class DriverActiveRideActions {
  Future<void> Function()? _refresh;

  Future<void> refresh() => _refresh?.call() ?? Future<void>.value();
}

class DriverActiveRideFeed extends StatefulWidget {
  final DriverActiveRideActions actions;
  final DriverRemoteDatasource datasource;
  final DriverRealtimeService realtimeService;
  final int? driverId;
  final DriverVehicleModel? vehicle;
  final bool busyWithRide;
  final Future<void> Function(RealtimeRideEvent) onAction;
  final Future<void> Function(RealtimeRideEvent) onCancel;
  final Future<void> Function(RealtimeRideEvent) onChat;
  final ValueChanged<int?> onActiveRideChanged;
  final VoidCallback onInitialLoadSettled;

  const DriverActiveRideFeed({
    super.key,
    required this.actions,
    required this.datasource,
    required this.realtimeService,
    required this.driverId,
    required this.busyWithRide,
    required this.vehicle,
    required this.onAction,
    required this.onCancel,
    required this.onChat,
    required this.onActiveRideChanged,
    required this.onInitialLoadSettled,
  });

  @override
  State<DriverActiveRideFeed> createState() => _DriverActiveRideFeedState();
}

class _DriverActiveRideFeedState extends State<DriverActiveRideFeed>
    with WidgetsBindingObserver {
  final Map<int, RealtimeRideEvent> _rideEvents = {};
  final Map<int, String?> _riderNames = {};
  final Set<int> _loadedRiderNames = {};
  final Set<int> _loadingRiderNames = {};
  final Set<int> _pendingRides = {};
  StreamSubscription<RealtimeRideEvent>? _subscription;
  Timer? _recoveryTimer;
  bool _fetching = false;
  bool _foreground = true;
  bool _refreshRequested = false;
  bool _loadingActive = true;
  bool _initialLoadReported = false;
  String? _error;
  DateTime? _lastAssignmentPush;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _foreground =
        WidgetsBinding.instance.lifecycleState == null ||
        WidgetsBinding.instance.lifecycleState == AppLifecycleState.resumed;
    widget.actions._refresh = refreshActiveRide;
    _subscription = widget.realtimeService.rideEvents.listen(_onRideEvent);
    unawaited(refreshActiveRide());
    _startRecoveryTimer();
  }

  void _startRecoveryTimer() {
    _recoveryTimer?.cancel();
    if (!_foreground) return;
    _recoveryTimer = Timer.periodic(
      const Duration(seconds: 15),
      (_) => unawaited(refreshActiveRide()),
    );
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    final foreground = state == AppLifecycleState.resumed;
    if (_foreground == foreground) return;
    _foreground = foreground;
    _startRecoveryTimer();
    if (_foreground) unawaited(refreshActiveRide());
  }

  @override
  void didUpdateWidget(covariant DriverActiveRideFeed oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.actions != oldWidget.actions) {
      oldWidget.actions._refresh = null;
      widget.actions._refresh = refreshActiveRide;
    }
    if (widget.driverId != oldWidget.driverId) {
      _rideEvents.clear();
      _riderNames.clear();
      _loadedRiderNames.clear();
      _loadingRiderNames.clear();
      unawaited(refreshActiveRide());
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    widget.actions._refresh = null;
    _subscription?.cancel();
    _recoveryTimer?.cancel();
    super.dispose();
  }

  void _reportInitialLoadSettled() {
    if (_initialLoadReported) return;
    _initialLoadReported = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) widget.onInitialLoadSettled();
    });
  }

  Future<void> refreshActiveRide() async {
    if (!mounted || !_foreground) return;
    if (_fetching) {
      _refreshRequested = true;
      return;
    }
    if (widget.driverId == null) {
      setState(() => _loadingActive = false);
      _reportInitialLoadSettled();
      return;
    }
    _fetching = true;
    try {
      final response = await widget.datasource.getActiveRideId();
      if (!mounted) return;
      if (!response.success) {
        setState(() {
          _loadingActive = false;
          if (_rideEvents.isEmpty) _error = response.message;
        });
        return;
      }
      final activeId = response.data;
      if (activeId == null) {
        final recentAssignment =
            _lastAssignmentPush != null &&
            DateTime.now().difference(_lastAssignmentPush!) <
                const Duration(seconds: 5);
        if (!recentAssignment) {
          if (_rideEvents.isNotEmpty) setState(() => _rideEvents.clear());
          _riderNames.clear();
          _loadedRiderNames.clear();
          widget.onActiveRideChanged(null);
        }
      } else {
        widget.onActiveRideChanged(activeId);
        await _hydrateRide(activeId);
      }
      if (mounted && (_loadingActive || _error != null)) {
        setState(() {
          _loadingActive = false;
          _error = null;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _loadingActive = false;
          if (_rideEvents.isEmpty) _error = 'Couldn’t load your active ride.';
        });
      }
    } finally {
      _fetching = false;
      _reportInitialLoadSettled();
      if (_refreshRequested && mounted) {
        _refreshRequested = false;
        unawaited(refreshActiveRide());
      }
    }
  }

  Future<void> _hydrateRide(int rideId) async {
    final response = await widget.datasource.getRideDetail(rideId);
    if (!mounted || !response.success || response.data == null) return;
    final detail = RealtimeRideEvent.fromRideDetail(response.data!);
    final existing = _rideEvents[rideId];
    final hydrated = existing == null
        ? detail
        : _rideStatusOrder(detail.rideStatus) >=
              _rideStatusOrder(existing.rideStatus)
        ? existing.merge(detail)
        : detail.merge(existing);
    setState(() {
      if (hydrated.isFinished) {
        _rideEvents.remove(rideId);
        _riderNames.remove(rideId);
        _loadedRiderNames.remove(rideId);
      } else {
        _rideEvents[rideId] = hydrated;
      }
    });
    if (!hydrated.isFinished) unawaited(_loadRiderSummary(rideId));
  }

  Future<void> _loadRiderSummary(int rideId) async {
    if (_loadedRiderNames.contains(rideId) || !_loadingRiderNames.add(rideId)) {
      return;
    }
    try {
      final response = await widget.datasource.getRiderSummary(rideId);
      if (!mounted || !_rideEvents.containsKey(rideId)) return;
      if (response.success && response.data != null) {
        setState(() {
          _riderNames[rideId] = response.data!.riderName;
          _loadedRiderNames.add(rideId);
        });
      }
    } catch (_) {
      // Ride actions remain available if the optional rider name cannot load.
    } finally {
      _loadingRiderNames.remove(rideId);
    }
  }

  static int _rideStatusOrder(String status) => switch (status) {
    'DRIVER_ASSIGNED' => 1,
    'DRIVER_ARRIVED' => 2,
    'STARTED' => 3,
    'COMPLETED' || 'CANCELLED' => 4,
    _ => 0,
  };

  void _onRideEvent(RealtimeRideEvent event) {
    final rideId = event.rideId;
    if (rideId == null || !mounted || event.driverId != widget.driverId) return;
    if (event.isOffer || event.isOfferWithdrawn || event.isRequest) return;
    const rideStatuses = {
      'DRIVER_ASSIGNED',
      'DRIVER_ARRIVED',
      'STARTED',
      'COMPLETED',
      'CANCELLED',
    };
    if (!rideStatuses.contains(event.rideStatus)) return;
    final previous = _rideEvents[rideId];
    if (previous != null &&
        event.eventTime != null &&
        previous.eventTime != null &&
        event.eventTime!.isBefore(previous.eventTime!)) {
      return;
    }
    setState(() {
      if (event.isFinished) {
        _rideEvents.remove(rideId);
        _riderNames.remove(rideId);
        _loadedRiderNames.remove(rideId);
      } else {
        _rideEvents[rideId] = previous?.merge(event) ?? event;
      }
      _loadingActive = false;
      _error = null;
    });
    widget.onActiveRideChanged(event.isFinished ? null : rideId);
    if (!event.isFinished) unawaited(_loadRiderSummary(rideId));
    if (!event.isFinished && previous == null) {
      _lastAssignmentPush = DateTime.now();
      unawaited(_hydrateRide(rideId));
      showDriverSnackBar(context, 'Ride #$rideId is assigned to you');
    }
  }

  Future<void> _performAction(
    RealtimeRideEvent event,
    Future<void> Function(RealtimeRideEvent) action,
  ) async {
    final id = event.rideId;
    if (id == null || _pendingRides.contains(id)) return;
    setState(() => _pendingRides.add(id));
    try {
      await action(event);
      await refreshActiveRide();
    } catch (_) {
      if (mounted) {
        showDriverSnackBar(
          context,
          'Couldn’t update this ride. Please try again.',
        );
      }
    } finally {
      if (mounted) setState(() => _pendingRides.remove(id));
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_rideEvents.isEmpty && _loadingActive) {
      return const SliverToBoxAdapter(
        child: Padding(
          padding: EdgeInsets.fromLTRB(12, 18, 12, 24),
          child: DriverRideLoadingCard(),
        ),
      );
    }
    if (_rideEvents.isEmpty && _error != null) {
      return SliverToBoxAdapter(
        child: DriverMessageState(
          icon: SwiftIcons.wifiOff,
          title: 'Ride status unavailable',
          message: _error!,
          actionText: 'Retry',
          onAction: refreshActiveRide,
        ),
      );
    }
    if (_rideEvents.isEmpty) {
      if (!widget.busyWithRide) {
        return const SliverToBoxAdapter(child: SizedBox.shrink());
      }
      return const SliverToBoxAdapter(
        child: DriverMessageState(
          icon: SwiftIcons.car,
          title: 'Finding your active ride',
          message: 'Your trip is being restored. Pull down to refresh.',
        ),
      );
    }

    final events = _rideEvents.values.toList().reversed.toList();
    return SliverList.separated(
      itemCount: events.length,
      separatorBuilder: (context, index) => const SizedBox(height: 10),
      itemBuilder: (context, index) {
        final event = events[index];
        return Padding(
          key: ValueKey(event.rideId),
          padding: EdgeInsets.fromLTRB(
            16,
            index == 0 ? 16 : 0,
            16,
            index == events.length - 1 ? 8 : 0,
          ),
          child: DriverActiveRideCard(
            key: ValueKey(event.rideId),
            event: event,
            riderName: _riderNames[event.rideId],
            vehicle: widget.vehicle,
            busy: _pendingRides.contains(event.rideId),
            onAction: () => _performAction(event, widget.onAction),
            onCancel: () => _performAction(event, widget.onCancel),
            onChat: () => widget.onChat(event),
          ),
        );
      },
    );
  }
}
