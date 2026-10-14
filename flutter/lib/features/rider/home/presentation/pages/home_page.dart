import 'dart:async';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/common/shared/widgets/auth_frame.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/vehicle_photo.dart';

import 'package:swift_app/constants/enums/address_type_enum.dart';

import 'package:swift_app/core/di/injection_container.dart';

import 'package:swift_app/core/notifications/notification_inbox_datasource.dart';

import 'package:swift_app/core/notifications/notification_inbox_page.dart';

import 'package:swift_app/core/notifications/push_notification_service.dart';

import 'package:swift_app/features/common/chat/data/datasource/chat_remote_datasource.dart';

import 'package:swift_app/features/common/chat/data/datasource/chat_realtime_service.dart';

import 'package:swift_app/features/common/chat/domain/chat_availability.dart';

import 'package:swift_app/features/common/chat/presentation/pages/chat_page.dart';

import 'package:swift_app/features/common/fare/presentation/blocs/fare_bloc.dart';

import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';

import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';

import 'package:swift_app/features/rider/home/presentation/widgets/add_address_sheet_widget.dart';

import 'package:swift_app/features/rider/home/domain/entity/ride_type.dart';

import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_event.dart';

import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_state.dart';

import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';

import 'package:swift_app/features/rider/home/presentation/blocs/offer_bloc.dart';

import 'package:swift_app/features/rider/home/presentation/blocs/ride_history_bloc.dart';

import 'package:swift_app/features/rider/home/presentation/blocs/ride_history_event.dart';

import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_bloc.dart';

import 'package:swift_app/features/rider/home/presentation/blocs/user_bloc.dart';

import 'package:swift_app/features/rider/home/presentation/blocs/user_event.dart';

import 'package:swift_app/features/rider/home/presentation/blocs/user_state.dart';

import 'package:swift_app/features/rider/home/presentation/models/location_selection.dart';

import 'package:swift_app/features/rider/home/presentation/pages/trip_location_page.dart';

import 'package:swift_app/features/rider/home/presentation/pages/trip_route_map_page.dart';

import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';

import 'package:swift_app/features/rider/ride/data/repository/ride_repository_impl.dart';

import 'package:swift_app/features/rider/ride/domain/usecases/ride_lifecycle_usecase.dart';

import 'package:swift_app/features/rider/ride/presentation/blocs/ride_bloc.dart';

import 'package:swift_app/features/rider/ride/presentation/pages/ride_status_page.dart';

import 'package:swift_app/features/rider/home/presentation/widgets/google_map_widget.dart';

import 'package:swift_app/themes/app_radii.dart';

import 'package:flutter/cupertino.dart';

import 'package:flutter/material.dart';

import 'package:flutter/services.dart';

import 'package:flutter_bloc/flutter_bloc.dart';

import 'package:geolocator/geolocator.dart';

/// Shares the rider blocs with a page pushed onto the root navigator.

///

/// The rider blocs are installed by \`MainPageWithProviders\` above \`MainPage\`,

/// but a route pushed with \`Navigator.push\` lands in the root overlay as a

/// sibling of that subtree rather than a descendant, so pages such as

/// \`TripRouteMapPage\` cannot resolve the blocs without this wrapper.

/// The existing instances are shared (never recreated) so booking state

/// stays consistent across the whole flow.

Widget _withRiderBlocs(BuildContext context, Widget page) {
  return MultiBlocProvider(
    providers: [
      BlocProvider<UserBloc>.value(value: context.read<UserBloc>()),

      BlocProvider<RideTypeBloc>.value(value: context.read<RideTypeBloc>()),

      BlocProvider<RideHistoryBloc>.value(
        value: context.read<RideHistoryBloc>(),
      ),

      BlocProvider<OfferBloc>.value(value: context.read<OfferBloc>()),

      BlocProvider<FareBloc>.value(value: context.read<FareBloc>()),

      BlocProvider<RideBloc>.value(value: context.read<RideBloc>()),
    ],

    child: page,
  );
}

class HomePage extends StatefulWidget {
  final VoidCallback? onAccountTap;
  final VoidCallback? onInitialMapSettled;
  final VoidCallback? onInitialRideCheckSettled;

