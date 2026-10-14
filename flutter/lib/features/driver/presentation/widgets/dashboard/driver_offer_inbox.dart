import 'dart:async';

import 'package:swift_app/features/driver/data/datasource/driver_realtime_service.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/driver/data/model/realtime_ride_event.dart';
import 'package:swift_app/features/driver/data/model/pending_offer_model.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_dashboard_states.dart';
import 'package:swift_app/features/driver/presentation/widgets/dashboard/driver_offer_card.dart';

/// Commands supported by the offer inbox; state stays inside the component.
class DriverOfferInboxActions {
  Future<void> Function()? _refresh;
  bool Function(int)? _hasOffer;

  Future<void> refresh() => _refresh?.call() ?? Future<void>.value();
  bool hasOffer(int rideId) => _hasOffer?.call(rideId) ?? false;
}

class DriverOfferInbox extends StatefulWidget {
  final DriverOfferInboxActions actions;
  final GlobalKey firstOfferCardKey;
  final DriverRemoteDatasource datasource;
  final DriverRealtimeService realtimeService;
  final int? driverId;
  final bool online;
  final VoidCallback onAccepted;
  final ValueChanged<int> onNewOffer;
  final VoidCallback onInitialLoadSettled;

  const DriverOfferInbox({
    super.key,
    required this.actions,
    required this.firstOfferCardKey,
    required this.datasource,
    required this.realtimeService,
    required this.driverId,
    required this.online,
    required this.onAccepted,
    required this.onNewOffer,
    required this.onInitialLoadSettled,
  });

  @override
  State<DriverOfferInbox> createState() => _DriverOfferInboxState();
}

