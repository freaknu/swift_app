import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:swift_app/core/notifications/notification_inbox_datasource.dart';
import 'package:swift_app/core/notifications/notification_inbox_page.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_bloc.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_event.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_state.dart';
import 'package:swift_app/features/common/shared/theme_toggle_button.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/driver/data/model/driver_document_model.dart';
import 'package:swift_app/features/driver/data/model/driver_profile_model.dart';
import 'package:swift_app/features/driver/data/model/profile_completeness_model.dart';
import 'package:swift_app/features/driver/presentation/pages/driver_notification_preferences_page.dart';

class DriverAccountTab extends StatefulWidget {
  final DriverRemoteDatasource datasource;
  final DriverProfileModel? profile;
  final List<DriverVehicleModel> vehicles;
  final ProfileCompletenessModel? completeness;
  final bool showNotifications;
  final Future<void> Function() onRefreshProfile;
  final Future<void> Function({int initialTab}) onOpenProfile;
  final ValueChanged<InboxNotification> onOpenNotifications;

  const DriverAccountTab({
    super.key,
    required this.datasource,
    required this.profile,
    required this.vehicles,
    required this.completeness,
    required this.showNotifications,
    required this.onRefreshProfile,
    required this.onOpenProfile,
    required this.onOpenNotifications,
  });

  @override
  State<DriverAccountTab> createState() => _DriverAccountTabState();
}

class _DriverAccountTabState extends State<DriverAccountTab> {
  List<DriverDocumentModel>? _documents;
  bool _loadingDocuments = false;
  bool _documentsFailed = false;

  @override
  void initState() {
    super.initState();
    _loadDocuments();
  }

