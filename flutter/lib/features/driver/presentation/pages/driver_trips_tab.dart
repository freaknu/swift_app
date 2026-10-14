import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:flutter/material.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/driver/data/model/driver_journey_model.dart';
import 'package:swift_app/features/driver/data/model/driver_rating_model.dart';
import 'package:swift_app/features/driver/data/model/driver_statistics_model.dart';

/// Driver-owned history and totals. All values come from the driver and ride
/// services; a fresh account displays an empty state instead of sample trips.
class DriverTripsTab extends StatefulWidget {
  final DriverRemoteDatasource datasource;
  final int? driverId;
  final DriverStatisticsModel? statistics;
  final Future<void> Function() onRefreshStatistics;

  const DriverTripsTab({
    super.key,
    required this.datasource,
    required this.driverId,
    required this.statistics,
    required this.onRefreshStatistics,
  });

  @override
  State<DriverTripsTab> createState() => _DriverTripsTabState();
}

class _DriverTripsTabState extends State<DriverTripsTab> {
  List<DriverJourney> _journeys = const [];
  DriverJourneyPage? _page;
  DriverRatingModel? _rating;
  bool _loading = false;
  bool _loadingMore = false;
  bool _loadingRating = false;
  String? _error;
  String? _ratingError;
  String _filter = 'ALL';
  int _requestGeneration = 0;

  @override
  void initState() {
    super.initState();
    if (widget.driverId != null) {
      _scheduleProfileRefresh();
    }
  }