class _DriverOfferInboxState extends State<DriverOfferInbox>
    with WidgetsBindingObserver {
  List<PendingOfferModel> _offers = const [];
  Timer? _pollTimer;
  Timer? _countdownTimer;
  Timer? _noticeTimer;
  StreamSubscription<RealtimeRideEvent>? _realtimeSub;
  final Set<int> _busy = {};
  final Map<int, DateTime> _socketReceivedAt = {};
  final Set<int> _revealedOfferIds = {};
  bool _fetching = false;
  bool _foreground = true;
  bool _refreshRequested = false;
  bool _loadingInitial = true;
  bool _initialLoadReported = false;
  String? _error;
  String? _notice;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _foreground =
        WidgetsBinding.instance.lifecycleState == null ||
        WidgetsBinding.instance.lifecycleState == AppLifecycleState.resumed;
    widget.actions._refresh = _fetchOffers;
    widget.actions._hasOffer = hasOffer;
    _realtimeSub = widget.realtimeService.rideEvents.listen(_onRealtimeEvent);
    unawaited(_fetchOffers());
    _startRecoveryTimers();
  }

  void _startRecoveryTimers() {
    _pollTimer?.cancel();
    _countdownTimer?.cancel();
    if (!_foreground) return;
    _pollTimer = Timer.periodic(
      const Duration(seconds: 5),
      (_) => unawaited(_fetchOffers()),
    );
    _countdownTimer = Timer.periodic(
      const Duration(seconds: 1),
      (_) => _tick(),
    );
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    final foreground = state == AppLifecycleState.resumed;
    if (_foreground == foreground) return;
    _foreground = foreground;
    _startRecoveryTimers();
    if (_foreground) {
      _tick();
      unawaited(_fetchOffers(queueIfFetching: true));
    }
  }

  void _tick() {
    if (!mounted || _offers.isEmpty) return;
    final live = _offers.where((offer) => !offer.isExpired).toList();
    // The countdown owns its small repaint; only expiry changes the inbox.
    if (live.length != _offers.length) setState(() => _offers = live);
  }

  void _onRealtimeEvent(RealtimeRideEvent event) {
    final rideId = event.rideId;
    if (rideId == null || !mounted || !widget.online) return;
    if (event.isOffer) {
      final incoming = PendingOfferModel.fromJson(event.raw);
      if (incoming.isExpired) return;
      final wasKnown = _offers.any((offer) => offer.rideId == rideId);
      _socketReceivedAt[rideId] = DateTime.now();
      setState(() {
        _loadingInitial = false;
        _error = null;
        _offers = [
          for (final offer in _offers)
            if (offer.rideId != rideId) offer,
          wasKnown
              ? _offers
                    .firstWhere((offer) => offer.rideId == rideId)
                    .merge(incoming)
              : incoming,
        ]..sort(_compareOffers);
      });
      if (!wasKnown) {
        _notifyNewOffer(rideId);
        ScaffoldMessenger.of(context).hideCurrentSnackBar();
        HapticFeedback.mediumImpact();
        _setNotice('New request received. Review the trip below.');
      }
    } else if (event.isOfferWithdrawn) {
      final wasKnown = _offers.any((offer) => offer.rideId == rideId);
      _socketReceivedAt.remove(rideId);
      if (wasKnown) {
        setState(
          () => _offers = _offers
              .where((offer) => offer.rideId != rideId)
              .toList(),
        );
        final reason = event.raw['reason']?.toString();
        _setNotice(
          reason == 'EXPIRED'
              ? 'That request has expired'
              : 'That request is no longer available',
        );
      }
    }
  }

  static int _compareOffers(PendingOfferModel a, PendingOfferModel b) {
    final aExpiry = a.localDeadline ?? a.expiresAt;
    final bExpiry = b.localDeadline ?? b.expiresAt;
    if (aExpiry == null) return bExpiry == null ? 0 : 1;
    if (bExpiry == null) return -1;
    return aExpiry.compareTo(bExpiry);
  }

  void _setNotice(String message) {
    _noticeTimer?.cancel();
    if (!mounted) return;
    setState(() => _notice = message);
    _noticeTimer = Timer(const Duration(seconds: 4), () {
      if (mounted) setState(() => _notice = null);
    });
  }

  Widget _buildNotice(BuildContext context) {
    return Semantics(
      liveRegion: true,
      child: AppCard(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
        child: Row(
          children: [
            AppIcon(
              SwiftIcons.info,
              size: 18,
              color: Theme.of(context).colorScheme.primary,
            ),
            const SizedBox(width: 9),
            Expanded(
              child: Text(
                _notice!,
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  void didUpdateWidget(covariant DriverOfferInbox oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.actions != oldWidget.actions) {
      oldWidget.actions._refresh = null;
      oldWidget.actions._hasOffer = null;
      widget.actions._refresh = _fetchOffers;
      widget.actions._hasOffer = hasOffer;
    }
    if (widget.driverId != oldWidget.driverId) _revealedOfferIds.clear();
    if (widget.online && !oldWidget.online) {
      setState(() => _loadingInitial = true);
      _fetchOffers();
    } else if (!widget.online && oldWidget.online) {
      setState(() {
        _offers = const [];
        _error = null;
        _notice = null;
        _loadingInitial = false;
      });
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    widget.actions._refresh = null;
    widget.actions._hasOffer = null;
    _realtimeSub?.cancel();
    _pollTimer?.cancel();
    _countdownTimer?.cancel();
    _noticeTimer?.cancel();
    super.dispose();
  }

  void _reportInitialLoadSettled() {
    if (_initialLoadReported) return;
    _initialLoadReported = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) widget.onInitialLoadSettled();
    });
  }

  Future<void> _fetchOffers({bool queueIfFetching = false}) async {
    if (!widget.online) {
      _reportInitialLoadSettled();
      return;
    }
    if (!mounted || !_foreground) return;
    if (_fetching) {
      _refreshRequested = _refreshRequested || queueIfFetching;
      return;
    }
    _fetching = true;
    try {
      final response = await widget.datasource.getPendingOffers();
      if (!mounted || !widget.online) return;
      if (!response.success || response.data == null) {
        setState(() {
          _loadingInitial = false;
          if (_offers.isEmpty) _error = response.message;
        });
        return;
      }
      final previous = {for (final offer in _offers) offer.rideId: offer};
      final fresh = <PendingOfferModel>[];
      for (final offer in response.data!) {
        if (offer.rideId == null || offer.isExpired) continue;
        fresh.add(previous[offer.rideId]?.merge(offer) ?? offer);
      }
      // Kafka persistence and the WebSocket push may arrive in either order.
      // Keep a freshly pushed offer briefly while the inbox catches up.
      final now = DateTime.now();
      for (final offer in _offers) {
        final id = offer.rideId;
        final pushedAt = id == null ? null : _socketReceivedAt[id];
        if (id != null &&
            pushedAt != null &&
            now.difference(pushedAt) < const Duration(seconds: 4) &&
            !offer.isExpired &&
            !fresh.any((item) => item.rideId == id)) {
          fresh.add(offer);
        }
      }
      fresh.sort(_compareOffers);
      // Empty successful polls are common while waiting for a request. Keep
      // that steady state from rebuilding the inbox every five seconds.
      if (_loadingInitial ||
          _error != null ||
          _offers.isNotEmpty ||
          fresh.isNotEmpty) {
        setState(() {
          _offers = fresh;
          _loadingInitial = false;
          _error = null;
        });
      }
      for (final offer in fresh) {
        final id = offer.rideId;
        if (id != null && !_revealedOfferIds.contains(id)) {
          _notifyNewOffer(id);
          break;
        }
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _loadingInitial = false;
          if (_offers.isEmpty) _error = 'Couldn’t load ride requests.';
        });
      }
    } finally {
      _fetching = false;
      _reportInitialLoadSettled();
      if (_refreshRequested && mounted) {
        _refreshRequested = false;
        unawaited(_fetchOffers());
      }
    }
  }

  bool hasOffer(int rideId) =>
      widget.online && _offers.any((offer) => offer.rideId == rideId);

  void _notifyNewOffer(int rideId) {
    if (_revealedOfferIds.add(rideId)) widget.onNewOffer(rideId);
  }

  Future<void> _answer(
    PendingOfferModel offer,
    Future<ApiResponseFormat<bool>> Function() call,
    String okMessage, {
    bool accepted = false,
  }) async {
    final rideId = offer.rideId;
    if (rideId == null || offer.isExpired || _busy.contains(rideId)) return;
    HapticFeedback.selectionClick();
    setState(() => _busy.add(rideId));
    try {
      final response = await call();
      if (!mounted) return;
      final acknowledged = response.success && response.data == true;
      setState(() {
        _busy.remove(rideId);
        if (acknowledged) {
          _offers = _offers.where((item) => item.rideId != rideId).toList();
          _socketReceivedAt.remove(rideId);
        }
      });
      _setNotice(acknowledged ? okMessage : response.message);
      if (acknowledged && accepted) {
        if (mounted) widget.onAccepted();
      }
      if (!acknowledged) _fetchOffers();
    } catch (_) {
      if (mounted) {
        setState(() => _busy.remove(rideId));
        _setNotice('Couldn’t respond. Please try again.');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!widget.online) {
      return const SliverToBoxAdapter(child: SizedBox.shrink());
    }
    if (_loadingInitial && _offers.isEmpty) {
      return const SliverToBoxAdapter(
        child: Padding(
          padding: EdgeInsets.fromLTRB(16, 16, 16, 0),
          child: DriverRideLoadingCard(),
        ),
      );
    }
    if (_error != null && _offers.isEmpty) {
      return SliverToBoxAdapter(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
          child: _OfferErrorCard(message: _error!, onRetry: _fetchOffers),
        ),
      );
    }
    if (_offers.isEmpty) {
      return SliverToBoxAdapter(
        child: _notice == null
            ? const SizedBox.shrink()
            : Padding(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
                child: _buildNotice(context),
              ),
      );
    }
    final offer = _offers.first;
    final rideId = offer.rideId;
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (_notice != null) ...[
              _buildNotice(context),
              const SizedBox(height: 12),
            ],
            DriverOfferCard(
              key: widget.firstOfferCardKey,
              offer: offer,
              busy: rideId == null || _busy.contains(rideId),
              loadingDetails: !offer.hasTripDetails,
              onAccept: () => _answer(
                offer,
                () => widget.datasource.acceptOffer(rideId: rideId!),
                'Accepted. Confirming your assignment…',
                accepted: true,
              ),
              onReject: () => _answer(
                offer,
                () => widget.datasource.rejectOffer(
                  rideId: rideId!,
                  reason: 'Declined by driver',
                ),
                'Request declined',
              ),
            ),
            if (_offers.length > 1)
              Padding(
                padding: const EdgeInsets.only(top: 8, left: 4),
                child: Text(
                  '${_offers.length - 1} more request${_offers.length == 2 ? '' : 's'} waiting',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _OfferErrorCard extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const _OfferErrorCard({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return AppCard(
      child: Row(
        children: [
          const AppIcon(SwiftIcons.wifiOff),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              'Ride offers unavailable. $message',
              style: theme.textTheme.bodySmall,
            ),
          ),
          TextButton(onPressed: onRetry, child: const Text('Retry')),
        ],
      ),
    );
  }
}

/// Tracks the driver's assigned ride. The server-side active ride lookup
/// restores the controls after reconnect or app restart.