  @override
  void didUpdateWidget(covariant DriverAccountTab oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.profile?.driverId != widget.profile?.driverId) {
      _documents = null;
      _loadingDocuments = false;
      _documentsFailed = false;
      _loadDocuments();
    }
  }

  Future<void> _loadDocuments() async {
    final driverId = widget.profile?.driverId;
    if (driverId == null || _loadingDocuments) return;
    setState(() => _loadingDocuments = true);
    final response = await widget.datasource.getDocuments();
    if (!mounted || widget.profile?.driverId != driverId) return;
    setState(() {
      _loadingDocuments = false;
      _documentsFailed = !response.success;
      _documents = response.success ? response.data : null;
    });
  }

  Future<void> _openProfile(int tab) async {
    await widget.onOpenProfile(initialTab: tab);
    if (mounted) await _loadDocuments();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final profile = widget.profile;
    final vehicle = widget.vehicles.isEmpty ? null : widget.vehicles.first;
    final vehicleSubtitle = vehicle == null
        ? 'Add a vehicle'
        : [
            vehicle.displayName,
            if (vehicle.registrationNumber.trim().isNotEmpty)
              vehicle.registrationNumber.trim(),
            if (vehicle.verified == true) 'Verified',
            if (vehicle.verified == false) 'Review pending',
          ].join(' · ');
    final name = profile?.driverName.trim().isNotEmpty == true
        ? profile!.driverName.trim()
        : 'Driver';
    final initials = name
        .split(RegExp(r'\s+'))
        .where((part) => part.isNotEmpty)
        .take(2)
        .map((part) => part[0].toUpperCase())
        .join();
    final completedSections = [
      widget.completeness?.personal.complete ?? false,
      widget.completeness?.vehicle.complete ?? false,
      widget.completeness?.documents.complete ?? false,
    ].where((value) => value).length;

    return RefreshIndicator.adaptive(
      onRefresh: () async {
        await Future.wait([_loadDocuments(), widget.onRefreshProfile()]);
      },
      child: ListView(
        key: const PageStorageKey('driver-account'),
        keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
        physics: const AlwaysScrollableScrollPhysics(),
        padding: EdgeInsets.fromLTRB(
          16,
          0,
          16,
          32 + MediaQuery.paddingOf(context).bottom,
        ),
        children: [
          SwiftAppBar(
            toolbarHeight: SwiftAppBar.toolbarHeightFor(context),
            title: const Text('Account'),
            primary: false,
            titleSpacing: 0,
            automaticallyImplyLeading: false,
            actions: [
              if (widget.showNotifications)
                NotificationBell(
                  role: 'DRIVER',
                  onOpenNotification: widget.onOpenNotifications,
                ),
            ],
          ),
          const SizedBox(height: 18),
          AppCard(
            padding: const EdgeInsets.all(18),
            child: Row(
              children: [
                CircleAvatar(
                  radius: 24,
                  backgroundColor: scheme.primary.withValues(alpha: 0.12),
                  child: Text(
                    initials.isEmpty ? 'D' : initials,
                    style: theme.textTheme.titleLarge?.copyWith(
                      color: scheme.primary,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        name,
                        style: theme.textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        profile?.email.isNotEmpty == true
                            ? profile!.email
                            : 'Driver partner',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: scheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  onPressed: () => _openProfile(0),
                  tooltip: 'Edit personal details',
                  icon: const AppIcon(SwiftIcons.chevronRight, size: 19),
                ),
              ],
            ),
          ),
          if (widget.completeness != null &&
              !widget.completeness!.complete) ...[
            const SizedBox(height: 12),
            AppCard(
              padding: const EdgeInsets.all(16),
              onTap: () => _openProfile(0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Complete your profile',
                    style: theme.textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 8),
                  LinearProgressIndicator(
                    value: completedSections / 3,
                    minHeight: 6,
                    borderRadius: BorderRadius.circular(99),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '$completedSections of 3 sections complete',
                    style: theme.textTheme.bodySmall,
                  ),
                ],
              ),
            ),
          ],
          const SizedBox(height: 24),
          _SectionTitle('Your details'),
          const SizedBox(height: 10),
          AppCard(
            padding: EdgeInsets.zero,
            child: Column(
              children: [
                _AccountRow(
                  icon: SwiftIcons.userRound,
                  title: 'Personal details',
                  subtitle: 'Name, phone, and date of birth',
                  onTap: () => _openProfile(0),
                ),
                const _RowDivider(),
                _AccountRow(
                  icon: SwiftIcons.car,
                  title: 'Vehicle',
                  subtitle: vehicleSubtitle,
                  onTap: () => _openProfile(1),
                ),
                const _RowDivider(),
                _AccountRow(
                  icon: SwiftIcons.fileText,
                  title: 'Documents',
                  subtitle: _loadingDocuments && _documents == null
                      ? 'Loading documents…'
                      : _documentsFailed
                      ? 'Could not load documents · open to retry'
                      : _documents == null
                      ? 'View and upload documents'
                      : '${_documents!.length} uploaded · ${_documents!.where((item) => const {'APPROVED', 'VERIFIED'}.contains(item.verificationStatus.toUpperCase())).length} approved',
                  onTap: () => _openProfile(2),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
          _SectionTitle('Preferences'),
          const SizedBox(height: 10),
          AppCard(
            padding: EdgeInsets.zero,
            child: Column(
              children: [
                _AccountRow(
                  icon: SwiftIcons.bell,
                  title: 'Notifications',
                  subtitle: 'Push, email, and ride alerts',
                  onTap: () => Navigator.of(context).push(
                    MaterialPageRoute<void>(
                      builder: (_) => DriverNotificationPreferencesPage(
                        datasource: widget.datasource,
                      ),
                    ),
                  ),
                ),
                const _RowDivider(),
                const _AccountRow(
                  icon: SwiftIcons.sun,
                  title: 'Appearance',
                  subtitle: 'Choose your app theme',
                  trailing: ThemeToggleButton(),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
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
                icon: const AppIcon(SwiftIcons.logOut, size: 19),
                label: Text(signingOut ? 'Signing out…' : 'Sign out'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: scheme.error,
                  minimumSize: const Size.fromHeight(50),
                  side: BorderSide(color: scheme.error.withValues(alpha: 0.35)),
                ),
              );
            },
          ),
        ],
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;
  const _SectionTitle(this.title);
  @override
  Widget build(BuildContext context) => Text(
    title,
    style: Theme.of(context).textTheme.titleSmall?.copyWith(
      color: Theme.of(context).colorScheme.onSurfaceVariant,

      fontWeight: FontWeight.w600,
    ),
  );
}

class _AccountRow extends StatelessWidget {
  final SwiftIcons icon;
  final String title;
  final String subtitle;
  final VoidCallback? onTap;
  final Widget? trailing;
  const _AccountRow({
    required this.icon,
    required this.title,
    required this.subtitle,
    this.onTap,
    this.trailing,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 15),
        child: Row(
          children: [
            SizedBox(
              width: 24,
              child: AppIcon(icon, color: scheme.onSurfaceVariant, size: 19),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: theme.textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    subtitle,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: scheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            trailing ?? const AppIcon(SwiftIcons.chevronRight, size: 19),
          ],
        ),
      ),
    );
  }
}

class _RowDivider extends StatelessWidget {
  const _RowDivider();
  @override
  Widget build(BuildContext context) => Divider(
    height: 1,
    indent: 52,
    endIndent: 16,
    color: Theme.of(context).colorScheme.outlineVariant,
  );
}