  const HomePage({
    super.key,
    this.onAccountTap,
    this.onInitialMapSettled,
    this.onInitialRideCheckSettled,
  });

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> with WidgetsBindingObserver {
  Position? _pickup;

  String? _pickupName;

  Position? _destination;

  String? _destinationName;

  bool _homeMapPaused = false;

  RideResponse? _activeRide;

  bool _checkingActiveRide = false;
  bool _reportedInitialRideCheck = false;

  // The map remains interactive while the content below scrolls over it.
  final ScrollController _homeScrollController = ScrollController();
  final ValueNotifier<double> _homeScrollOffset = ValueNotifier<double>(0);

  void _onHomeScroll() {
    if (_homeScrollController.hasClients) {
      _homeScrollOffset.value = _homeScrollController.offset.clamp(
        0.0,
        double.infinity,
      );
    }
  }

  @override
  void initState() {
    super.initState();

    WidgetsBinding.instance.addObserver(this);
    _homeScrollController.addListener(_onHomeScroll);

    WidgetsBinding.instance.addPostFrameCallback((_) => _checkActiveRide());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _homeScrollController.removeListener(_onHomeScroll);
    _homeScrollController.dispose();
    _homeScrollOffset.dispose();

    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state != AppLifecycleState.resumed) return;

    final userBloc = context.read<UserBloc>();

    if (userBloc.state.user == null) {
      userBloc.add(const FetchUserDetails());
    } else {
      unawaited(_checkActiveRide(userBloc.state.user));
    }

    context.read<RideHistoryBloc>().add(const FetchRideHistory());
  }

  Future<void> _chooseDestination({String? preferredRideTypeCode}) async {
    setState(() {
      _homeMapPaused = true;
    });

    try {
      final TripLocations?
      trip = await Navigator.of(context).push<TripLocations>(
        CupertinoPageRoute(
          builder: (_) => TripLocationPage(
            initialPickup: _selectionFrom(_pickup, _pickupName),

            initialDestination: _selectionFrom(_destination, _destinationName),

            savedAddresses:
                context.read<UserBloc>().state.user?.allAddresses ?? const [],
          ),
        ),
      );

      if (!mounted || trip == null) return;

      _applyTrip(trip);

      await _closeTextInput();

      if (!mounted) return;

      await Navigator.of(context).push<void>(
        CupertinoPageRoute(
          builder: (_) => _withRiderBlocs(
            context,

            TripRouteMapPage(
              trip: trip,

              preferredRideTypeCode: preferredRideTypeCode,
            ),
          ),
        ),
      );

      if (!mounted) return;

      await _checkActiveRide();

      if (mounted) {
        context.read<RideHistoryBloc>().add(const FetchRideHistory());
      }
    } finally {
      if (mounted) {
        setState(() {
          _homeMapPaused = false;
        });
      }
    }
  }