  @override
  void didUpdateWidget(covariant DriverTripsTab oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.driverId != widget.driverId) {
      _requestGeneration++;
      _journeys = const [];
      _page = null;
      _rating = null;
      _loading = false;
      _loadingMore = false;
      _loadingRating = false;
      _error = null;
      _ratingError = null;
      _filter = 'ALL';
      if (widget.driverId != null) _scheduleProfileRefresh();
    }
  }

  void _scheduleProfileRefresh() {
    final generation = _requestGeneration;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted ||
          widget.driverId == null ||
          generation != _requestGeneration) {
        return;
      }
      _refresh();
    });
  }

  Future<void> _refresh() async {
    final driverId = widget.driverId;
    if (driverId == null || _loading) return;
    final generation = ++_requestGeneration;
    setState(() {
      _loading = true;
      _loadingMore = false;
      _loadingRating = true;
      _error = null;
      _ratingError = null;
    });
    final ridesFuture = widget.datasource.getJourneys(
      status: _filter == 'ALL' ? null : _filter,
    );
    final ratingFuture = widget.datasource.getRating(driverId);
    final statsFuture = widget.onRefreshStatistics();
    final rides = await ridesFuture;
    final rating = await ratingFuture;
    await statsFuture;
    if (!mounted ||
        widget.driverId != driverId ||
        generation != _requestGeneration) {
      return;
    }
    setState(() {
      _loading = false;
      _loadingRating = false;
      if (rides.success && rides.data != null) {
        _page = rides.data;
        _journeys = rides.data!.content;
      } else {
        _error = rides.message;
      }
      if (rating.success && rating.data != null) {
        _rating = rating.data;
      } else if (rating.message.toLowerCase().contains('not found')) {
        _rating = const DriverRatingModel(averageRating: 0, entries: []);
      } else {
        _ratingError = rating.message;
      }
    });
  }

  Future<void> _loadMore() async {
    final page = _page;
    if (_loadingMore || page == null || page.last) return;
    final generation = _requestGeneration;
    final driverId = widget.driverId;
    setState(() => _loadingMore = true);
    final response = await widget.datasource.getJourneys(
      page: page.page + 1,
      status: _filter == 'ALL' ? null : _filter,
    );
    if (!mounted ||
        widget.driverId != driverId ||
        generation != _requestGeneration) {
      return;
    }
    setState(() {
      _loadingMore = false;
      if (response.success && response.data != null) {
        _page = response.data;
        final seen = _journeys.map((ride) => ride.id).toSet();
        _journeys = [
          ..._journeys,
          ...response.data!.content.where((ride) => seen.add(ride.id)),
        ];
      } else {
        _error = response.message;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final hasHistory =
        widget.driverId != null &&
        !(_loading && _page == null) &&
        !(_error != null && _page == null) &&
        _journeys.isNotEmpty;
    final header = <Widget>[
      SwiftAppBar(
        toolbarHeight: SwiftAppBar.toolbarHeightFor(context, hasSubtitle: true),
        title: Text('Trips'),
        subtitle: 'Your ride history and lifetime totals',
        primary: false,
        titleSpacing: 0,
        automaticallyImplyLeading: false,
      ),
      const SizedBox(height: 20),
      Container(
        padding: const EdgeInsets.all(4),
        decoration: BoxDecoration(
          color: scheme.onSurface.withValues(alpha: 0.05),
          borderRadius: BorderRadius.circular(16),
        ),
        child: Row(
          children: [
            for (final option in const ['ALL', 'COMPLETED', 'CANCELLED'])
              Expanded(
                child: InkWell(
                  onTap: _loading
                      ? null
                      : () {
                          if (_filter == option) return;
                          setState(() {
                            _filter = option;
                            _journeys = const [];
                            _page = null;
                            _error = null;
                          });
                          _refresh();
                        },
                  borderRadius: BorderRadius.circular(13),
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 6,
                      vertical: 10,
                    ),
                    constraints: const BoxConstraints(minHeight: 44),
                    decoration: BoxDecoration(
                      color: _filter == option
                          ? scheme.primary
                          : Colors.transparent,
                      borderRadius: BorderRadius.circular(13),
                    ),
                    alignment: Alignment.center,
                    child: Text(
                      option == 'ALL'
                          ? 'All'
                          : option == 'COMPLETED'
                          ? 'Completed'
                          : 'Cancelled',
                      textAlign: TextAlign.center,
                      style: theme.textTheme.labelMedium?.copyWith(
                        color: _filter == option
                            ? scheme.onPrimary
                            : scheme.onSurfaceVariant,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ),
              ),
          ],
        ),
      ),
      const SizedBox(height: 20),
      _TotalsCard(statistics: widget.statistics),
      const SizedBox(height: 24),
      _SectionTitle(
        title: 'Ride history',
        trailing: _page == null ? null : '${_page!.totalElements} rides',
      ),
      const SizedBox(height: 12),
      if (widget.driverId == null || _loading && _page == null)
        const _TripsSkeleton()
      else if (_error != null && _page == null)
        _InlineState(
          icon: SwiftIcons.wifiOff,
          title: 'Could not load rides',
          message: _error!,
          onRetry: _refresh,
        )
      else if (_journeys.isEmpty)
        const _InlineState(
          icon: SwiftIcons.route,
          title: 'No rides yet',
          message: 'Your ride history will appear here after your first trip.',
        ),
    ];
    final footer = <Widget>[
      if (hasHistory && _page?.last == false)
        OutlinedButton(
          onPressed: _loadingMore ? null : _loadMore,
          child: Text(_loadingMore ? 'Loading more…' : 'Load more rides'),
        ),
      if (hasHistory && _error != null)
        TextButton(onPressed: _loadMore, child: Text('Retry: $_error')),
      const SizedBox(height: 24),
      const _SectionTitle(title: 'Rider ratings'),
      const SizedBox(height: 12),
      if (_loadingRating)
        const _TripsSkeleton()
      else if (_ratingError != null)
        _InlineState(
          icon: SwiftIcons.star,
          title: 'Ratings unavailable',
          message: _ratingError!,
          onRetry: _refresh,
        )
      else if (_rating == null || _rating!.entries.isEmpty)
        const _InlineState(
          icon: SwiftIcons.star,
          title: 'No ratings yet',
          message: 'Ratings from completed trips will appear here.',
        )
      else
        _RatingsCard(rating: _rating!),
    ];
    final historyCount = hasHistory ? _journeys.length : 0;
    return RefreshIndicator.adaptive(
      onRefresh: _refresh,
      child: ListView.builder(
        key: const PageStorageKey('driver-trip-history'),
        keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
        physics: const AlwaysScrollableScrollPhysics(),
        padding: EdgeInsets.fromLTRB(
          16,
          0,
          16,
          28 + MediaQuery.paddingOf(context).bottom,
        ),
        itemCount: header.length + historyCount + footer.length,
        itemBuilder: (context, index) {
          if (index < header.length) return header[index];
          final historyIndex = index - header.length;
          if (historyIndex < historyCount) {
            final ride = _journeys[historyIndex];
            return Padding(
              key: ValueKey('driver-trip-${ride.id}'),
              padding: const EdgeInsets.only(bottom: 10),
              child: _JourneyCard(ride: ride),
            );
          }
          return footer[historyIndex - historyCount];
        },
      ),
    );
  }
}

class _TotalsCard extends StatelessWidget {
  final DriverStatisticsModel? statistics;
  const _TotalsCard({required this.statistics});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final stats = statistics;
    return AppCard(
      padding: const EdgeInsets.all(18),
      child: stats == null
          ? const AppShimmer(
              child: Column(
                children: [
                  ShimmerBlock(width: double.infinity, height: 48),
                  SizedBox(height: 12),
                  ShimmerBlock(width: double.infinity, height: 48),
                ],
              ),
            )
          : Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Lifetime earnings', style: theme.textTheme.bodyMedium),
                const SizedBox(height: 6),
                Text(
                  '₹${stats.lifetimeEarning.toStringAsFixed(0)}',
                  style: theme.textTheme.headlineMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 18),
                Row(
                  children: [
                    Expanded(
                      child: _TotalMetric(
                        value: '${stats.completedTrips}',
                        label: 'Completed',
                      ),
                    ),
                    Expanded(
                      child: _TotalMetric(
                        value: '${stats.cancelledTrips}',
                        label: 'Cancelled',
                      ),
                    ),
                    Expanded(
                      child: _TotalMetric(
                        value:
                            '${stats.lifetimeDistanceKm.toStringAsFixed(1)} km',
                        label: 'Distance',
                      ),
                    ),
                  ],
                ),
              ],
            ),
    );
  }
}

