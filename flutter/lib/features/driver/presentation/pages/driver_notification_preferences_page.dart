import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:flutter/material.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/driver/data/model/driver_notification_preferences_model.dart';

class DriverNotificationPreferencesPage extends StatefulWidget {
  final DriverRemoteDatasource datasource;

  const DriverNotificationPreferencesPage({
    super.key,
    required this.datasource,
  });

  @override
  State<DriverNotificationPreferencesPage> createState() =>
      _DriverNotificationPreferencesPageState();
}

class _DriverNotificationPreferencesPageState
    extends State<DriverNotificationPreferencesPage> {
  DriverNotificationPreferencesModel? _preferences;
  bool _loading = true;
  bool _saving = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    final response = await widget.datasource.getNotificationPreferences();
    if (!mounted) return;
    setState(() {
      _loading = false;
      if (response.success && response.data != null) {
        _preferences = response.data;
      } else {
        _error = response.message;
      }
    });
  }

  Future<void> _save() async {
    final current = _preferences;
    if (current == null || _saving) return;
    setState(() => _saving = true);
    final response = await widget.datasource.updateNotificationPreferences(
      current,
    );
    if (!mounted) return;
    setState(() {
      _saving = false;
      if (response.success && response.data != null) {
        _preferences = response.data;
      }
    });
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          response.success
              ? 'Notification preferences saved'
              : response.message,
        ),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final preferences = _preferences;
    return Scaffold(
      appBar: SwiftAppBar(
        toolbarHeight: SwiftAppBar.toolbarHeightFor(context),
        title: const Text('Notifications'),
      ),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 700),
            child: _loading && preferences == null
                ? const SingleChildScrollView(
                    padding: EdgeInsets.all(16),
                    child: AppShimmer(
                      child: Column(
                        children: [
                          ShimmerBlock(width: double.infinity, height: 220),
                          SizedBox(height: 16),
                          ShimmerBlock(width: double.infinity, height: 180),
                        ],
                      ),
                    ),
                  )
                : preferences == null
                ? SingleChildScrollView(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const AppIcon(SwiftIcons.wifiOff, size: 24),
                        const SizedBox(height: 12),
                        Text(
                          _error ?? 'Could not load preferences.',
                          textAlign: TextAlign.center,
                        ),
                        TextButton(
                          onPressed: _load,
                          child: const Text('Retry'),
                        ),
                      ],
                    ),
                  )
                : ListView(
                    keyboardDismissBehavior:
                        ScrollViewKeyboardDismissBehavior.onDrag,
                    padding: const EdgeInsets.fromLTRB(16, 20, 16, 24),
                    children: [
                      Text(
                        'Delivery',
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 12),
                      AppCard(
                        padding: EdgeInsets.zero,
                        child: Column(
                          children: [
                            _PreferenceSwitch(
                              icon: SwiftIcons.bell,
                              title: 'Push notifications',
                              subtitle: 'Get updates on this device',
                              value: preferences.pushEnabled,
                              onChanged: (value) => setState(
                                () => _preferences = preferences.copyWith(
                                  pushEnabled: value,
                                ),
                              ),
                            ),
                            const _PreferenceDivider(),
                            _PreferenceSwitch(
                              icon: SwiftIcons.mail,
                              title: 'Email',
                              subtitle: 'Receive updates by email',
                              value: preferences.emailEnabled,
                              onChanged: (value) => setState(
                                () => _preferences = preferences.copyWith(
                                  emailEnabled: value,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 24),
                      Text(
                        'Updates',
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 12),
                      AppCard(
                        padding: EdgeInsets.zero,
                        child: Column(
                          children: [
                            _PreferenceSwitch(
                              icon: SwiftIcons.car,
                              title: 'Ride requests',
                              subtitle: 'Alerts when a ride is offered to you',
                              value: preferences.rideAlertsEnabled,
                              onChanged: (value) => setState(
                                () => _preferences = preferences.copyWith(
                                  rideAlertsEnabled: value,
                                ),
                              ),
                            ),
                            Padding(
                              padding: const EdgeInsets.fromLTRB(16, 6, 16, 16),
                              child: Row(
                                children: [
                                  AppIcon(
                                    SwiftIcons.info,
                                    size: 18,
                                    color: scheme.onSurfaceVariant,
                                  ),
                                  const SizedBox(width: 8),
                                  Expanded(
                                    child: Text(
                                      'Ride request alerts also require phone notification permission.',
                                      style: theme.textTheme.bodySmall
                                          ?.copyWith(
                                            color: scheme.onSurfaceVariant,
                                          ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
          ),
        ),
      ),
      bottomNavigationBar: preferences == null
          ? null
          : SafeArea(
              minimum: const EdgeInsets.fromLTRB(16, 8, 16, 12),
              child: Center(
                heightFactor: 1,
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 700),
                  child: FilledButton(
                    onPressed: _saving ? null : _save,
                    style: FilledButton.styleFrom(
                      minimumSize: const Size.fromHeight(52),
                    ),
                    child: Text(_saving ? 'Saving…' : 'Save preferences'),
                  ),
                ),
              ),
            ),
    );
  }
}

class _PreferenceSwitch extends StatelessWidget {
  final SwiftIcons icon;
  final String title;
  final String subtitle;
  final bool value;
  final ValueChanged<bool> onChanged;
  const _PreferenceSwitch({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.value,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return SwitchListTile.adaptive(
      contentPadding: const EdgeInsets.fromLTRB(16, 8, 12, 8),
      secondary: AppIcon(icon, color: scheme.onSurfaceVariant, size: 19),
      title: Text(
        title,
        style: Theme.of(
          context,
        ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w700),
      ),
      subtitle: Text(subtitle, style: Theme.of(context).textTheme.bodySmall),
      value: value,
      onChanged: onChanged,
    );
  }
}

class _PreferenceDivider extends StatelessWidget {
  const _PreferenceDivider();
  @override
  Widget build(BuildContext context) => Divider(
    height: 1,
    indent: 60,
    color: Theme.of(context).colorScheme.outlineVariant,
  );
}
