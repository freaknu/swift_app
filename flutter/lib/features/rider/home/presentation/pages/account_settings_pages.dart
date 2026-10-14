import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/rider/home/data/datasource/rider_account_datasource.dart';
import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_event.dart';

RiderAccountDatasource riderAccountApi() => RiderAccountDatasource(
  sl<DioClient>().dio,
  sl<AuthLocalDatasource>(),
  sl<RiderApiEndpoints>(),
);
String _message(Object error) =>
    error.toString().replaceFirst('Bad state: ', '');

class NotificationPreferencesPage extends StatefulWidget {
  final RiderAccountDatasource? datasource;
  const NotificationPreferencesPage({super.key, this.datasource});
  @override
  State<NotificationPreferencesPage> createState() =>
      _NotificationPreferencesPageState();
}

class _NotificationPreferencesPageState
    extends State<NotificationPreferencesPage> {
  late final api = widget.datasource ?? riderAccountApi();
  Map<String, dynamic>? _preferences;
  String? _error;
  bool _saving = false;
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final result = await api.preferences();
      if (mounted) setState(() => _preferences = result);
    } catch (error) {
      if (mounted) setState(() => _error = _message(error));
    }
  }

  Future<void> _save() async {
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await api.savePreferences(_preferences!);
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Preferences saved')));
      }
    } catch (error) {
      if (mounted) setState(() => _error = _message(error));
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) => _SettingsShell(
    title: 'Notifications',
    children: [
      Text(
        'Choose how Swift notifies you.',
        style: Theme.of(context).textTheme.bodyMedium,
      ),
      const SizedBox(height: 24),
      if (_preferences == null && _error == null) const _LoadingSettings(),
      if (_preferences != null)
        Column(
          children: [
            _toggle(
              'Push notifications',
              'Ride updates and messages on your phone',
              'pushNotificationEnabled',
              SwiftIcons.bell,
            ),
            const SizedBox(height: 10),
            _toggle(
              'Email',
              'Updates sent to your registered email',
              'emailNotificationEnabled',
              SwiftIcons.mail,
            ),
            const SizedBox(height: 10),
            _toggle(
              'SMS',
              'Your SMS delivery preference',
              'smsNotificationEnabled',
              SwiftIcons.messageCircle,
            ),
          ],
        ),
      if (_error != null)
        _ErrorPanel(
          message: _error!,
          retry: _preferences == null ? _load : null,
        ),
      const SizedBox(height: 24),
      FilledButton(
        onPressed: _preferences == null || _saving ? null : _save,
        style: FilledButton.styleFrom(
          minimumSize: const Size.fromHeight(52),
          shape: const StadiumBorder(),
        ),
        child: Text(_saving ? 'Saving…' : 'Save preferences'),
      ),
    ],
  );
  Widget _toggle(String title, String subtitle, String key, SwiftIcons icon) {
    final scheme = Theme.of(context).colorScheme;
    return _AccountSettingsCard(
      child: LayoutBuilder(
        builder: (context, constraints) {
          final stacked =
              constraints.maxWidth < 250 ||
              MediaQuery.textScalerOf(context).scale(14) > 20;
          final toggle = Switch.adaptive(
            value: _preferences![key] == true,
            onChanged: _saving
                ? null
                : (value) => setState(() => _preferences![key] = value),
          );
          final description = Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _AccountCircleIcon(icon: icon),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: Theme.of(context).textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      subtitle,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: scheme.onSurfaceVariant,
                        height: 1.35,
                      ),
                    ),
                  ],
                ),
              ),
              if (!stacked) ...[const SizedBox(width: 8), toggle],
            ],
          );
          return Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            mainAxisSize: MainAxisSize.min,
            children: [
              description,
              if (stacked)
                Align(alignment: Alignment.centerRight, child: toggle),
            ],
          );
        },
      ),
    );
  }
}

class EditRiderProfilePage extends StatefulWidget {
  final UserReponse user;
  final RiderAccountDatasource? datasource;
  const EditRiderProfilePage({super.key, required this.user, this.datasource});
  @override
  State<EditRiderProfilePage> createState() => _EditRiderProfilePageState();
}