class _TotalMetric extends StatelessWidget {
  final String value;
  final String label;
  const _TotalMetric({required this.value, required this.label});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          value,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        Text(label, style: theme.textTheme.bodySmall),
      ],
    );
  }
}

class _JourneyCard extends StatelessWidget {
  final DriverJourney ride;
  const _JourneyCard({required this.ride});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final date = ride.completedAt ?? ride.cancelledAt ?? ride.requestedAt;
    final currency = ride.currency == 'INR' || ride.currency == null
        ? '₹'
        : '${ride.currency} ';
    final distance = ride.actualDistanceKm ?? ride.estimatedDistanceKm;
    return AppCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 42,
                height: 42,
                decoration: BoxDecoration(
                  color: scheme.primary.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: AppIcon(
                  SwiftIcons.route,
                  color: scheme.primary,
                  size: 19,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Ride #${ride.id}',
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    Text(
                      date == null ? 'Date unavailable' : _formatDate(date),
                      style: theme.textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
              if (ride.fare != null)
                Flexible(
                  child: Text(
                    '$currency${ride.fare!.toStringAsFixed(0)}',
                    textAlign: TextAlign.end,
                    style: theme.textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 6,
            children: [
              _Chip(
                label: _statusLabel(ride.status),
                icon: _statusIcon(ride.status),
              ),
              if (ride.rideTypeCode?.isNotEmpty == true)
                _Chip(label: ride.rideTypeCode!, icon: SwiftIcons.car),
              if (distance != null)
                _Chip(
                  label: '${distance.toStringAsFixed(1)} km',
                  icon: SwiftIcons.ruler,
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _RatingsCard extends StatelessWidget {
  final DriverRatingModel rating;
  const _RatingsCard({required this.rating});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              AppIcon(SwiftIcons.star, color: scheme.primary, size: 19),
              const SizedBox(width: 6),
              Text(
                rating.averageRating.toStringAsFixed(1),
                style: theme.textTheme.titleLarge?.copyWith(
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  'from ${rating.entries.length} ratings',
                  style: theme.textTheme.bodySmall,
                ),
              ),
            ],
          ),
          for (final entry in rating.entries.take(5)) ...[
            const Divider(height: 22),
            Row(
              children: [
                Expanded(
                  child: Text(
                    entry.rideId == null
                        ? 'Trip rating'
                        : 'Ride #${entry.rideId}',
                  ),
                ),
                AppIcon(SwiftIcons.star, size: 18, color: scheme.primary),
                Text(
                  entry.score.toStringAsFixed(1),
                  style: theme.textTheme.labelLarge,
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

class _Chip extends StatelessWidget {
  final String label;
  final SwiftIcons icon;
  const _Chip({required this.label, required this.icon});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 6),
      decoration: BoxDecoration(
        color: scheme.primary.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          AppIcon(icon, size: 14, color: scheme.primary),
          const SizedBox(width: 4),
          Flexible(
            child: Text(label, style: Theme.of(context).textTheme.labelSmall),
          ),
        ],
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;
  final String? trailing;
  const _SectionTitle({required this.title, this.trailing});

  @override
  Widget build(BuildContext context) => Row(
    children: [
      Expanded(
        child: Text(
          title,
          style: Theme.of(
            context,
          ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w600),
        ),
      ),
      if (trailing != null)
        Text(trailing!, style: Theme.of(context).textTheme.bodySmall),
    ],
  );
}

class _TripsSkeleton extends StatelessWidget {
  const _TripsSkeleton();
  @override
  Widget build(BuildContext context) => const AppShimmer(
    child: AppCard(
      child: Column(
        children: [
          ShimmerBlock(width: double.infinity, height: 20),
          SizedBox(height: 12),
          ShimmerBlock(width: double.infinity, height: 14),
          SizedBox(height: 12),
          ShimmerBlock(width: 150, height: 14),
        ],
      ),
    ),
  );
}

class _InlineState extends StatelessWidget {
  final SwiftIcons icon;
  final String title;
  final String message;
  final VoidCallback? onRetry;
  const _InlineState({
    required this.icon,
    required this.title,
    required this.message,
    this.onRetry,
  });

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      children: [
        AppIcon(icon, size: 26, color: Theme.of(context).colorScheme.primary),
        const SizedBox(height: 8),
        Text(title, style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 4),
        Text(
          message,
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.bodySmall,
        ),
        if (onRetry != null)
          TextButton(onPressed: onRetry, child: const Text('Retry')),
      ],
    ),
  );
}

String _formatDate(DateTime date) {
  final local = date.toLocal();
  final day = local.day.toString().padLeft(2, '0');
  final month = local.month.toString().padLeft(2, '0');
  return '$day/$month/${local.year}';
}

String _statusLabel(String status) => switch (status) {
  'COMPLETED' => 'Completed',
  'CANCELLED' => 'Cancelled',
  'MATCH_FAILED' => 'No driver found',
  _ => status.replaceAll('_', ' ').toLowerCase(),
};

SwiftIcons _statusIcon(String status) => switch (status) {
  'COMPLETED' => SwiftIcons.circleCheck,
  'CANCELLED' => SwiftIcons.circleX,
  _ => SwiftIcons.info,
};
