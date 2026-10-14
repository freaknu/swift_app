import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_bloc.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_event.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_state.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/widgets/auth_home_ready_reporter.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:swift_app/features/common/shared/widgets/app_loading_page.dart';
import 'package:swift_app/features/common/shared/widgets/lazy_tab_stack.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/common/shared/widgets/swift_bottom_navigation_bar.dart';
import 'package:swift_app/features/common/splash/welcome/presentation/pages/welcome_page.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_state.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_history_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_history_state.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_state.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_state.dart';
import 'package:swift_app/features/rider/home/presentation/pages/home_page.dart';
import 'package:swift_app/features/rider/home/presentation/pages/rider_activity_page.dart';
import 'package:swift_app/features/rider/home/presentation/pages/account_settings_pages.dart';
import 'package:swift_app/features/rider/home/presentation/pages/trip_location_page.dart';
import 'package:swift_app/features/rider/home/presentation/pages/trip_route_map_page.dart';
import 'package:swift_app/features/rider/home/presentation/models/location_selection.dart';
import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';
import 'package:swift_app/core/notifications/notification_inbox_page.dart';
import 'package:swift_app/core/notifications/notification_inbox_datasource.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/offer_section_widget.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/saved_address_section_widget.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:get/get.dart';

class MainPage extends StatefulWidget {
  const MainPage({super.key, this.onHomeReady});

  final Future<bool> Function()? onHomeReady;

  @override
  State<MainPage> createState() => _MainPageState();
}

class _MainPageState extends State<MainPage> {
  int _currentIndex = 0;
  bool _initialMapSettled = false;
  bool _initialRideCheckSettled = false;
  final _activityKey = GlobalKey<RiderActivityPageState>();

  void _onInitialMapSettled() {
    if (!mounted || _initialMapSettled) return;
    setState(() => _initialMapSettled = true);
  }

  void _onInitialRideCheckSettled() {
    if (!mounted || _initialRideCheckSettled) return;
    setState(() => _initialRideCheckSettled = true);
  }

  void _refreshCurrentTab() {
    switch (_currentIndex) {
      case 1:
      case 4:
        context.read<UserBloc>().add(const FetchUserDetails());
        return;
      case 2:
        _activityKey.currentState?.refresh();
        return;
      case 3:
        final bloc = context.read<OfferBloc>();
        final request = bloc.state.lastRequest;
        if (request != null) bloc.add(FetchOffers(request: request));
        return;
    }
  }

  Future<void> _bookSavedPlace(UserAddress address) async {
    final trip = await Navigator.of(context).push<TripLocations>(
      MaterialPageRoute(
        builder: (_) => TripLocationPage(
          initialDestination: LocationSelection.fromCoordinates(
            latitude: address.latitude,
            longitude: address.longitude,
            label: address.label,
          ),
        ),
      ),
    );
    if (!mounted || trip == null) return;
    await Navigator.of(
      context,
    ).push(MaterialPageRoute(builder: (_) => TripRouteMapPage(trip: trip)));
  }