class _EditRiderProfilePageState extends State<EditRiderProfilePage> {
  final _form = GlobalKey<FormState>();
  late final _name = TextEditingController(text: widget.user.fullName);
  late final _phone = TextEditingController(text: widget.user.phoneNumber);
  bool _saving = false;
  String? _error;
  @override
  void dispose() {
    _name.dispose();
    _phone.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await (widget.datasource ?? riderAccountApi()).updateProfile({
        'id': widget.user.id,
        'authUserId': widget.user.authUserId,
        'fullName': _name.text.trim(),
        'email': widget.user.email,
        'phoneNumber': _phone.text.trim(),
        'profileImageUrl': widget.user.profileImageUrl,
      });
      if (mounted) {
        context.read<UserBloc>().add(const FetchUserDetails());
        Navigator.pop(context);
      }
    } catch (error) {
      if (mounted) setState(() => _error = _message(error));
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) => _SettingsShell(
    title: 'Edit profile',
    children: [
      Form(
        key: _form,
        child: Column(
          children: [
            TextFormField(
              controller: _name,
              decoration: const InputDecoration(labelText: 'Full name'),
              textCapitalization: TextCapitalization.words,
              validator: (value) => value == null || value.trim().length < 2
                  ? 'Enter your full name.'
                  : null,
            ),
            const SizedBox(height: 18),
            TextFormField(
              initialValue: widget.user.email ?? '',
              readOnly: true,
              decoration: const InputDecoration(
                labelText: 'Email',
                helperText: 'Your sign-in email',
              ),
            ),
            const SizedBox(height: 18),
            TextFormField(
              controller: _phone,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(labelText: 'Phone number'),
              validator: (value) =>
                  (value ?? '').replaceAll(RegExp(r'\D'), '').length < 10
                  ? 'Enter a valid phone number.'
                  : null,
            ),
          ],
        ),
      ),
      if (_error != null) _ErrorPanel(message: _error!),
      const SizedBox(height: 28),
      FilledButton(
        onPressed: _saving ? null : _save,
        style: FilledButton.styleFrom(
          minimumSize: const Size.fromHeight(52),
          shape: const StadiumBorder(),
        ),
        child: Text(_saving ? 'Saving…' : 'Save changes'),
      ),
    ],
  );
}

class EmergencyContactsPage extends StatefulWidget {
  final RiderAccountDatasource? datasource;
  const EmergencyContactsPage({super.key, this.datasource});
  @override
  State<EmergencyContactsPage> createState() => _EmergencyContactsPageState();
}

