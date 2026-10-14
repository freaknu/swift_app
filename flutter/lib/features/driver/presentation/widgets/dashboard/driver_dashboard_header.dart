import 'package:swift_app/core/notifications/notification_inbox_datasource.dart';
import 'package:swift_app/core/notifications/notification_inbox_page.dart';
import 'package:swift_app/features/driver/data/model/driver_profile_model.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:flutter/material.dart';

class DriverDashboardHeader extends StatelessWidget {
  final DriverProfileModel? profile;
  final DriverVehicleModel? vehicle;
  final bool showNotifications;
  final ValueChanged<InboxNotification> onOpenNotification;
  final VoidCallback onOpenAccount;

  const DriverDashboardHeader({
    super.key,
    required this.profile,
    required this.vehicle,
    required this.showNotifications,
    required this.onOpenNotification,
    required this.onOpenAccount,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final name = profile?.driverName.trim().isNotEmpty == true
        ? profile!.driverName.trim()
        : 'Driver';
    final initials = name
        .split(RegExp(r'\s+'))
        .where((part) => part.isNotEmpty)
        .take(2)
        .map((part) => part.characters.first.toUpperCase())
        .join();

    final height = SwiftAppBar.toolbarHeightFor(context, hasSubtitle: true);
    return SizedBox(
      height: height,
      child: SwiftAppBar(
        primary: false,
        automaticallyImplyLeading: false,
        toolbarHeight: height,
        title: Text(
          'Drive',
          style: theme.textTheme.titleLarge?.copyWith(
            fontWeight: FontWeight.w600,
            color: scheme.onSurface,
          ),
        ),
        subtitle: 'Hello, ${name.split(RegExp(r'\s+')).first}',
        actions: [
          if (showNotifications)
            NotificationBell(
              role: 'DRIVER',
              onOpenNotification: onOpenNotification,
            ),
          IconButton(
            tooltip: 'Open account',
            onPressed: onOpenAccount,
            icon: CircleAvatar(
              radius: 17,
              backgroundColor: scheme.primaryContainer,
              child: Text(
                initials.isEmpty ? 'D' : initials,
                style: theme.textTheme.labelLarge?.copyWith(
                  color: scheme.onPrimaryContainer,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Shown on the driver home page when the profile-completeness API
/// reports missing details. Tapping it opens the completion flow.