  @override
  Widget build(BuildContext context) {
    final user = context.watch<UserBloc>().state;
    final catalog = context.watch<RideTypeBloc>().state;
    final offers = context.watch<OfferBloc>().state;
    final history = context.watch<RideHistoryBloc>().state;
    final userSettled =
        user.user != null ||
        user.status == UserStatus.success ||
        user.status == UserStatus.failure;
    final catalogSettled =
        catalog.rideTypes.isNotEmpty ||
        catalog.status == RideTypeStatus.success ||
        catalog.status == RideTypeStatus.failure;
    final offersSettled =
        offers.offers.isNotEmpty ||
        offers.status == OfferStatus.success ||
        offers.status == OfferStatus.failure;
    final historySettled =
        history.rides.isNotEmpty ||
        history.status == RideHistoryStatus.success ||
        history.status == RideHistoryStatus.failure;
    // A missing essential response must expose the page's retry controls even
    // when an unrelated request or location initialization is still pending.
    final criticalFailure =
        (user.user == null &&
            (user.status == UserStatus.failure ||
                user.status == UserStatus.success)) ||
        (catalog.rideTypes.isEmpty && catalog.status == RideTypeStatus.failure);
    final ready =
        criticalFailure ||
        (_initialMapSettled &&
            _initialRideCheckSettled &&
            userSettled &&
            catalogSettled &&
            offersSettled &&
            historySettled);
    return BlocListener<AuthBloc, AuthState>(
      listenWhen: (previous, current) =>
          previous is! AuthUnauthenticated && current is AuthUnauthenticated,
      listener: (context, state) => Get.offAll(() => const WelcomePage()),
      child: AuthHomeReadyReporter(
        ready: ready && !criticalFailure && user.user != null,
        onReady: widget.onHomeReady,
        child: AppStartupGate(
          ready: ready,
          child: Scaffold(
            extendBody: true,
            backgroundColor: Theme.of(context).scaffoldBackgroundColor,
            appBar: _currentIndex == 0
                ? null
                : SwiftAppBar(
                    toolbarHeight: SwiftAppBar.toolbarHeightFor(
                      context,
                      hasSubtitle: true,
                    ),
                    title: Text(switch (_currentIndex) {
                      1 => 'Saved',
                      2 => 'Activity',
                      3 => 'Offers',
                      _ => 'Account',
                    }),
                    subtitle: switch (_currentIndex) {
                      1 => 'Places you can reach in a tap',
                      2 => 'Your journeys, in one place',
                      3 => 'A little extra for your next ride',
                      _ => 'Profile and preferences',
                    },
                    automaticallyImplyLeading: false,
                    actions: [
                      IconButton(
                        icon: const AppIcon(SwiftIcons.refreshCw, size: 20),
                        onPressed: _refreshCurrentTab,
                        tooltip: 'Refresh',
                      ),
                    ],
                  ),
            body: LazyTabStack(
              index: _currentIndex,
              builders: [
                (_) => HomePage(
                  onAccountTap: () => setState(() => _currentIndex = 4),
                  onInitialMapSettled: _onInitialMapSettled,
                  onInitialRideCheckSettled: _onInitialRideCheckSettled,
                ),
                (_) => _CollectionPage(
                  title: 'Saved',
                  childIsSliver: true,
                  child: SavedAddressSection(
                    expanded: true,
                    asSliver: true,
                    onAddressSelected: _bookSavedPlace,
                  ),
                ),
                (_) => RiderActivityPage(key: _activityKey),
                (_) => _CollectionPage(
                  title: 'Offers',
                  childIsSliver: true,
                  child: const OfferSection(
                    expanded: true,
                    browseCategories: true,
                    asSliver: true,
                  ),
                ),
                (_) => _AccountPage(
                  onSaved: () => setState(() => _currentIndex = 1),
                ),
              ],
            ),
            bottomNavigationBar: SwiftBottomNavigationBar(
              selectedIndex: _currentIndex,
              onDestinationSelected: (index) {
                if (_currentIndex == index) return;
                HapticFeedback.selectionClick();
                FocusManager.instance.primaryFocus?.unfocus();
                setState(() => _currentIndex = index);
              },
              destinations: const [
                SwiftNavigationDestination(
                  icon: SwiftIcons.navigation,
                  label: 'Ride',
                ),
                SwiftNavigationDestination(
                  icon: SwiftIcons.bookmark,
                  label: 'Saved',
                ),
                SwiftNavigationDestination(
                  icon: SwiftIcons.clock3,
                  label: 'Activity',
                ),
                SwiftNavigationDestination(
                  icon: SwiftIcons.tag,
                  label: 'Offers',
                ),
                SwiftNavigationDestination(
                  icon: SwiftIcons.userRound,
                  label: 'Account',
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _CollectionPage extends StatelessWidget {
  final String title;
  final Widget child;
  final bool childIsSliver;

  const _CollectionPage({
    required this.title,
    required this.child,
    this.childIsSliver = false,
  });

  @override
  Widget build(BuildContext context) {
    return AppGlassBackground(
      child: SafeArea(
        bottom: false,
        child: Align(
          alignment: Alignment.topCenter,
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 700),
            child: CustomScrollView(
              key: PageStorageKey(title),
              physics: const BouncingScrollPhysics(
                parent: AlwaysScrollableScrollPhysics(),
              ),
              slivers: [
                const SliverToBoxAdapter(child: SizedBox(height: 16)),
                if (childIsSliver) child else SliverToBoxAdapter(child: child),
                SliverToBoxAdapter(
                  child: SizedBox(
                    height: MediaQuery.paddingOf(context).bottom + 28,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _AccountPage extends StatelessWidget {
  final VoidCallback onSaved;
  const _AccountPage({required this.onSaved});

  @override
  Widget build(BuildContext context) {
    return _CollectionPage(
      title: 'Account',
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            BlocBuilder<UserBloc, UserState>(
              builder: (context, state) {
                final user = state.user;
                if (user == null) {
                  if (state.status == UserStatus.initial ||
                      state.status == UserStatus.loading) {
                    return const GlassSurface(
                      semanticLabel: 'Loading your account',
                      padding: EdgeInsets.all(16),
                      child: AppShimmer(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                ShimmerBlock(width: 52, height: 52, radius: 26),
                                SizedBox(width: 14),
                                ShimmerBlock(width: 152, height: 18),
                              ],
                            ),
                            SizedBox(height: 22),
                            ShimmerBlock(width: 44, height: 11),
                            SizedBox(height: 9),
                            ShimmerBlock(width: double.infinity, height: 15),
                            SizedBox(height: 20),
                            ShimmerBlock(width: 48, height: 11),
                            SizedBox(height: 9),
                            ShimmerBlock(width: 180, height: 15),
                          ],
                        ),
                      ),
                    );
                  }
                  return GlassSurface(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      children: [
                        AppIcon(
                          SwiftIcons.cloudOff,
                          size: 22,
                          color: Theme.of(context).colorScheme.onSurfaceVariant,
                        ),
                        const SizedBox(height: 12),
                        const Text(
                          'Unable to load account.',
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 12),
                        OutlinedButton(
                          onPressed: () => context.read<UserBloc>().add(
                            const FetchUserDetails(),
                          ),
                          child: const Text('Try again'),
                        ),
                      ],
                    ),
                  );
                }
                final name = user.fullName.trim();
                return GlassSurface(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          CircleAvatar(
                            radius: 26,
                            backgroundColor: Theme.of(
                              context,
                            ).colorScheme.primaryContainer,
                            child: Text(
                              name.isEmpty
                                  ? 'S'
                                  : name.characters.first.toUpperCase(),
                              style: Theme.of(context).textTheme.titleLarge
                                  ?.copyWith(
                                    color: Theme.of(
                                      context,
                                    ).colorScheme.primary,
                                  ),
                            ),
                          ),
                          const SizedBox(width: 14),
                          Expanded(
                            child: Text(
                              name.isEmpty ? 'Swift rider' : name,
                              style: Theme.of(context).textTheme.titleMedium,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),
                      OutlinedButton.icon(
                        onPressed: () => Navigator.of(context).push(
                          MaterialPageRoute(
                            builder: (_) => BlocProvider.value(
                              value: context.read<UserBloc>(),
                              child: EditRiderProfilePage(user: user),
                            ),
                          ),
                        ),
                        icon: const AppIcon(SwiftIcons.pencil, size: 18),
                        label: const Text('Edit profile'),
                      ),
                      const Divider(),
                      _AccountDetail(
                        icon: SwiftIcons.mail,
                        label: 'Email',
                        value: user.email?.trim().isNotEmpty == true
                            ? user.email!
                            : 'Not provided',
                      ),
                      const SizedBox(height: 16),
                      _AccountDetail(
                        icon: SwiftIcons.phone,
                        label: 'Phone',
                        value: user.phoneNumber.isEmpty
                            ? 'Not provided'
                            : user.phoneNumber,
                      ),
                    ],
                  ),
                );
              },
            ),
            const SizedBox(height: 12),
            GlassSurface(
              child: Column(
                children: [
                  ListTile(
                    leading: const AppIcon(SwiftIcons.bell, size: 20),
                    title: const Text('Notifications'),
                    trailing: const AppIcon(SwiftIcons.chevronRight, size: 16),
                    onTap: () => Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => const NotificationPreferencesPage(),
                      ),
                    ),
                  ),
                  const Divider(),
                  ListTile(
                    leading: const AppIcon(SwiftIcons.inbox, size: 20),
                    title: const Text('Notification inbox'),
                    trailing: const AppIcon(SwiftIcons.chevronRight, size: 16),
                    onTap: () => Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => NotificationInboxPage(
                          role: 'RIDER',
                          datasource: sl<NotificationInboxDatasource>(),
                        ),
                      ),
                    ),
                  ),
                  const Divider(),
                  ListTile(
                    leading: const AppIcon(SwiftIcons.contactRound, size: 20),
                    title: const Text('Emergency contacts'),
                    trailing: const AppIcon(SwiftIcons.chevronRight, size: 16),
                    onTap: () => Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (_) => const EmergencyContactsPage(),
                      ),
                    ),
                  ),
                  const Divider(),
                  ListTile(
                    leading: const AppIcon(SwiftIcons.bookmark, size: 20),
                    title: const Text('Saved places'),
                    trailing: const AppIcon(SwiftIcons.chevronRight, size: 16),
                    onTap: onSaved,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            BlocSelector<AuthBloc, AuthState, bool>(
              selector: (state) =>
                  state is AuthLoading && state.action == AuthAction.logout,
              builder: (context, signingOut) {
                return OutlinedButton.icon(
                  onPressed: signingOut
                      ? null
                      : () => context.read<AuthBloc>().add(
                          const AuthLogoutRequested(),
                        ),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: Theme.of(context).colorScheme.error,
                    side: BorderSide.none,
                  ),
                  icon: const AppIcon(SwiftIcons.logOut, size: 18),
                  label: Text(signingOut ? 'Signing out…' : 'Sign out'),
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _AccountDetail extends StatelessWidget {
  final SwiftIcons icon;
  final String label;
  final String value;
  const _AccountDetail({
    required this.icon,
    required this.label,
    required this.value,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        AppIcon(
          icon,
          color: Theme.of(context).colorScheme.onSurfaceVariant,
          size: 20,
        ),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: Theme.of(context).textTheme.bodySmall),
              const SizedBox(height: 4),
              SelectableText(
                value,
                style: Theme.of(context).textTheme.bodyLarge,
              ),
            ],
          ),
        ),
      ],
    );
  }
}