class _EmergencyContactsPageState extends State<EmergencyContactsPage> {
  late final api = widget.datasource ?? riderAccountApi();
  List<Map<String, dynamic>>? _contacts;
  String? _error;
  bool _busy = false;
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final items = await api.contacts();
      if (mounted) setState(() => _contacts = items);
    } catch (error) {
      if (mounted) setState(() => _error = _message(error));
    }
  }

  Future<void> _remove(Map<String, dynamic> contact) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Remove contact?'),
        content: Text(contact['contactPersonName']?.toString() ?? ''),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Keep'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Remove'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    setState(() => _busy = true);
    try {
      final items = await api.removeContact((contact['id'] as num).toInt());
      if (mounted) setState(() => _contacts = items);
    } catch (error) {
      if (mounted) setState(() => _error = _message(error));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _add() async {
    final result = await showDialog<Map<String, dynamic>>(
      context: context,
      builder: (_) => const _AddContactDialog(),
    );
    if (result == null || !mounted) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final items = await api.addContact(result);
      if (mounted) setState(() => _contacts = items);
    } catch (error) {
      if (mounted) setState(() => _error = _message(error));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => _SettingsShell(
    title: 'Emergency contacts',
    children: [
      Text(
        'Keep your trusted contacts in one place.',
        style: Theme.of(context).textTheme.bodyMedium,
      ),
      const SizedBox(height: 24),
      if (_contacts == null && _error == null) const _LoadingSettings(),
      if (_contacts?.isEmpty == true)
        const GlassSurface(
          padding: EdgeInsets.all(24),
          child: Text('Add someone you can reach during a trip.'),
        ),
      for (final contact in _contacts ?? <Map<String, dynamic>>[])
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: _AccountSettingsCard(
            child: Row(
              children: [
                const _AccountCircleIcon(icon: SwiftIcons.userRound),
                const SizedBox(width: 13),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        contact['contactPersonName']?.toString() ?? 'Contact',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        contact['contactType']?.toString() ?? '',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.labelSmall?.copyWith(
                          color: Theme.of(context).colorScheme.primary,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        contact['contactPersonNumber']?.toString() ?? '',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: Theme.of(context).colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton.filledTonal(
                  tooltip: 'Remove contact',
                  onPressed: _busy ? null : () => _remove(contact),
                  icon: const AppIcon(SwiftIcons.trash, size: 19),
                ),
              ],
            ),
          ),
        ),
      if (_error != null) _ErrorPanel(message: _error!, retry: _load),
      const SizedBox(height: 20),
      FilledButton.icon(
        onPressed: _busy ? null : _add,
        icon: const AppIcon(SwiftIcons.plus),
        style: FilledButton.styleFrom(
          minimumSize: const Size.fromHeight(52),
          shape: const StadiumBorder(),
        ),
        label: const Text('Add contact'),
      ),
    ],
  );
}

class _AddContactDialog extends StatefulWidget {
  const _AddContactDialog();
  @override
  State<_AddContactDialog> createState() => _AddContactDialogState();
}

class _AddContactDialogState extends State<_AddContactDialog> {
  final _form = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _phone = TextEditingController();
  final _relation = TextEditingController();
  @override
  void dispose() {
    _name.dispose();
    _phone.dispose();
    _relation.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: const Text('Add contact'),
    content: SingleChildScrollView(
      child: Form(
        key: _form,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextFormField(
              controller: _name,
              decoration: const InputDecoration(labelText: 'Name'),
              validator: (v) =>
                  (v ?? '').trim().isEmpty ? 'Enter a name.' : null,
            ),
            const SizedBox(height: 14),
            TextFormField(
              controller: _relation,
              decoration: const InputDecoration(labelText: 'Relationship'),
              validator: (v) =>
                  (v ?? '').trim().isEmpty ? 'Enter a relationship.' : null,
            ),
            const SizedBox(height: 14),
            TextFormField(
              controller: _phone,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(labelText: 'Phone number'),
              validator: (v) =>
                  (v ?? '').replaceAll(RegExp(r'\D'), '').length < 10
                  ? 'Enter a valid number.'
                  : null,
            ),
          ],
        ),
      ),
    ),
    actions: [
      TextButton(
        onPressed: () => Navigator.pop(context),
        child: const Text('Cancel'),
      ),
      FilledButton(
        onPressed: () {
          if (_form.currentState!.validate()) {
            Navigator.pop(context, {
              'contactPersonName': _name.text.trim(),
              'contactType': _relation.text.trim(),
              'contactPersonNumber': _phone.text.trim(),
            });
          }
        },
        child: const Text('Add'),
      ),
    ],
  );
}

class _SettingsShell extends StatelessWidget {
  final String title;
  final List<Widget> children;
  const _SettingsShell({required this.title, required this.children});
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: SwiftAppBar(
        toolbarHeight: SwiftAppBar.toolbarHeightFor(context),
        title: Text(title),
      ),
      body: SafeArea(
        top: false,
        child: Align(
          alignment: Alignment.topCenter,
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 600),
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 20, 16, 24),
              children: children,
            ),
          ),
        ),
      ),
    );
  }
}

class _LoadingSettings extends StatelessWidget {
  const _LoadingSettings();
  @override
  Widget build(BuildContext context) => const AppShimmer(
    child: Column(
      children: [
        ShimmerBlock(width: double.infinity, height: 76, radius: 18),
        SizedBox(height: 12),
        ShimmerBlock(width: double.infinity, height: 76, radius: 18),
        SizedBox(height: 12),
        ShimmerBlock(width: double.infinity, height: 76, radius: 18),
      ],
    ),
  );
}

class _ErrorPanel extends StatelessWidget {
  final String message;
  final VoidCallback? retry;
  const _ErrorPanel({required this.message, this.retry});
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 16),
    child: Column(
      children: [
        Text(
          message,
          style: TextStyle(color: Theme.of(context).colorScheme.error),
        ),
        if (retry != null)
          TextButton(onPressed: retry, child: const Text('Try again')),
      ],
    ),
  );
}

/// Shared, compact account card styling for settings and contacts.
class _AccountSettingsCard extends StatelessWidget {
  const _AccountSettingsCard({required this.child});
  final Widget child;
  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
      decoration: BoxDecoration(
        color: scheme.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: scheme.outlineVariant, width: 0.8),
      ),
      child: child,
    );
  }
}

class _AccountCircleIcon extends StatelessWidget {
  const _AccountCircleIcon({required this.icon});
  final SwiftIcons icon;
  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      width: 36,
      height: 36,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: scheme.surfaceContainerLow,
      ),
      child: AppIcon(icon, size: 18, color: scheme.onSurfaceVariant),
    );
  }
}
