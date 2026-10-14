import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/constants/widgets/primary_button_widget.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/common/fare/data/model/fare_estimate_request_model.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_bloc.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_event.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_state.dart';
import 'package:swift_app/features/common/fare/presentation/widgets/fare_estimate_widget.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_request.dart';
import 'package:swift_app/features/rider/home/domain/entity/offer_response.dart';
import 'package:swift_app/features/rider/home/domain/entity/ride_type.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_event.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/offer_section_widget.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_state.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/models/location_selection.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/ride_type_section_widget.dart';
import 'package:swift_app/features/rider/ride/domain/entity/route.dart'
    as ride_models;
import 'package:swift_app/features/rider/ride/domain/entity/ride_response.dart';
import 'package:swift_app/features/rider/ride/domain/entity/ride_request.dart';
import 'package:swift_app/features/rider/ride/presentation/blocs/ride_bloc.dart';
import 'package:swift_app/features/rider/ride/presentation/blocs/ride_event.dart';
import 'package:swift_app/features/rider/ride/presentation/blocs/ride_state.dart';
import 'package:swift_app/features/rider/ride/presentation/pages/ride_status_page.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:geolocator/geolocator.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

/// The final, explicit step between choosing a destination and requesting a
/// ride. It keeps price, ride type, discount, and the primary action together
/// so a rider never has to guess what happens after tapping a vehicle card.
class RideOptionsPage extends StatefulWidget {
  final TripLocations trip;

  const RideOptionsPage({super.key, required this.trip});

  @override
  State<RideOptionsPage> createState() => _RideOptionsPageState();
}

class _RideOptionsPageState extends State<RideOptionsPage> {
  RideType? _selectedRideType;
  OfferResponse? _selectedOffer;
  bool _openingStatus = false;

