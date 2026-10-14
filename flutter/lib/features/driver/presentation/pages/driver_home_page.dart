import 'dart:async';

import 'package:swift_app/features/driver/data/datasource/driver_realtime_service.dart';
import 'package:swift_app/features/driver/data/datasource/driver_location_publisher.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/core/notifications/notification_inbox_datasource.dart';
import 'package:swift_app/core/notifications/push_notification_service.dart';
import 'package:swift_app/features/common/chat/data/datasource/chat_realtime_service.dart';
import 'package:swift_app/features/common/chat/data/datasource/chat_remote_datasource.dart';
import 'package:swift_app/features/common/chat/presentation/pages/chat_page.dart';
import 'package:swift_app/features/driver/presentation/pages/complete_profile_page.dart';
import 'package:swift_app/features/driver/presentation/pages/driver_trips_tab.dart';
import 'package:swift_app/features/driver/presentation/pages/driver_account_tab.dart';
import 'package:swift_app/features/driver/data/model/realtime_ride_event.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/swift_bottom_navigation_bar.dart';
import 'package:swift_app/features/common/shared/widgets/lazy_tab_stack.dart';
import 'package:swift_app/features/common/shared/widgets/app_loading_page.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/widgets/auth_home_ready_reporter.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:swift_app/features/driver/presentation/controllers/driver_dashboard_controller.dart';
import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_active_ride_feed.dart';
import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_offer_inbox.dart';
import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_dashboard_states.dart';
import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_dashboard_header.dart';
import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_availability_card.dart';
import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_map_section.dart';
import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_lifetime_summary.dart';

export 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_offer_card.dart'
    show DriverOfferCard;

class DriverHomePage extends StatefulWidget {
  final DriverRemoteDatasource datasource;
  final DriverRealtimeService realtimeService;
  final DriverLocationPublisher locationPublisher;
  final ChatRemoteDatasource? chatDatasource;
  final ChatRealtimeService? chatRealtime;
  final Future<bool> Function()? onHomeReady;

  const DriverHomePage({
    super.key,
    required this.datasource,
    required this.realtimeService,
    required this.locationPublisher,
    this.chatDatasource,
    this.chatRealtime,
    this.onHomeReady,
  });

  @override
  State<DriverHomePage> createState() => _DriverHomePageState();
}