  Future<void> _checkActiveRide([UserReponse? providedUser]) async {
    if (_checkingActiveRide || !mounted) return;

    final user = providedUser ?? context.read<UserBloc>().state.user;

    if (user == null) return;

    _checkingActiveRide = true;

    try {
      final ride = await sl<RideLifecycleUsecase>().fetchActiveRide(user.id);

      if (mounted) setState(() => _activeRide = ride);
    } catch (error) {
      // A 404 means there is no active ride. Keep the last known ride visible

      // through a transient gateway/network failure so the rider can still

      // recover the journey from this screen.

      if (error is RideRepositoryException && error.statusCode == 404) {
        if (mounted) setState(() => _activeRide = null);
      }
    } finally {
      _checkingActiveRide = false;
      if (mounted && !_reportedInitialRideCheck) {
        _reportedInitialRideCheck = true;
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted) widget.onInitialRideCheckSettled?.call();
        });
        // Reduced motion can leave startup completely still. A failed best
        // effort request must schedule the frame that delivers this callback.
        WidgetsBinding.instance.ensureVisualUpdate();
      }
    }
  }

  Future<void> _openActiveRide() async {
    final ride = _activeRide;

    final user = context.read<UserBloc>().state.user;

    if (ride == null || user == null) return;

    final rideHistoryBloc = context.read<RideHistoryBloc>();

    await Navigator.of(context).push(
      CupertinoPageRoute(
        builder: (_) => RideStatusPage(ride: ride, riderId: user.id),
      ),
    );

    if (mounted) {
      await _checkActiveRide(user);

      rideHistoryBloc.add(const FetchRideHistory());
    }
  }

  Future<void> _openNotification(InboxNotification item) async {
    final rideId = item.rideId;

    final user = context.read<UserBloc>().state.user;

    if (rideId == null || user == null) return;

    try {
      final ride = await sl<RideLifecycleUsecase>().fetchRide(rideId);

      if (!mounted) return;

      if (item.type == 'CHAT_MESSAGE' &&
          ride.driverId != null &&
          ChatAvailability.isAvailable(ride.currentStatus)) {
        await Navigator.of(context).push(
          CupertinoPageRoute(
            builder: (_) => ChatPage(
              rideId: rideId,

              currentUserId: user.id,

              role: 'RIDER',

              rideStatus: ride.currentStatus,

              riderId: user.id,

              driverId: ride.driverId,

              datasource: sl<ChatRemoteDatasource>(),

              realtime: sl<ChatRealtimeService>(),
            ),
          ),
        );
      } else {
        await Navigator.of(context).push(
          CupertinoPageRoute(
            builder: (_) => RideStatusPage(ride: ride, riderId: user.id),
          ),
        );
      }

      if (mounted) {
        unawaited(_checkActiveRide(user));

        context.read<RideHistoryBloc>().add(const FetchRideHistory());
      }
    } catch (_) {
      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Could not open this ride. Please retry.'),
        ),
      );
    }
  }

  Future<void> _closeTextInput() async {
    FocusManager.instance.primaryFocus?.unfocus(
      disposition: UnfocusDisposition.scope,
    );

    await SystemChannels.textInput.invokeMethod<void>('TextInput.hide');

    try {
      await SystemChannels.textInput.invokeMethod<void>(
        'TextInput.clearClient',
      );
    } catch (_) {
      // Older Flutter engines may not expose clearClient.
    }
  }

  void _applyTrip(TripLocations trip) {
    setState(() {
      _pickup = trip.pickup.position;

      _pickupName = trip.pickup.label ?? 'Pickup';

      _destination = trip.destination.position;

      _destinationName = trip.destination.label ?? 'Destination';
    });
  }

  LocationSelection? _selectionFrom(Position? position, String? label) {
    if (position == null) {
      return null;
    }

    return LocationSelection(position: position, label: label);
  }

  void _selectSavedAddress(UserAddress address) {
    final selection = LocationSelection.fromCoordinates(
      latitude: address.latitude,

      longitude: address.longitude,

      label: address.label,
    );

    setState(() {
      _destinationName = address.label;

      _destination = selection.position;
    });

    unawaited(_chooseDestination());
  }

  void _onMapLocationChanged(Position position) {
    if (!mounted ||
        (_pickupName != null && _pickupName != 'Current location')) {
      return;
    }

    _pickup = position;

    if (_pickupName == null) {
      setState(() => _pickupName = 'Current location');
    }
  }

  void _openAddAddressSheet() {
    final userBloc = context.read<UserBloc>();

    showModalBottomSheet<void>(
      context: context,

      isScrollControlled: true,

      backgroundColor: Colors.transparent,

      useSafeArea: true,

      builder: (_) => BlocProvider<UserBloc>.value(
        value: userBloc,

        child: const AddAddressSheet(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    final mapHeight = (MediaQuery.sizeOf(context).height * 0.43).clamp(
      320.0,

      440.0,
    );

    return BlocListener<UserBloc, UserState>(
      listenWhen: (previous, current) =>
          previous.user == null && current.user != null,

      listener: (_, state) {
        unawaited(_checkActiveRide(state.user));

        if (sl.isRegistered<PushNotificationService>()) {
          unawaited(sl<PushNotificationService>().syncAfterProfileReady());
        }
      },

      child: Scaffold(
        backgroundColor: theme.scaffoldBackgroundColor,
        appBar: SwiftAppBar(
          toolbarHeight: SwiftAppBar.toolbarHeightFor(context),
          title: const SwiftBrand(onBar: true),
          automaticallyImplyLeading: false,
          actions: [
            NotificationBell(
              role: 'RIDER',
              onOpenNotification: _openNotification,
            ),
            SwiftAppBarAction(
              icon: SwiftIcons.userRound,
              tooltip: 'Account',
              onPressed: widget.onAccountTap,
            ),
          ],
        ),

        body: Stack(
          children: [
            Positioned.fill(
              child: CustomScrollView(
                controller: _homeScrollController,
                physics: const BouncingScrollPhysics(),
                slivers: [
                  SliverToBoxAdapter(child: SizedBox(height: mapHeight)),
                  DecoratedSliver(
                    decoration: BoxDecoration(
                      color: theme.scaffoldBackgroundColor,
                    ),
                    sliver: SliverMainAxisGroup(
                      slivers: [
                        if (_activeRide != null)
                          SliverToBoxAdapter(
                            child: Padding(
                              padding: const EdgeInsets.only(top: 16),

                              child: _ActiveRideBanner(
                                ride: _activeRide!,

                                onTap: _openActiveRide,
                              ),
                            ),
                          ),

                        SliverToBoxAdapter(
                          child: Padding(
                            padding: const EdgeInsets.fromLTRB(16, 18, 16, 0),

                            child: _DestinationShortcut(
                              destination: _destinationName,

                              onTap: _chooseDestination,
                            ),
                          ),
                        ),

                        SliverToBoxAdapter(
                          child: Padding(
                            padding: const EdgeInsets.fromLTRB(16, 14, 16, 0),

                            child: BlocBuilder<UserBloc, UserState>(
                              builder: (context, state) {
                                if (state.user == null &&
                                    (state.status == UserStatus.loading ||
                                        state.status == UserStatus.initial)) {
                                  return const AppShimmer(
                                    child: Row(
                                      children: [
                                        Expanded(
                                          child: ShimmerBlock(
                                            width: double.infinity,

                                            height: 76,
                                          ),
                                        ),

                                        SizedBox(width: 10),

                                        Expanded(
                                          child: ShimmerBlock(
                                            width: double.infinity,

                                            height: 76,
                                          ),
                                        ),
                                      ],
                                    ),
                                  );
                                }

                                if (state.user == null &&
                                    state.status == UserStatus.failure) {
                                  return _HomeRetryCard(
                                    message:
                                        state.message ??
                                        'Saved places unavailable.',

                                    onRetry: () => context.read<UserBloc>().add(
                                      const FetchUserDetails(),
                                    ),
                                  );
                                }

                                final addresses =
                                    state.user?.allAddresses ??
                                    const <UserAddress>[];

                                UserAddress? findPlace(AddressTypeEnum type) {
                                  for (final address in addresses) {
                                    if (address.addressType == type) {
                                      return address;
                                    }
                                  }

                                  return null;
                                }

                                return Row(
                                  children: [
                                    Expanded(
                                      child: _SavedPlaceShortcut(
                                        label: 'Home',

                                        icon: SwiftIcons.house,

                                        address: findPlace(
                                          AddressTypeEnum.HOME,
                                        ),

                                        onTap: (address) => address == null
                                            ? _openAddAddressSheet()
                                            : _selectSavedAddress(address),
                                      ),
                                    ),

                                    const SizedBox(width: 10),

                                    Expanded(
                                      child: _SavedPlaceShortcut(
                                        label: 'Work',

                                        icon: SwiftIcons.briefcase,

                                        address: findPlace(
                                          AddressTypeEnum.WORK,
                                        ),

                                        onTap: (address) => address == null
                                            ? _openAddAddressSheet()
                                            : _selectSavedAddress(address),
                                      ),
                                    ),
                                  ],
                                );
                              },
                            ),
                          ),
                        ),

                        SliverToBoxAdapter(
                          child: Padding(
                            padding: const EdgeInsets.fromLTRB(20, 25, 20, 10),

                            child: Text(
                              'Available vehicle types',

                              style: theme.textTheme.titleMedium?.copyWith(
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ),
                        ),

                        SliverToBoxAdapter(
                          child: BlocBuilder<RideTypeBloc, RideTypeState>(
                            builder: (context, state) {
                              final types =
                                  state.rideTypes
                                      .where((type) => type.active)
                                      .toList()
                                    ..sort(
                                      (a, b) =>
                                          a.sortOrder.compareTo(b.sortOrder),
                                    );

                              if (types.isEmpty &&
                                  (state.status == RideTypeStatus.loading ||
                                      state.status == RideTypeStatus.initial)) {
                                return const Padding(
                                  padding: EdgeInsets.symmetric(horizontal: 16),

                                  child: AppShimmer(
                                    child: Row(
                                      children: [
                                        Expanded(
                                          child: ShimmerBlock(
                                            width: double.infinity,

                                            height: 112,
                                          ),
                                        ),

                                        SizedBox(width: 10),

                                        Expanded(
                                          child: ShimmerBlock(
                                            width: double.infinity,

                                            height: 112,
                                          ),
                                        ),

                                        SizedBox(width: 10),

                                        Expanded(
                                          child: ShimmerBlock(
                                            width: double.infinity,

                                            height: 112,
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                );
                              }

                              if (types.isEmpty) {
                                return Padding(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 16,
                                  ),

                                  child: _HomeRetryCard(
                                    message:
                                        state.status == RideTypeStatus.failure
                                        ? 'Vehicle types could not load.'
                                        : 'No vehicle types are available yet.',

                                    onRetry: () => context
                                        .read<RideTypeBloc>()
                                        .add(const FetchRideTypes()),
                                  ),
                                );
                              }

                              return SizedBox(
                                height:
                                    88 +
                                    MediaQuery.textScalerOf(
                                          context,
                                        ).scale(12.5) *
                                        1.15 +
                                    MediaQuery.textScalerOf(context).scale(10) *
                                        1.1,

                                child: ListView.separated(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 16,
                                  ),

                                  scrollDirection: Axis.horizontal,

                                  itemCount: types.length,

                                  separatorBuilder: (_, _) =>
                                      const SizedBox(width: 10),

                                  itemBuilder: (_, index) {
                                    final type = types[index];

                                    return _HomeRideTypeCard(
                                      type: type,

                                      onTap: () => _chooseDestination(
                                        preferredRideTypeCode: type.code,
                                      ),
                                    );
                                  },
                                ),
                              );
                            },
                          ),
                        ),

                        const SliverToBoxAdapter(child: SizedBox(height: 128)),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            // Draw the map ABOVE the scroll view so gestures reach GoogleMap.
            // As content scrolls upward, clip away the exposed map area.
            Positioned(
              top: 0,
              left: 0,
              right: 0,
              child: ValueListenableBuilder<double>(
                valueListenable: _homeScrollOffset,
                child: SizedBox(
                  height: mapHeight,
                  child: RepaintBoundary(
                    child: GoogleMapWidget(
                      active: !_homeMapPaused,
                      onInitialLoadSettled: widget.onInitialMapSettled,
                      showNearbyDrivers: true,
                      onLocationChanged: _onMapLocationChanged,
                    ),
                  ),
                ),
                builder: (context, offset, mapChild) {
                  final visibleHeight = (mapHeight - offset).clamp(
                    0.0,
                    mapHeight,
                  );
                  return SizedBox(
                    height: visibleHeight,
                    child: ClipRect(
                      child: Align(
                        alignment: Alignment.topCenter,
                        heightFactor: visibleHeight / mapHeight,
                        child: mapChild,
                      ),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ActiveRideBanner extends StatelessWidget {
  final RideResponse ride;

  final VoidCallback onTap;

  const _ActiveRideBanner({required this.ride, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    final status = (ride.currentStatus ?? 'MATCHING').toUpperCase();

    final label = switch (status) {
      'DRIVER_ASSIGNED' => 'Driver is on the way',

      'DRIVER_ARRIVED' => 'Driver has arrived',

      'STARTED' => 'Ride in progress',

      _ => 'Finding your driver',
    };

    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 2, 12, 12),

      child: GlassSurface(
        padding: const EdgeInsets.fromLTRB(14, 12, 10, 12),

        borderRadius: AppRadii.cardRadius,

        border: Border.all(
          color: theme.colorScheme.primary.withValues(alpha: 0.24),
        ),

        child: Row(
          children: [
            AppIcon(SwiftIcons.car, color: theme.colorScheme.primary),

            const SizedBox(width: 10),

            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,

                children: [
                  Text(label, style: theme.textTheme.titleSmall),

                  const SizedBox(height: 2),

                  Text(
                    'Ride #${ride.id ?? '-'} · Tap to view status',

                    style: theme.textTheme.bodySmall,
                  ),
                ],
              ),
            ),

            IconButton(
              onPressed: onTap,

              tooltip: 'View active ride',

              icon: const AppIcon(SwiftIcons.chevronRight),
            ),
          ],
        ),
      ),
    );
  }
}

class _DestinationShortcut extends StatelessWidget {
  const _DestinationShortcut({required this.destination, required this.onTap});

  final String? destination;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final hasDestination = destination?.trim().isNotEmpty == true;

    return Material(
      color: scheme.surface,
      borderRadius: BorderRadius.circular(50),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(50),
        child: Container(
          constraints: BoxConstraints(minHeight: 56),
          padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 5),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(50),
            border: Border.all(
              color: scheme.outlineVariant.withValues(alpha: 0.5),
              width: 0.8,
            ),
          ),
          child: Row(
            children: [
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  child: Text(
                    hasDestination
                        ? destination!
                        : 'Where would you like to go?',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.titleMedium?.copyWith(
                      fontSize: 14,
                      fontWeight: FontWeight.w500,
                      height: 1.3,
                    ),
                  ),
                ),
              ),

              const SizedBox(width: 5),

              // The full shortcut is a single destination-search action.
              Material(
                color: scheme.primary,
                shape: const CircleBorder(),
                child: InkWell(
                  onTap: onTap,
                  customBorder: const CircleBorder(),
                  child: SizedBox(
                    width: 44,
                    height: 44,
                    child: AppIcon(
                      SwiftIcons.search,
                      color: scheme.onPrimary,
                      size: 20,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SavedPlaceShortcut extends StatelessWidget {
  const _SavedPlaceShortcut({
    required this.label,
    required this.icon,
    required this.address,
    required this.onTap,
  });

  final String label;
  final SwiftIcons icon;
  final UserAddress? address;
  final ValueChanged<UserAddress?> onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;

    final hasAddress = address?.fullAddress.isNotEmpty == true;

    return Material(
      color: scheme.surface,
      borderRadius: BorderRadius.circular(50),
      child: InkWell(
        onTap: () => onTap(address),
        borderRadius: BorderRadius.circular(50),
        child: Container(
          constraints: BoxConstraints(
            minHeight:
                (MediaQuery.textScalerOf(context).scale(12.5) * 1.15 +
                        MediaQuery.textScalerOf(context).scale(10.5) * 1.15 +
                        14)
                    .clamp(54.0, double.infinity),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(50),
            border: Border.all(
              color: scheme.outlineVariant.withValues(alpha: 0.45),
              width: 0.8,
            ),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 36,
                height: 36,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: scheme.primary.withValues(alpha: 0.08),
                ),
                child: AppIcon(icon, size: 18, color: scheme.primary),
              ),

              const SizedBox(width: 9),

              Flexible(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      label,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.labelMedium?.copyWith(
                        fontSize: 12.5,
                        fontWeight: FontWeight.w700,
                        height: 1.15,
                      ),
                    ),

                    const SizedBox(height: 2),

                    Text(
                      hasAddress ? address!.fullAddress : 'Add place',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodySmall?.copyWith(
                        fontSize: 10.5,
                        height: 1.15,
                        color: scheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(width: 3),
            ],
          ),
        ),
      ),
    );
  }
}

class _HomeRideTypeCard extends StatelessWidget {
  const _HomeRideTypeCard({required this.type, required this.onTap});

  final RideType type;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final isDark = theme.brightness == Brightness.dark;

    final hasDescription = type.description.trim().isNotEmpty;

    return SizedBox(
      width: 128,
      child: Material(
        color: scheme.surface,
        borderRadius: BorderRadius.circular(20),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(20),
          splashColor: scheme.primary.withValues(alpha: 0.08),
          highlightColor: scheme.primary.withValues(alpha: 0.04),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: scheme.outlineVariant.withValues(
                  alpha: isDark ? 0.35 : 0.55,
                ),
                width: 0.8,
              ),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                VehiclePhoto(code: type.code, width: 100, height: 50),

                const SizedBox(height: 9),

                // Vehicle name
                Text(
                  type.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  textAlign: TextAlign.center,
                  style: theme.textTheme.labelLarge?.copyWith(
                    fontSize: 12.5,
                    fontWeight: FontWeight.w700,
                    letterSpacing: -0.2,
                    height: 1.15,
                    color: scheme.onSurface,
                  ),
                ),

                if (hasDescription) ...[
                  const SizedBox(height: 3),

                  // Vehicle description
                  Text(
                    type.description,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    textAlign: TextAlign.center,
                    style: theme.textTheme.bodySmall?.copyWith(
                      fontSize: 10,
                      fontWeight: FontWeight.w400,
                      color: scheme.onSurfaceVariant,
                      height: 1.1,
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
}

class _HomeRetryCard extends StatelessWidget {
  const _HomeRetryCard({required this.message, required this.onRetry});

  final String message;

  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => Row(
    children: [
      const AppIcon(SwiftIcons.info, size: 19),

      const SizedBox(width: 8),

      Expanded(child: Text(message)),

      TextButton(onPressed: onRetry, child: const Text('Retry')),
    ],
  );
}
