import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:geocoding/geocoding.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_state.dart';
import 'package:swift_app/features/rider/ride/data/model/ride_response_model.dart';
import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';
import 'package:swift_app/features/rider/ride/presentation/pages/ride_status_page.dart';

/// Uses ride-service history so status, fare and timestamps stay authoritative.
class RiderActivityPage extends StatefulWidget {
  const RiderActivityPage({super.key});
  @override
  State<RiderActivityPage> createState() => RiderActivityPageState();
}

class RiderActivityPageState extends State<RiderActivityPage> {
  List<RideResponse> _rides = [];
  int? _riderId;
  int _nextPage = 0;
  bool _last = false;
  bool _loading = false;
  bool _waitingForProfile = false;
  String? _error;
  Future<void> refresh() => _load(reset: true);
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _load(reset: true);
    });
  }

  Future<void> _load({bool reset = false}) async {
    if (_loading) return;
    final profileState = context.read<UserBloc>().state;
    final profile = profileState.user;
    if (profile == null) {
      setState(() {
        _waitingForProfile =
            profileState.status == UserStatus.initial ||
            profileState.status == UserStatus.loading;
        _error = _waitingForProfile
            ? null
            : 'Unable to load your profile. Please retry.';
      });
      context.read<UserBloc>().add(const FetchUserDetails());
      return;
    }
    setState(() {
      _loading = true;
      _waitingForProfile = false;
      _error = null;
    });
    final page = reset ? 0 : _nextPage;
    try {
      final response = await sl<DioClient>().dio.get(
        sl<RiderApiEndpoints>().getRideHistoryPaginated(profile.id),
        queryParameters: {'page': page, 'size': 20},
      );
      final body = response.data;
      if (response.statusCode != 200 ||
          body is! Map ||
          body['success'] == false ||
          body['data'] is! Map) {
        throw StateError(
          body is Map
              ? body['message']?.toString() ?? 'Unable to load trips.'
              : 'Unable to load trips.',
        );
      }
      final data = body['data'] as Map;
      final items = (data['content'] as List)
          .map(
            (e) =>
                RideResponseModel.fromJson(Map<String, dynamic>.from(e as Map)),
          )
          .toList();
      if (mounted) {
        setState(() {
          _riderId = profile.id;
          _rides = reset ? items : [..._rides, ...items];
          _nextPage = page + 1;
          _last = data['last'] == true;
        });
      }
    } catch (error) {
      if (mounted) {
        setState(
          () => _error = error.toString().replaceFirst('Bad state: ', ''),
        );
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) => BlocListener<UserBloc, UserState>(
    listenWhen: (previous, current) =>
        previous.user?.id != current.user?.id ||
        current.user == null && previous.status != current.status,
    listener: (_, state) {
      if (state.user != null) _load(reset: true);
      if (state.user == null && state.status == UserStatus.failure) {
        setState(() {
          _waitingForProfile = false;
          _error = 'Unable to load your profile. Please retry.';
        });
      }
    },
    child: SafeArea(
      bottom: false,
      child: RefreshIndicator(
        onRefresh: () => _load(reset: true),
        child: ColoredBox(
          color: Theme.of(context).scaffoldBackgroundColor,
          child: CustomScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            slivers: [
              const SliverToBoxAdapter(child: SizedBox(height: 12)),
              if (_rides.isEmpty && (_loading || _waitingForProfile))
                const SliverToBoxAdapter(
                  child: Padding(
                    padding: EdgeInsets.all(16),
                    child: AppShimmer(
                      child: Column(
                        children: [
                          ShimmerBlock(
                            width: double.infinity,
                            height: 140,
                            radius: 18,
                          ),
                          SizedBox(height: 14),
                          ShimmerBlock(
                            width: double.infinity,
                            height: 140,
                            radius: 18,
                          ),
                          SizedBox(height: 14),
                          ShimmerBlock(
                            width: double.infinity,
                            height: 140,
                            radius: 18,
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              if (_error != null)
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      children: [
                        Text(_error!, textAlign: TextAlign.center),
                        TextButton(
                          onPressed: () => _load(reset: _rides.isEmpty),
                          child: const Text('Try again'),
                        ),
                      ],
                    ),
                  ),
                ),
              if (_rides.isEmpty &&
                  !_loading &&
                  !_waitingForProfile &&
                  _error == null)
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Container(
                      padding: const EdgeInsets.all(28),
                      decoration: BoxDecoration(
                        color: Theme.of(context).colorScheme.surface,
                        borderRadius: BorderRadius.circular(18),
                        border: Border.all(
                          color: Theme.of(context).colorScheme.outlineVariant,
                        ),
                      ),
                      child: Column(
                        children: [
                          AppIcon(
                            SwiftIcons.route,
                            size: 22,
                            color: Theme.of(
                              context,
                            ).colorScheme.onSurfaceVariant,
                          ),
                          const SizedBox(height: 12),
                          const Text(
                            'Your first journey starts here',
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 6),
                          const Text(
                            'Booked rides will appear in Activity.',
                            textAlign: TextAlign.center,
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              SliverList.builder(
                itemCount: _rides.length,
                itemBuilder: (context, index) => Padding(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 14),
                  child: _JourneyCard(
                    ride: _rides[index],
                    onTap: () async {
                      await Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => RideStatusPage(
                            ride: _rides[index],
                            riderId: _riderId!,
                          ),
                        ),
                      );
                      if (mounted) _load(reset: true);
                    },
                  ),
                ),
              ),
              if (_rides.isNotEmpty && !_last)
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: OutlinedButton(
                      onPressed: _loading ? null : () => _load(),
                      child: Text(_loading ? 'Loading…' : 'Load more trips'),
                    ),
                  ),
                ),
              SliverToBoxAdapter(
                child: SizedBox(
                  height: 28 + MediaQuery.paddingOf(context).bottom,
                ),
              ),
            ],
          ),
        ),
      ),
    ),
  );
}

class _JourneyCard extends StatefulWidget {
  final RideResponse ride;
  final VoidCallback onTap;
  const _JourneyCard({required this.ride, required this.onTap});
  @override
  State<_JourneyCard> createState() => _JourneyCardState();
}

class _JourneyCardState extends State<_JourneyCard> {
  static final Map<String, String> _places = {};
  late final _geocoding = Geocoding(locale: const Locale('en', 'IN'));
  String? _pickup;
  String? _drop;
  @override
  void initState() {
    super.initState();
    _resolve();
  }

  @override
  void didUpdateWidget(covariant _JourneyCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.ride.id != widget.ride.id) _resolve();
  }

  Future<String> _name(double? lat, double? lng, String label) async {
    if (lat == null || lng == null) return '$label unavailable';
    final key = '$lat,$lng';
    if (_places.containsKey(key)) return _places[key]!;
    var value = '${lat.toStringAsFixed(4)}, ${lng.toStringAsFixed(4)}';
    try {
      final places = await _geocoding
          .placemarkFromCoordinates(lat, lng)
          .timeout(const Duration(seconds: 6));
      if (places.isNotEmpty) {
        final place = places.first;
        final parts = [
          place.name,
          place.subLocality,
          place.locality,
        ].whereType<String>().where((e) => e.trim().isNotEmpty).toSet();
        if (parts.isNotEmpty) value = parts.join(', ');
      }
    } catch (_) {}
    if (_places.length >= 120) _places.remove(_places.keys.first);
    _places[key] = value;
    return value;
  }

  Future<void> _resolve() async {
    final ride = widget.ride;
    final places = await Future.wait([
      _name(ride.pickupLatitude, ride.pickupLongitude, 'Pickup'),
      _name(ride.destinationLatitude, ride.destinationLongitude, 'Destination'),
    ]);
    if (mounted && widget.ride.id == ride.id) {
      setState(() {
        _pickup = places[0];
        _drop = places[1];
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final ride = widget.ride;
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final status = (ride.currentStatus ?? 'REQUESTED')
        .replaceAll('_', ' ')
        .toLowerCase();
    final statusLabel = status.isEmpty
        ? 'Requested'
        : '${status[0].toUpperCase()}${status.substring(1)}';
    final date = DateTime.tryParse(ride.requestedAt ?? ride.createdAt ?? '');
    final fare = ride.fareDetail?.totalFare;
    final distance = ride.actualDistance ?? ride.estimatedDistance;
    return Material(
      color: Colors.transparent,
      borderRadius: BorderRadius.circular(18),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: widget.onTap,
        borderRadius: BorderRadius.circular(18),
        child: Container(
          padding: const EdgeInsets.fromLTRB(16, 15, 16, 14),
          decoration: BoxDecoration(
            color: scheme.surface,
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: scheme.outlineVariant, width: 0.8),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    width: 36,
                    height: 36,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: scheme.surfaceContainerLow,
                    ),
                    child: AppIcon(
                      SwiftIcons.route,
                      color: scheme.onSurfaceVariant,
                      size: 18,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          date == null
                              ? 'Ride #${ride.id}'
                              : _dateLabel(date.toLocal()),
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: theme.textTheme.labelMedium?.copyWith(
                            fontWeight: FontWeight.w500,
                            color: scheme.onSurface,
                          ),
                        ),
                        const SizedBox(height: 3),
                        Text(
                          statusLabel,
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: scheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 8),
                  AppIcon(
                    SwiftIcons.chevronRight,
                    size: 17,
                    color: scheme.onSurfaceVariant,
                  ),
                ],
              ),
              const SizedBox(height: 17),
              _placeLine('PICKUP', _pickup, theme, pickup: true),
              const SizedBox(height: 15),
              _placeLine('DROP-OFF', _drop, theme),
              const SizedBox(height: 15),
              Divider(
                height: 1,
                color: scheme.outlineVariant.withValues(alpha: 0.6),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  AppIcon(
                    SwiftIcons.car,
                    size: 17,
                    color: scheme.onSurfaceVariant,
                  ),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Text(
                      distance == null
                          ? 'Trip details'
                          : '${distance.toStringAsFixed(1)} km',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: scheme.onSurfaceVariant,
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      fare == null
                          ? 'Fare unavailable'
                          : '${ride.fareDetail?.currency == 'INR' ? '₹' : '${ride.fareDetail?.currency ?? ''} '}${fare.toStringAsFixed(0)}',
                      textAlign: TextAlign.end,
                      style: theme.textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: scheme.onSurface,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _routeDot(Color color, {required bool filled}) => Container(
    width: 12,
    height: 12,
    decoration: BoxDecoration(
      shape: BoxShape.circle,
      color: filled ? color : Colors.transparent,
      border: Border.all(color: color, width: 2),
    ),
  );
  Widget _placeLine(
    String heading,
    String? value,
    ThemeData theme, {
    bool pickup = false,
  }) {
    final scheme = theme.colorScheme;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 3),
          child: _routeDot(scheme.onSurfaceVariant, filled: pickup),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                heading,
                style: theme.textTheme.labelSmall?.copyWith(
                  fontSize: 10,
                  letterSpacing: 0.6,
                  fontWeight: FontWeight.w500,
                  color: scheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 3),
              value == null
                  ? const AppShimmer(
                      child: ShimmerBlock(width: 170, height: 14),
                    )
                  : Text(
                      value,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodyMedium?.copyWith(height: 1.3),
                    ),
            ],
          ),
        ),
      ],
    );
  }

  String _dateLabel(DateTime date) {
    const months = [
      'Jan',
      'Feb',
      'Mar',
      'Apr',
      'May',
      'Jun',
      'Jul',
      'Aug',
      'Sep',
      'Oct',
      'Nov',
      'Dec',
    ];
    return '${date.day} ${months[date.month - 1]} ${date.year} · ${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';
  }
}