class _DriverHomePageState extends State<DriverHomePage>
    with WidgetsBindingObserver {
  final DriverActiveRideActions _rideActions = DriverActiveRideActions();
  final DriverOfferInboxActions _offerActions = DriverOfferInboxActions();
  final GlobalKey _firstOfferCardKey = GlobalKey();
  final GlobalKey _navigationKey = GlobalKey();
  final ScrollController _scrollController = ScrollController();
  bool _offerRevealScheduled = false;
  int? _offerToReveal;
  late final DriverDashboardController _dashboard;
  int _selectedTab = 0;
  bool _startupWasComplete = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _dashboard = DriverDashboardController(
      datasource: widget.datasource,
      realtimeService: widget.realtimeService,
      locationPublisher: widget.locationPublisher,
      onMessage: (message) {
        if (mounted) showDriverSnackBar(context, message);
      },
      onProfileRequired: () => unawaited(_openCompleteProfile()),
      onProfileReady: () {
        if (sl.isRegistered<PushNotificationService>()) {
          unawaited(sl<PushNotificationService>().syncAfterProfileReady());
        }
      },
    )..addListener(_dashboardChanged);
    _dashboard.start();
  }

  void _dashboardChanged() {
    if (!mounted) return;
    setState(() {});
    final pendingOffer = _offerToReveal;
    if (!_startupWasComplete && _dashboard.startupComplete) {
      _startupWasComplete = true;
      if (pendingOffer != null) _revealNewOffer(pendingOffer);
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _scrollController.dispose();
    _dashboard.removeListener(_dashboardChanged);
    _dashboard.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      unawaited(_refreshAfterResume());
    }
  }

  Future<void> _refreshAfterResume() async {
    await _dashboard.loadDriver();
    if (!mounted) return;
    unawaited(_offerActions.refresh());
    unawaited(_rideActions.refresh());
  }

  Future<void> _openNotification(InboxNotification item) async {
    if (item.type == 'CHAT_MESSAGE' && item.rideId != null) {
      final response = await widget.datasource.getRideDetail(item.rideId!);
      if (!mounted) return;
      final ride = response.data;
      if (response.success && ride != null) {
        final event = RealtimeRideEvent.fromRideDetail(ride);
        if (const {
          'DRIVER_ASSIGNED',
          'DRIVER_ARRIVED',
          'STARTED',
        }.contains(event.rideStatus)) {
          await _openChat(event);
        }
      }
    }
    await _refreshAfterResume();
    if (!mounted) return;
    final profileTab = switch (item.type) {
      'DOCUMENT_APPROVED' || 'DOCUMENT_REJECTED' || 'DOCUMENT_PENDING' => 2,
      'VEHICLE_VERIFIED' || 'VEHICLE_REJECTED' || 'VEHICLE_PENDING' => 1,
      'DRIVER_SUSPENDED' || 'DRIVER_REACTIVATED' => 0,
      _ => null,
    };
    if (profileTab != null) {
      await _openCompleteProfile(initialTab: profileTab);
      return;
    }
    if (!mounted || !_scrollController.hasClients) return;
    unawaited(
      _scrollController.animateTo(
        0,
        duration: const Duration(milliseconds: 250),
        curve: Curves.easeOut,
      ),
    );
  }

  Future<void> _openCompleteProfile({int initialTab = 0}) async {
    await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => CompleteProfilePage(
          datasource: widget.datasource,
          profile: _dashboard.profile,
          completeness: _dashboard.completeness,
          initialTab: initialTab,
        ),
      ),
    );
    if (mounted) unawaited(_dashboard.loadDriver());
  }

  void _revealNewOffer(int rideId) {
    _offerToReveal = rideId;
    if (!_dashboard.startupComplete) return;
    if (_offerRevealScheduled) return;
    _offerRevealScheduled = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _offerRevealScheduled = false;
      if (!mounted || ModalRoute.of(context)?.isCurrent == false) return;
      final pendingRideId = _offerToReveal;
      if (pendingRideId == null || !_offerActions.hasOffer(pendingRideId)) {
        return;
      }
      if (_selectedTab != 0) setState(() => _selectedTab = 0);
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted ||
            _selectedTab != 0 ||
            ModalRoute.of(context)?.isCurrent == false ||
            !_offerActions.hasOffer(pendingRideId)) {
          return;
        }
        final cardContext = _firstOfferCardKey.currentContext;
        if (cardContext == null) return;
        final cardBox = cardContext.findRenderObject();
        final navigationBox = _navigationKey.currentContext?.findRenderObject();
        var alignment = 0.0;
        if (cardBox is RenderBox &&
            navigationBox is RenderBox &&
            _scrollController.hasClients) {
          final spareHeight =
              _scrollController.position.viewportDimension -
              cardBox.size.height;
          if (spareHeight > 0) {
            // The body extends below the frosted bar. Reveal the entire offer,
            // including its actions, above that surface rather than behind it.
            alignment = (1 - (navigationBox.size.height + 16) / spareHeight)
                .clamp(0.0, 0.95)
                .toDouble();
          }
        }
        unawaited(
          Scrollable.ensureVisible(
            cardContext,
            alignment: alignment,
            duration: MediaQuery.disableAnimationsOf(context)
                ? Duration.zero
                : const Duration(milliseconds: 360),
            curve: Curves.easeOutCubic,
          ),
        );
      });
      WidgetsBinding.instance.ensureVisualUpdate();
    });
    WidgetsBinding.instance.ensureVisualUpdate();
  }

  Future<void> _runRideAction(RealtimeRideEvent event) async {
    final rideId = event.rideId;
    final driverId = _dashboard.profile?.driverId;

    if (rideId == null || driverId == null) {
      showDriverSnackBar(context, 'Driver profile is not ready yet');
      return;
    }

    HapticFeedback.selectionClick();
    final status = event.rideStatus;
    final response = switch (status) {
      'DRIVER_ASSIGNED' => await widget.datasource.driverArrived(
        rideId: rideId,
        driverId: driverId,
      ),
      'DRIVER_ARRIVED' => await widget.datasource.startRide(
        rideId: rideId,
        driverId: driverId,
      ),
      'STARTED' => await widget.datasource.completeRide(
        rideId: rideId,
        driverId: driverId,
      ),
      _ => null,
    };

    if (!mounted || response == null) return;

    showDriverSnackBar(
      context,
      response.success ? 'Ride updated' : response.message,
    );
  }

  Future<void> _cancelRide(RealtimeRideEvent event) async {
    final rideId = event.rideId;
    final driverId = _dashboard.profile?.driverId;

    if (rideId == null || driverId == null) {
      showDriverSnackBar(context, 'Driver profile is not ready yet');
      return;
    }

    final confirmed = await showCupertinoDialog<bool>(
      context: context,
      builder: (context) => CupertinoAlertDialog(
        title: const Text('Cancel this ride?'),
        content: Text(
          'Ride #$rideId will be cancelled. This cannot be undone.',
        ),
        actions: [
          CupertinoDialogAction(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Keep ride'),
          ),
          CupertinoDialogAction(
            isDestructiveAction: true,
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Cancel ride'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    HapticFeedback.mediumImpact();
    final response = await widget.datasource.cancelRide(
      rideId: rideId,
      driverId: driverId,
      reason: 'Cancelled by driver',
    );

    if (!mounted) return;

    showDriverSnackBar(
      context,
      response.success ? 'Ride cancelled' : response.message,
    );
  }

  Future<void> _openChat(RealtimeRideEvent event) async {
    final rideId = event.rideId;
    final driverId = _dashboard.profile?.driverId ?? event.driverId;
    final riderId = event.riderId;
    if (rideId == null || driverId == null || riderId == null) {
      showDriverSnackBar(context, 'Chat details are not ready yet');
      return;
    }
    await Navigator.of(context).push(
      CupertinoPageRoute(
        builder: (_) => ChatPage(
          rideId: rideId,
          currentUserId: driverId,
          role: 'DRIVER',
          rideStatus: event.rideStatus,
          riderId: riderId,
          driverId: driverId,
          datasource: widget.chatDatasource ?? sl<ChatRemoteDatasource>(),
          realtime: widget.chatRealtime ?? sl<ChatRealtimeService>(),
          rideStatusReader: () async {
            final response = await widget.datasource.getRideDetail(rideId);
            return response.data?.currentStatus;
          },
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AuthHomeReadyReporter(
      ready: _dashboard.homeReady,
      onReady: widget.onHomeReady,
      child: AppStartupGate(
        ready: _dashboard.startupComplete,
        child: Scaffold(
          extendBody: true,
          bottomNavigationBar: SwiftBottomNavigationBar(
            key: _navigationKey,
            selectedIndex: _selectedTab,
            onDestinationSelected: (index) {
              if (index == _selectedTab) return;
              HapticFeedback.selectionClick();
              FocusManager.instance.primaryFocus?.unfocus();
              setState(() => _selectedTab = index);
              if (index == 1 && _dashboard.profile?.driverId != null) {
                unawaited(
                  _dashboard.loadStatistics(_dashboard.profile!.driverId!),
                );
              }
            },
            destinations: const [
              SwiftNavigationDestination(icon: SwiftIcons.car, label: 'Drive'),
              SwiftNavigationDestination(
                icon: SwiftIcons.receiptText,
                label: 'Trips',
              ),
              SwiftNavigationDestination(
                icon: SwiftIcons.userRound,
                label: 'Account',
              ),
            ],
          ),
          body: AppGlassBackground(
            child: SafeArea(
              bottom: false,
              child: Center(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 700),
                  child: LazyTabStack(
                    index: _selectedTab,
                    builders: [
                      (_) => CustomScrollView(
                        cacheExtent: _dashboard.startupComplete ? null : 10000,
                        keyboardDismissBehavior:
                            ScrollViewKeyboardDismissBehavior.onDrag,
                        controller: _scrollController,
                        physics: const BouncingScrollPhysics(
                          parent: AlwaysScrollableScrollPhysics(),
                        ),
                        slivers: [
                          CupertinoSliverRefreshControl(
                            onRefresh: _dashboard.loadDriver,
                          ),
                          SliverToBoxAdapter(
                            child: DriverDashboardHeader(
                              profile: _dashboard.profile,
                              vehicle: _dashboard.vehicles.isEmpty
                                  ? null
                                  : _dashboard.vehicles.first,
                              showNotifications: _selectedTab == 0,
                              onOpenNotification: (item) =>
                                  unawaited(_openNotification(item)),
                              onOpenAccount: () =>
                                  setState(() => _selectedTab = 2),
                            ),
                          ),
                          if (!_dashboard.loading &&
                              _dashboard.message == null &&
                              _dashboard.completeness != null &&
                              !_dashboard.completeness!.complete)
                            SliverToBoxAdapter(
                              child: DriverCompleteProfileBanner(
                                completeness: _dashboard.completeness!,
                                onTap: _openCompleteProfile,
                              ),
                            ),
                          if (!_dashboard.loading)
                            SliverToBoxAdapter(
                              child: DriverAvailabilityCard(
                                online: _dashboard.online,
                                busyWithRide: _dashboard.busyWithRide,
                                loading:
                                    _dashboard.loading ||
                                    _dashboard.refreshing ||
                                    _dashboard.updatingStatus,
                                onChanged:
                                    _dashboard.loading ||
                                        _dashboard.refreshing ||
                                        _dashboard.updatingStatus ||
                                        _dashboard.busyWithRide ||
                                        _dashboard.profile == null
                                    ? null
                                    : _dashboard.toggleOnline,
                              ),
                            ),
                          if (!_dashboard.loading && _dashboard.message == null)
                            DriverOfferInbox(
                              actions: _offerActions,
                              firstOfferCardKey: _firstOfferCardKey,
                              datasource: widget.datasource,
                              realtimeService: widget.realtimeService,
                              driverId: _dashboard.profile?.driverId,
                              online: _dashboard.online,
                              onNewOffer: _revealNewOffer,
                              onInitialLoadSettled: () =>
                                  _dashboard.settleStartup(offers: true),
                              onAccepted: () {
                                unawaited(_rideActions.refresh());
                                unawaited(_dashboard.loadDriver());
                              },
                            ),
                          if (!_dashboard.loading && _dashboard.message != null)
                            SliverFillRemaining(
                              hasScrollBody: false,
                              child: DriverMessageState(
                                icon: SwiftIcons.userRound,
                                title: 'Driver profile unavailable',
                                message: _dashboard.message!,
                                actionText: 'Retry',
                                onAction: _dashboard.loadDriver,
                              ),
                            )
                          else if (!_dashboard.loading)
                            DriverActiveRideFeed(
                              actions: _rideActions,
                              datasource: widget.datasource,
                              realtimeService: widget.realtimeService,
                              driverId: _dashboard.profile?.driverId,
                              busyWithRide: _dashboard.busyWithRide,
                              vehicle: _dashboard.vehicles.isEmpty
                                  ? null
                                  : _dashboard.vehicles.first,
                              onAction: _runRideAction,
                              onCancel: _cancelRide,
                              onChat: _openChat,
                              onInitialLoadSettled: () =>
                                  _dashboard.settleStartup(ride: true),
                              onActiveRideChanged: (rideId) => unawaited(
                                _dashboard.activeRideChanged(rideId),
                              ),
                            ),
                          if (!_dashboard.loading && _dashboard.message == null)
                            SliverToBoxAdapter(
                              child: DriverMapSection(
                                active:
                                    _dashboard.online ||
                                    _dashboard.busyWithRide,
                                showLoadingIndicator:
                                    _dashboard.startupComplete,
                                onInitialLoadSettled: () =>
                                    _dashboard.settleStartup(map: true),
                              ),
                            ),
                          if (!_dashboard.loading &&
                              _dashboard.message == null &&
                              _dashboard.profile?.driverId != null)
                            SliverToBoxAdapter(
                              child: DriverLifetimeSummary(
                                statistics: _dashboard.statistics,
                                loading: _dashboard.loadingStatistics,
                                error: _dashboard.statisticsError,
                                onRetry: () => _dashboard.loadStatistics(
                                  _dashboard.profile!.driverId!,
                                ),
                                onSeeTrips: () =>
                                    setState(() => _selectedTab = 1),
                              ),
                            ),
                          if (!_dashboard.loading &&
                              _dashboard.message == null &&
                              _dashboard.vehicles.isNotEmpty)
                            SliverToBoxAdapter(
                              child: DriverVehicleSummary(
                                vehicle: _dashboard.vehicles.first,
                                onTap: () =>
                                    _openCompleteProfile(initialTab: 1),
                              ),
                            ),
                          SliverToBoxAdapter(
                            child: SizedBox(
                              height: MediaQuery.paddingOf(context).bottom + 20,
                            ),
                          ),
                        ],
                      ),
                      (_) => DriverTripsTab(
                        datasource: widget.datasource,
                        driverId: _dashboard.profile?.driverId,
                        statistics: _dashboard.statistics,
                        onRefreshStatistics: () async {
                          final driverId = _dashboard.profile?.driverId;
                          if (driverId != null) {
                            await _dashboard.loadStatistics(driverId);
                          }
                        },
                      ),
                      (_) => DriverAccountTab(
                        datasource: widget.datasource,
                        profile: _dashboard.profile,
                        vehicles: _dashboard.vehicles,
                        completeness: _dashboard.completeness,
                        showNotifications: _selectedTab == 2,
                        onRefreshProfile: _dashboard.loadDriver,
                        onOpenProfile: _openCompleteProfile,
                        onOpenNotifications: (item) =>
                            unawaited(_openNotification(item)),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
