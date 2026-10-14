import 'dart:async';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';

import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/core/notifications/notification_inbox_datasource.dart';
import 'package:swift_app/core/notifications/push_notification_service.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';

class NotificationBell extends StatefulWidget {
  const NotificationBell({
    super.key,
    required this.role,
    this.onOpenNotification,
    this.datasource,
  });

  final String role;
  final ValueChanged<InboxNotification>? onOpenNotification;
  final NotificationInboxDatasource? datasource;

  @override
  State<NotificationBell> createState() => _NotificationBellState();
}

class _NotificationBellState extends State<NotificationBell>
    with WidgetsBindingObserver {
  late NotificationInboxDatasource _datasource;
  Timer? _refreshTimer;
  int _unread = 0;
  Object? _refreshRequest;
  bool _appActive = true;
  bool _visible = true;
  bool _observedVisibility = false;

  @override
  void initState() {
    super.initState();
    _datasource = widget.datasource ?? sl<NotificationInboxDatasource>();
    WidgetsBinding.instance.addObserver(this);
    final lifecycle = WidgetsBinding.instance.lifecycleState;
    _appActive = lifecycle == null || lifecycle == AppLifecycleState.resumed;
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final visible = TickerMode.valuesOf(context).enabled;
    final becameVisible = visible && (!_observedVisibility || !_visible);
    _visible = visible;
    _observedVisibility = true;
    _updatePolling();
    if (becameVisible) unawaited(_refresh());
  }

  void _updatePolling() {
    if (!_appActive || !_visible) {
      _refreshTimer?.cancel();
      _refreshTimer = null;
      return;
    }
    _refreshTimer ??= Timer.periodic(
      const Duration(seconds: 30),
      (_) => unawaited(_refresh()),
    );
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    _appActive = state == AppLifecycleState.resumed;
    _updatePolling();
    if (_appActive) unawaited(_refresh());
  }

  @override
  void didUpdateWidget(covariant NotificationBell oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.role != widget.role ||
        oldWidget.datasource != widget.datasource) {
      _datasource = widget.datasource ?? sl<NotificationInboxDatasource>();
      _refreshRequest = null;
      _unread = 0;
      unawaited(_refresh());
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _refreshTimer?.cancel();
    super.dispose();
  }

  Future<void> _refresh() async {
    if (!_appActive || !_visible || _refreshRequest != null) return;
    final request = Object();
    final datasource = _datasource;
    final role = widget.role;
    _refreshRequest = request;
    try {
      final count = await datasource.unreadCount(role);
      if (mounted && identical(request, _refreshRequest) && count != _unread) {
        setState(() => _unread = count);
      }
    } catch (_) {
      // The inbox itself exposes the load error and a Retry action.
    } finally {
      if (identical(request, _refreshRequest)) _refreshRequest = null;
    }
  }

  Future<void> _open() async {
    final notification = await Navigator.of(context).push<InboxNotification>(
      MaterialPageRoute(
        builder: (_) =>
            NotificationInboxPage(role: widget.role, datasource: _datasource),
      ),
    );
    if (!mounted) return;
    unawaited(_refresh());
    if (notification != null) widget.onOpenNotification?.call(notification);
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final push = sl.isRegistered<PushNotificationService>()
        ? sl<PushNotificationService>()
        : null;
    return IconButton(
      constraints: const BoxConstraints(minWidth: 48, minHeight: 48),
      padding: const EdgeInsets.all(14),
      onPressed: _open,
      tooltip: 'Notifications${_unread > 0 ? ', $_unread unread' : ''}',
      icon: Stack(
        clipBehavior: Clip.none,
        children: [
          const AppIcon(SwiftIcons.bell, size: 19),
          if (push != null)
            StreamBuilder<PushDeliveryStatus>(
              stream: push.statusChanges,
              initialData: push.status,
              builder: (context, snapshot) =>
                  snapshot.data?.needsAttention == true && _unread == 0
                  ? Positioned(
                      top: -3,
                      right: -3,
                      child: AppIcon(
                        SwiftIcons.circleAlert,
                        size: 13,
                        color: scheme.error,
                      ),
                    )
                  : const SizedBox.shrink(),
            ),
          if (_unread > 0)
            Positioned(
              top: -5,
              right: -7,
              child: Container(
                constraints: const BoxConstraints(minWidth: 17, minHeight: 17),
                padding: const EdgeInsets.symmetric(horizontal: 3),
                decoration: BoxDecoration(
                  color: scheme.error,
                  borderRadius: BorderRadius.circular(9),
                ),
                child: Center(
                  child: Text(
                    _unread > 9 ? '9+' : '$_unread',
                    style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: scheme.onError,
                      fontSize: 9,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class NotificationInboxPage extends StatefulWidget {
  const NotificationInboxPage({super.key, required this.role, this.datasource});

  final String role;
  final NotificationInboxDatasource? datasource;

  @override
  State<NotificationInboxPage> createState() => _NotificationInboxPageState();
}

class _NotificationInboxPageState extends State<NotificationInboxPage> {
  late final NotificationInboxDatasource _datasource;
  List<InboxNotification> _items = const [];
  String? _error;
  bool _loading = true;
  int? _openingId;
  Future<void>? _loadOperation;

  @override
  void initState() {
    super.initState();
    _datasource = widget.datasource ?? sl<NotificationInboxDatasource>();
    unawaited(_load());
  }

  Future<void> _load() =>
      _loadOperation ??= _fetch().whenComplete(() => _loadOperation = null);

  Future<void> _fetch() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final items = await _datasource.fetch(widget.role);
      if (!mounted) return;
      setState(() {
        _items = items;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = error.toString();
      });
    }
  }

  Future<void> _open(InboxNotification item) async {
    if (_openingId != null) return;
    setState(() {
      _openingId = item.id;
      _error = null;
    });
    try {
      if (!item.read) await _datasource.markRead(item.id, widget.role);
      if (!mounted) return;
      Navigator.of(context).pop(item);
    } catch (error) {
      if (!mounted) return;
      setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _openingId = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final push = sl.isRegistered<PushNotificationService>()
        ? sl<PushNotificationService>()
        : null;
    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: SwiftAppBar(
        toolbarHeight: SwiftAppBar.toolbarHeightFor(context),
        title: const Text('Notifications'),
      ),
      body: AppGlassBackground(
        child: SafeArea(
          top: false,
          child: RefreshIndicator(
            onRefresh: _load,
            child: CustomScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              slivers: [
                if (push != null)
                  SliverToBoxAdapter(
                    child: StreamBuilder<PushDeliveryStatus>(
                      stream: push.statusChanges,
                      initialData: push.status,
                      builder: (context, snapshot) => PushStatusBanner(
                        status: snapshot.data ?? push.status,
                        onRetry: () => push.startSession(),
                        onOpenSettings: Geolocator.openAppSettings,
                      ),
                    ),
                  ),
                if (_loading && _items.isEmpty)
                  SliverPadding(
                    padding: const EdgeInsets.all(16),
                    sliver: SliverList.builder(
                      itemCount: 4,
                      itemBuilder: (_, _) => const Padding(
                        padding: EdgeInsets.only(bottom: 12),
                        child: AppShimmer(
                          child: AppCard(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                ShimmerBlock(width: 150, height: 16),
                                SizedBox(height: 10),
                                ShimmerBlock(
                                  width: double.infinity,
                                  height: 12,
                                ),
                                SizedBox(height: 8),
                                ShimmerBlock(width: 100, height: 12),
                              ],
                            ),
                          ),
                        ),
                      ),
                    ),
                  )
                else if (_error != null && _items.isEmpty)
                  SliverFillRemaining(
                    hasScrollBody: false,
                    child: _InboxMessage(
                      title: 'Could not load notifications',
                      message: _error!,
                      onRetry: _load,
                    ),
                  )
                else ...[
                  if (_error != null)
                    SliverToBoxAdapter(
                      child: Padding(
                        padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
                        child: Text(
                          _error!,
                          style: TextStyle(color: theme.colorScheme.error),
                        ),
                      ),
                    ),
                  if (_items.isEmpty)
                    const SliverFillRemaining(
                      hasScrollBody: false,
                      child: _InboxMessage(
                        title: 'All caught up',
                        message: 'Ride updates will appear here.',
                      ),
                    )
                  else
                    SliverPadding(
                      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                      sliver: SliverList.builder(
                        itemCount: _items.length,
                        itemBuilder: (context, index) {
                          final item = _items[index];
                          return _NotificationTile(
                            key: ValueKey(item.id),
                            item: item,
                            opening: _openingId == item.id,
                            timeLabel: item.createdAt == null
                                ? null
                                : _timeLabel(item.createdAt!),
                            onTap: _openingId == null
                                ? () => _open(item)
                                : null,
                          );
                        },
                      ),
                    ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }

  String _timeLabel(DateTime time) {
    final local = time.toLocal();
    final hour = local.hour.toString().padLeft(2, '0');
    final minute = local.minute.toString().padLeft(2, '0');
    final now = DateTime.now();
    if (local.year == now.year &&
        local.month == now.month &&
        local.day == now.day) {
      return 'Today, $hour:$minute';
    }
    return '${local.day}/${local.month}/${local.year}, $hour:$minute';
  }
}

class _NotificationTile extends StatelessWidget {
  const _NotificationTile({
    super.key,
    required this.item,
    required this.opening,
    required this.timeLabel,
    required this.onTap,
  });

  final InboxNotification item;
  final bool opening;
  final String? timeLabel;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: AppCard(
        padding: EdgeInsets.zero,
        child: ListTile(
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 14,
            vertical: 8,
          ),
          leading: AppIcon(
            SwiftIcons.bell,
            size: 20,
            color: item.read
                ? theme.colorScheme.onSurfaceVariant
                : theme.colorScheme.primary,
          ),
          title: Text(
            item.title,
            style: theme.textTheme.titleSmall?.copyWith(
              fontWeight: item.read ? FontWeight.w500 : FontWeight.w600,
            ),
          ),
          subtitle: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 5),
              Text(item.body),
              if (timeLabel != null) ...[
                const SizedBox(height: 7),
                Text(timeLabel!, style: theme.textTheme.labelSmall),
              ],
            ],
          ),
          titleAlignment: ListTileTitleAlignment.top,
          trailing: opening
              ? const SizedBox.square(
                  dimension: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const AppIcon(SwiftIcons.chevronRight, size: 18),
          onTap: onTap,
        ),
      ),
    );
  }
}

/// Explains why system push alerts are unavailable while the server inbox
/// remains usable. The diagnostic is a bounded error code, never a token.
class PushStatusBanner extends StatelessWidget {
  const PushStatusBanner({
    super.key,
    required this.status,
    required this.onRetry,
    required this.onOpenSettings,
  });

  final PushDeliveryStatus status;
  final VoidCallback onRetry;
  final VoidCallback onOpenSettings;

  @override
  Widget build(BuildContext context) {
    if (status.state == PushDeliveryState.inactive ||
        status.state == PushDeliveryState.unavailable) {
      return const SizedBox.shrink();
    }
    final denied = status.state == PushDeliveryState.permissionDenied;
    final theme = Theme.of(context);
    if (!status.needsAttention) {
      final ready = status.state == PushDeliveryState.ready;
      return Padding(
        padding: const EdgeInsets.fromLTRB(18, 12, 18, 0),
        child: Row(
          children: [
            AppIcon(
              ready ? SwiftIcons.circleCheck : SwiftIcons.refreshCw,
              size: 18,
              color: theme.colorScheme.primary,
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                ready ? 'Push alerts connected' : 'Connecting push alerts…',
                style: theme.textTheme.labelMedium?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ),
          ],
        ),
      );
    }
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
      child: AppCard(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            AppIcon(
              SwiftIcons.bellOff,
              size: 18,
              color: theme.colorScheme.error,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    denied
                        ? 'Push alerts are off'
                        : 'Push alerts are not connected',
                    style: theme.textTheme.titleSmall,
                  ),
                  const SizedBox(height: 4),
                  Text(
                    denied
                        ? 'Enable notifications in your device settings to get ride and chat alerts. Updates remain available here.'
                        : 'Ride and chat updates remain available here. Retry push setup when connected.',
                  ),
                  if (!denied && status.code != null) ...[
                    const SizedBox(height: 4),
                    Text(
                      'Code: ${status.code}',
                      style: theme.textTheme.labelSmall,
                    ),
                  ],
                  const SizedBox(height: 8),
                  TextButton(
                    onPressed: denied ? onOpenSettings : onRetry,
                    child: Text(denied ? 'Open settings' : 'Retry'),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _InboxMessage extends StatelessWidget {
  const _InboxMessage({
    required this.title,
    required this.message,
    this.onRetry,
  });

  final String title;
  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) => Center(
    child: SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const AppIcon(SwiftIcons.bell, size: 24),
          const SizedBox(height: 12),
          Text(
            title,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: 7),
          Text(message, textAlign: TextAlign.center),
          if (onRetry != null) ...[
            const SizedBox(height: 12),
            TextButton(onPressed: onRetry, child: const Text('Retry')),
          ],
        ],
      ),
    ),
  );
}