  Position get _pickup => widget.trip.pickup.position;
  Position get _destination => widget.trip.destination.position;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _selectInitialRide());
  }

  void _selectInitialRide() {
    if (!mounted || _selectedRideType != null) return;
    final state = context.read<RideTypeBloc>().state;
    final rides = state.rideTypes.where((ride) => ride.active).toList()
      ..sort((a, b) => a.sortOrder.compareTo(b.sortOrder));
    if (rides.isNotEmpty) _selectRideType(rides.first);
  }

  void _selectRideType(RideType rideType) {
    if (!mounted) return;
    setState(() {
      _selectedRideType = rideType;
      _selectedOffer = null;
    });

    context.read<RideTypeBloc>().add(SelectRideType(rideTypeId: rideType.id));
    context.read<OfferBloc>().add(
      FetchOffers(
        request: OfferRequest(
          rideTypeCode: rideType.code,
          startFrom: DateTime.now(),
        ),
      ),
    );
    _estimateFare();
  }

  void _selectOffer(OfferResponse offer) {
    setState(() => _selectedOffer = offer);
    _estimateFare(offerId: offer.id);
  }

  void _estimateFare({int? offerId}) {
    context.read<FareBloc>().add(
      EstimateFare(
        request: FareEstimateRequestModel(
          offerId: offerId,
          pickupLatitude: _pickup.latitude,
          pickupLongitude: _pickup.longitude,
          destinationLatitude: _destination.latitude,
          destinationLongitude: _destination.longitude,
          rideType: _selectedRideType?.code,
        ),
      ),
    );
  }

  void _requestRide() {
    final rideType = _selectedRideType;
    final user = context.read<UserBloc>().state.user;
    final authUserId = sl<AuthLocalDatasource>().userId;
    if (rideType == null ||
        rideType.code.isEmpty ||
        user == null ||
        authUserId == null) {
      return;
    }

    context.read<RideBloc>().add(
      BookRide(
        request: RideRequest(
          authUserId: authUserId,
          riderId: user.id,
          offerId: _selectedOffer?.id,
          rideType: rideType.code,
          route: ride_models.Route(
            offerId: _selectedOffer?.id,
            pickupLatitude: _pickup.latitude,
            pickupLongitude: _pickup.longitude,
            destinationLatitude: _destination.latitude,
            destinationLongitude: _destination.longitude,
            rideType: rideType.code,
          ),
        ),
      ),
    );
  }

  Future<void> _openRideStatus(RideResponse ride, int riderId) async {
    if (_openingStatus || ride.id == null) return;
    _openingStatus = true;
    final retry = await Navigator.of(context).push<bool>(
      CupertinoPageRoute(
        builder: (_) => RideStatusPage(ride: ride, riderId: riderId),
      ),
    );
    if (!mounted) return;
    _openingStatus = false;
    context.read<RideBloc>().add(const ResetRide());
    if (retry != true) Navigator.of(context).pop();
  }

  String _locationLabel(LocationSelection selection, String fallback) {
    final value = selection.label?.trim() ?? '';
    return value.isEmpty ? fallback : value;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;

    return MultiBlocListener(
      listeners: [
        BlocListener<RideTypeBloc, RideTypeState>(
          listenWhen: (previous, current) =>
              previous.rideTypes != current.rideTypes ||
              previous.status != current.status,
          listener: (_, state) {
            if (_selectedRideType == null && state.rideTypes.isNotEmpty) {
              _selectInitialRide();
            }
          },
        ),
        BlocListener<RideBloc, RideState>(
          listenWhen: (previous, current) =>
              previous.status != current.status &&
              current.status == RideStatus.success,
          listener: (_, state) {
            final user = context.read<UserBloc>().state.user;
            if (state.ride != null && user != null) {
              _openRideStatus(state.ride!, user.id);
            }
          },
        ),
      ],
      child: Scaffold(
        backgroundColor: Colors.transparent,
        appBar: SwiftAppBar(
          toolbarHeight: SwiftAppBar.toolbarHeightFor(
            context,
            hasSubtitle: true,
          ),
          title: const Text('Choose your ride'),
          subtitle: 'Review the route and request when ready',
          automaticallyImplyLeading: false,
          leading: SwiftAppBarAction(
            icon: SwiftIcons.chevronLeft,
            onPressed: () => Navigator.of(context).pop(),
            tooltip: 'Back',
          ),
        ),
        body: AppGlassBackground(
          child: SafeArea(
            top: false,
            bottom: false,
            child: CustomScrollView(
              physics: const BouncingScrollPhysics(
                parent: AlwaysScrollableScrollPhysics(),
              ),
              slivers: [
                SliverToBoxAdapter(child: _routeCard(theme)),
                const SliverToBoxAdapter(child: SizedBox(height: 6)),
                SliverToBoxAdapter(
                  child: RideTypeSection(onRideTypeSelected: _selectRideType),
                ),
                SliverToBoxAdapter(child: const FareEstimateWidget()),
                SliverToBoxAdapter(
                  child: OfferSection(
                    expanded: true,
                    selectedOfferId: _selectedOffer?.id,
                    onOfferSelected: _selectOffer,
                  ),
                ),
                SliverToBoxAdapter(
                  child: BlocBuilder<RideBloc, RideState>(
                    builder: (context, state) {
                      if (!state.hasError) return const SizedBox.shrink();
                      return Padding(
                        padding: const EdgeInsets.fromLTRB(16, 0, 16, 14),
                        child: GlassSurface(
                          padding: const EdgeInsets.all(14),
                          borderRadius: AppRadii.cardRadius,
                          child: Row(
                            children: [
                              AppIcon(
                                SwiftIcons.circleAlert,
                                color: scheme.error,
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: Text(
                                  state.errorMessage ??
                                      'We could not request this ride.',
                                  style: theme.textTheme.bodySmall,
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ),
                const SliverToBoxAdapter(child: SizedBox(height: 24)),
              ],
            ),
          ),
        ),
        bottomNavigationBar: _requestBar(context),
      ),
    );
  }

  Widget _routeCard(ThemeData theme) {
    final pickupLabel = _locationLabel(widget.trip.pickup, 'Pickup');
    final destinationLabel = _locationLabel(
      widget.trip.destination,
      'Destination',
    );
    final center = LatLng(
      (_pickup.latitude + _destination.latitude) / 2,
      (_pickup.longitude + _destination.longitude) / 2,
    );

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
      child: GlassSurface(
        padding: EdgeInsets.zero,
        borderRadius: AppRadii.cardRadius,
        clipBehavior: Clip.antiAlias,
        child: Column(
          children: [
            SizedBox(
              height: 178,
              child: GoogleMap(
                initialCameraPosition: CameraPosition(target: center, zoom: 12),
                markers: {
                  Marker(
                    markerId: const MarkerId('options-pickup'),
                    position: LatLng(_pickup.latitude, _pickup.longitude),
                    icon: BitmapDescriptor.defaultMarkerWithHue(
                      BitmapDescriptor.hueGreen,
                    ),
                  ),
                  Marker(
                    markerId: const MarkerId('options-destination'),
                    position: LatLng(
                      _destination.latitude,
                      _destination.longitude,
                    ),
                    icon: BitmapDescriptor.defaultMarkerWithHue(
                      BitmapDescriptor.hueRed,
                    ),
                  ),
                },
                zoomControlsEnabled: false,
                myLocationButtonEnabled: false,
                mapToolbarEnabled: false,
                compassEnabled: false,
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(14, 13, 14, 14),
              child: Column(
                children: [
                  _routeRow(
                    icon: SwiftIcons.mapPin,
                    color: Theme.of(context).colorScheme.primary,
                    label: pickupLabel,
                  ),
                  const SizedBox(height: 9),
                  _routeRow(
                    icon: SwiftIcons.flag,
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                    label: destinationLabel,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _routeRow({
    required SwiftIcons icon,
    required Color color,
    required String label,
  }) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        AppIcon(icon, size: 17, color: color),
        const SizedBox(width: 9),
        Expanded(
          child: Text(
            label,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(
              context,
            ).textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w600),
          ),
        ),
      ],
    );
  }

  Widget _requestBar(BuildContext context) {
    return SafeArea(
      top: false,
      minimum: const EdgeInsets.fromLTRB(16, 8, 16, 10),
      child: GlassSurface(
        borderRadius: AppRadii.cardRadius,
        padding: const EdgeInsets.fromLTRB(12, 10, 12, 10),
        child: BlocBuilder2<FareBloc, FareState, RideBloc, RideState>(
          builder: (context, fareState, rideState) {
            final userReady = context.read<UserBloc>().state.user != null;
            final canRequest =
                _selectedRideType != null &&
                userReady &&
                fareState.status == FareStatus.success &&
                !rideState.isLoading;
            final label = rideState.isLoading
                ? 'Requesting ride…'
                : _selectedRideType == null
                ? 'Select a ride type'
                : fareState.status == FareStatus.loading
                ? 'Calculating fare…'
                : fareState.status == FareStatus.failure
                ? 'Retry fare estimate'
                : fareState.status == FareStatus.initial
                ? 'Calculating fare…'
                : 'Request ${_selectedRideType!.name}';

            return ConstrainedBox(
              constraints: const BoxConstraints(
                minWidth: double.infinity,
                minHeight: 52,
              ),
              child: AppPrimaryButton(
                text: label,
                onPressed:
                    fareState.status == FareStatus.failure &&
                        _selectedRideType != null &&
                        !rideState.isLoading
                    ? () => _estimateFare(offerId: _selectedOffer?.id)
                    : canRequest
                    ? _requestRide
                    : null,
                loading: rideState.isLoading,
              ),
            );
          },
        ),
      ),
    );
  }
}

/// Small helper that combines two bloc builders without rebuilding the whole
/// options page for every fare/booking state change.
class BlocBuilder2<
  A extends StateStreamable<S1>,
  S1,
  B extends StateStreamable<S2>,
  S2
>
    extends StatelessWidget {
  final Widget Function(BuildContext, S1, S2) builder;

  const BlocBuilder2({super.key, required this.builder});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<A, S1>(
      builder: (context, first) => BlocBuilder<B, S2>(
        builder: (context, second) => builder(context, first, second),
      ),
    );
  }
}
