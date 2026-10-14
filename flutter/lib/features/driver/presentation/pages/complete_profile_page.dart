import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/constants/api_response/api_response_format.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/app_card.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/driver/data/model/driver_profile_model.dart';
import 'package:swift_app/features/driver/data/model/driver_document_model.dart';
import 'package:swift_app/features/driver/data/model/profile_completeness_model.dart';
import 'package:swift_app/features/driver/data/model/vehicle_type_option_model.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';

/// Tabbed profile-completion flow shown from the driver home page when
/// GET /api/v1/driver/profile/completeness reports the profile incomplete.
///
/// Tabs: Personal, Vehicle, Documents. Pops with `true` when the profile
/// became complete so the home page can refresh and enable going online.
class CompleteProfilePage extends StatefulWidget {
  final DriverRemoteDatasource datasource;
  final DriverProfileModel? profile;
  final ProfileCompletenessModel? completeness;
  final int initialTab;

  const CompleteProfilePage({
    super.key,
    required this.datasource,
    this.profile,
    this.completeness,
    this.initialTab = 0,
  });

  @override
  State<CompleteProfilePage> createState() => _CompleteProfilePageState();
}

class _CompleteProfilePageState extends State<CompleteProfilePage> {
  ProfileCompletenessModel? _completeness;
  bool _finishing = false;

  @override
  void initState() {
    super.initState();
    _completeness = widget.completeness;
  }

  Future<void> _refreshCompleteness() async {
    final response = await widget.datasource.getProfileCompleteness();
    if (!mounted) return;
    if (response.success && response.data != null) {
      setState(() => _completeness = response.data);
    }
  }

  void _showSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), behavior: SnackBarBehavior.floating),
    );
  }

  Future<void> _finish() async {
    if (_finishing) return;
    setState(() => _finishing = true);
    var didPop = false;
    try {
      await _refreshCompleteness();
      if (!mounted) return;
      if ((_completeness?.complete ?? false)) {
        didPop = true;
        Navigator.of(context).pop(true);
      } else {
        _showSnackBar(
          'Still missing: ${(_completeness?.missing ?? []).join(', ')}',
        );
      }
    } finally {
      if (mounted && !didPop) setState(() => _finishing = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final completeness = _completeness;
    final completedSections = [
      completeness?.personal.complete ?? false,
      completeness?.vehicle.complete ?? false,
      completeness?.documents.complete ?? false,
    ].where((complete) => complete).length;
    // Read this before Scaffold removes keyboard insets from its body MediaQuery.
    final compactHeight =
        MediaQuery.sizeOf(context).height -
            MediaQuery.viewInsetsOf(context).bottom <
        560;

    return DefaultTabController(
      length: 3,
      initialIndex: widget.initialTab,
      child: Scaffold(
        backgroundColor: Colors.transparent,
        body: AppGlassBackground(
          child: SafeArea(
            bottom: false,
            child: Align(
              alignment: Alignment.topCenter,
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 760),
                child: Column(
                  children: [
                    _ProfileHeader(
                      completedSections: completedSections,
                      complete: completeness?.complete ?? false,
                      compactHeight: compactHeight,
                    ),
                    _ProfileTabBar(completeness: completeness),
                    const SizedBox(height: 12),
                    Expanded(
                      child: _ProfileTabView(
                        children: [
                          _PersonalTab(
                            datasource: widget.datasource,
                            profile: widget.profile,
                            missing: completeness?.personal.missing ?? const [],
                            onSaved: _refreshCompleteness,
                            onMessage: _showSnackBar,
                          ),
                          _VehicleTab(
                            datasource: widget.datasource,
                            onSaved: _refreshCompleteness,
                            onMessage: _showSnackBar,
                          ),
                          _DocumentsTab(
                            datasource: widget.datasource,
                            missing:
                                completeness?.documents.missing ?? const [],
                            onSaved: _refreshCompleteness,
                            onMessage: _showSnackBar,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
        bottomNavigationBar: GlassNavigationBar(
          margin: const EdgeInsets.fromLTRB(12, 8, 12, 8),
          padding: const EdgeInsets.all(10),
          semanticLabel: 'Complete profile action',
          child: GlassButton(
            width: double.infinity,
            filled: true,
            onPressed: _finishing ? null : _finish,
            semanticLabel: _finishing
                ? 'Checking profile completeness'
                : 'Done, check profile completeness',
            child: AnimatedSwitcher(
              duration: MediaQuery.disableAnimationsOf(context)
                  ? Duration.zero
                  : const Duration(milliseconds: 180),
              child: _finishing
                  ? const _ActionContent(
                      key: ValueKey('checking-profile'),
                      label: 'Checking…',
                      icon: SwiftIcons.refreshCw,
                      loading: true,
                    )
                  : const _ActionContent(
                      key: ValueKey('done-profile'),
                      label: 'Done',
                      icon: SwiftIcons.check,
                    ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Retain form drafts without leaving an offscreen field connected to the IME.
/// The controller listener covers tab taps, swipes and programmatic changes.
class _ProfileTabView extends StatefulWidget {
  const _ProfileTabView({required this.children});

  final List<Widget> children;

  @override
  State<_ProfileTabView> createState() => _ProfileTabViewState();
}

class _ProfileTabViewState extends State<_ProfileTabView> {
  TabController? _controller;
  int _index = 0;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final controller = DefaultTabController.of(context);
    if (identical(controller, _controller)) return;
    _controller?.removeListener(_handleTabChange);
    _controller = controller;
    _index = controller.index;
    controller.addListener(_handleTabChange);
  }

  void _handleTabChange() {
    final index = _controller!.index;
    if (index == _index) return;
    FocusManager.instance.primaryFocus?.unfocus();
    setState(() => _index = index);
  }

  @override
  void dispose() {
    _controller?.removeListener(_handleTabChange);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => TabBarView(
    controller: _controller,
    children: [
      for (var index = 0; index < widget.children.length; index++)
        ExcludeFocus(excluding: index != _index, child: widget.children[index]),
    ],
  );
}

class _ProfileHeader extends StatelessWidget {
  final int completedSections;
  final bool complete;
  final bool compactHeight;

  const _ProfileHeader({
    required this.completedSections,
    required this.complete,
    required this.compactHeight,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final compact =
        compactHeight ||
        MediaQuery.sizeOf(context).width < 380 ||
        MediaQuery.textScalerOf(context).scale(16) > 20;
    final progress = completedSections / 3;
    final summary = complete
        ? 'Everything is ready for your next ride.'
        : completedSections == 0
        ? 'Add a few details to start driving with confidence.'
        : 'You are nearly there — finish the remaining sections.';

    return Padding(
      padding: EdgeInsets.fromLTRB(12, compact ? 8 : 14, 12, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          SwiftAppBar(
            toolbarHeight: SwiftAppBar.toolbarHeightFor(
              context,
              hasSubtitle: !compact,
            ),
            primary: false,
            automaticallyImplyLeading: false,
            leading: SwiftAppBarAction(
              icon: SwiftIcons.chevronLeft,
              onPressed: () => Navigator.of(context).maybePop(),
              tooltip: 'Back',
            ),
            title: Text(
              complete ? 'Your profile' : 'Complete your profile',
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style:
                  (compact
                          ? theme.textTheme.titleLarge
                          : theme.textTheme.headlineSmall)
                      ?.copyWith(
                        fontWeight: FontWeight.w600,
                        letterSpacing: -0.4,
                        color: scheme.onSurface,
                      ),
            ),
            subtitle: compact ? null : summary,
          ),
          SizedBox(height: compact ? 8 : 18),
          if (!compact) ...[
            Row(
              children: [
                Expanded(
                  child: Text(
                    'Profile progress',
                    style: theme.textTheme.labelLarge?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
                Text(
                  '$completedSections of 3 sections',
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: scheme.primary,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 9),
          ],
          Semantics(
            label: 'Profile completion',
            value: '$completedSections of 3 sections complete',
            child: ClipRRect(
              borderRadius: BorderRadius.circular(999),
              child: LinearProgressIndicator(
                value: progress,
                minHeight: 7,
                backgroundColor: scheme.onSurface.withValues(alpha: 0.08),
                color: scheme.primary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ProfileTabBar extends StatelessWidget {
  final ProfileCompletenessModel? completeness;

  const _ProfileTabBar({required this.completeness});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return AppCard(
      margin: const EdgeInsets.fromLTRB(12, 10, 12, 0),
      padding: const EdgeInsets.all(4),
      opacity: 1,
      semanticLabel: 'Profile sections',
      child: TabBar(
        isScrollable: true,
        labelPadding: const EdgeInsets.symmetric(horizontal: 14),
        indicatorSize: TabBarIndicatorSize.tab,
        indicator: BoxDecoration(
          color: scheme.primary.withValues(alpha: 0.14),
          borderRadius: AppRadii.button,
          border: Border.all(color: scheme.primary.withValues(alpha: 0.3)),
        ),
        dividerColor: Colors.transparent,
        labelColor: scheme.primary,
        unselectedLabelColor: theme.colorScheme.onSurfaceVariant,
        labelStyle: theme.textTheme.labelLarge?.copyWith(
          fontWeight: FontWeight.w700,
        ),
        unselectedLabelStyle: theme.textTheme.labelLarge?.copyWith(
          fontWeight: FontWeight.w600,
        ),
        tabs: [
          _CompletionTab(
            label: 'Personal',
            done: completeness?.personal.complete ?? false,
          ),
          _CompletionTab(
            label: 'Vehicle',
            done: completeness?.vehicle.complete ?? false,
          ),
          _CompletionTab(
            label: 'Documents',
            done: completeness?.documents.complete ?? false,
          ),
        ],
      ),
    );
  }
}

class _CompletionTab extends StatelessWidget {
  final String label;
  final bool done;

  const _CompletionTab({required this.label, required this.done});

  @override
  Widget build(BuildContext context) {
    return Tab(
      child: Semantics(
        container: true,
        label: '$label, ${done ? 'complete' : 'incomplete'}',
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(label, softWrap: false),
            const SizedBox(width: 6),
            ExcludeSemantics(
              child: AppIcon(
                done ? SwiftIcons.circleCheck : SwiftIcons.circle,
                size: 16,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ActionContent extends StatelessWidget {
  final String label;
  final SwiftIcons icon;
  final bool loading;

  const _ActionContent({
    super.key,
    required this.label,
    required this.icon,
    this.loading = false,
  });

  @override
  Widget build(BuildContext context) {
    final foreground = Theme.of(context).colorScheme.onPrimary;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        if (loading)
          SizedBox(
            width: 18,
            height: 18,
            child: CircularProgressIndicator(strokeWidth: 2, color: foreground),
          )
        else
          AppIcon(icon, size: 19, color: foreground),
        const SizedBox(width: 8),
        Text(
          label,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: TextStyle(color: foreground, fontWeight: FontWeight.w700),
        ),
      ],
    );
  }
}

class _SectionHeading extends StatelessWidget {
  final SwiftIcons icon;
  final String title;
  final String subtitle;

  const _SectionHeading({
    required this.icon,
    required this.title,
    required this.subtitle,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 44,
          height: 44,
          decoration: BoxDecoration(
            color: scheme.primary.withValues(alpha: 0.12),
            borderRadius: BorderRadius.circular(15),
          ),
          child: AppIcon(icon, color: scheme.primary, size: 20),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Semantics(
                header: true,
                child: Text(
                  title,
                  style: theme.textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.w600,
                    letterSpacing: -0.2,
                  ),
                ),
              ),
              const SizedBox(height: 3),
              Text(subtitle, style: theme.textTheme.bodySmall),
            ],
          ),
        ),
      ],
    );
  }
}

InputDecoration _profileInputDecoration(
  BuildContext context, {
  required String label,
  SwiftIcons? icon,
  String? hintText,
  Widget? suffixIcon,
}) {
  final theme = Theme.of(context);
  final scheme = theme.colorScheme;
  final radius = BorderRadius.circular(16);
  final fieldColor = scheme.surface.withValues(
    alpha: theme.brightness == Brightness.dark ? 0.42 : 0.58,
  );

  OutlineInputBorder outline(Color color, [double width = 1]) {
    return OutlineInputBorder(
      borderRadius: radius,
      borderSide: BorderSide(color: color, width: width),
    );
  }

  return InputDecoration(
    labelText: label,
    hintText: hintText,
    prefixIcon: icon == null ? null : AppIcon(icon, size: 19),
    suffixIcon: suffixIcon,
    filled: true,
    fillColor: fieldColor,
    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
    border: outline(Colors.transparent, 0),
    enabledBorder: outline(scheme.outlineVariant.withValues(alpha: 0.42)),
    focusedBorder: outline(scheme.primary, 1.5),
    errorBorder: outline(scheme.error),
    focusedErrorBorder: outline(scheme.error, 1.5),
  );
}

// ---------------------------------------------------------------- Personal

class _PersonalTab extends StatefulWidget {
  final DriverRemoteDatasource datasource;
  final DriverProfileModel? profile;
  final List<String> missing;
  final Future<void> Function() onSaved;
  final void Function(String) onMessage;

  const _PersonalTab({
    required this.datasource,
    required this.profile,
    required this.missing,
    required this.onSaved,
    required this.onMessage,
  });

  @override
  State<_PersonalTab> createState() => _PersonalTabState();
}

class _PersonalTabState extends State<_PersonalTab>
    with AutomaticKeepAliveClientMixin<_PersonalTab> {
  late final TextEditingController _name;
  late final TextEditingController _email;
  late final TextEditingController _phone;
  String? _gender;
  DateTime? _dob;
  bool _saving = false;

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    _name = TextEditingController(text: widget.profile?.driverName ?? '');
    _email = TextEditingController(text: widget.profile?.email ?? '');
    _phone = TextEditingController(text: widget.profile?.phoneNumber ?? '');
    _gender = widget.profile?.gender;
    _dob = DateTime.tryParse(widget.profile?.dateOfBirth ?? '');
  }

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    _phone.dispose();
    super.dispose();
  }

  String _label(String field, String fallback) =>
      widget.missing.contains(field) ? '$fallback *' : fallback;

  Future<void> _save() async {
    if (_saving) return;
    if (_name.text.trim().isEmpty ||
        _email.text.trim().isEmpty ||
        _phone.text.trim().isEmpty) {
      widget.onMessage('Please fill name, email and phone number.');
      return;
    }
    setState(() => _saving = true);
    final data = <String, dynamic>{
      'driverName': _name.text.trim(),
      'email': _email.text.trim(),
      'phoneNumber': _phone.text.trim(),
    };
    if (_gender != null) data['gender'] = _gender;
    if (_dob != null) {
      data['dateOfBirth'] =
          '${_dob!.year.toString().padLeft(4, '0')}-'
          '${_dob!.month.toString().padLeft(2, '0')}-'
          '${_dob!.day.toString().padLeft(2, '0')}';
    }
    try {
      final ApiResponseFormat<Map<String, dynamic>> response = await widget
          .datasource
          .updateDriverProfile(data);
      if (!mounted) return;
      widget.onMessage(
        response.success ? 'Personal details saved.' : response.message,
      );
      if (response.success) await widget.onSaved();
    } catch (_) {
      if (mounted) {
        widget.onMessage('Could not save personal details. Please retry.');
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final theme = Theme.of(context);
    return ListView(
      keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
      physics: const BouncingScrollPhysics(
        parent: AlwaysScrollableScrollPhysics(),
      ),
      padding: const EdgeInsets.fromLTRB(12, 4, 12, 28),
      children: [
        AppCard(
          margin: EdgeInsets.zero,
          padding: const EdgeInsets.fromLTRB(16, 18, 16, 16),
          opacity: 0.84,
          semanticLabel: 'Personal details form',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _SectionHeading(
                icon: SwiftIcons.userRound,
                title: 'Personal details',
                subtitle: 'Fields marked * are still missing.',
              ),
              const SizedBox(height: 22),
              TextField(
                controller: _name,
                decoration: _profileInputDecoration(
                  context,
                  label: _label('driverName', 'Full name'),
                  icon: SwiftIcons.userRound,
                ),
                textCapitalization: TextCapitalization.words,
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _email,
                decoration: _profileInputDecoration(
                  context,
                  label: _label('email', 'Email'),
                  icon: SwiftIcons.mail,
                ),
                keyboardType: TextInputType.emailAddress,
              ),
              const SizedBox(height: 16),
              TextField(
                controller: _phone,
                decoration: _profileInputDecoration(
                  context,
                  label: _label('phoneNumber', 'Phone number'),
                  icon: SwiftIcons.phone,
                ),
                keyboardType: TextInputType.phone,
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<String>(
                icon: const AppIcon(SwiftIcons.chevronDown, size: 18),
                initialValue: _gender,
                decoration: _profileInputDecoration(
                  context,
                  label: _label('gender', 'Gender'),
                  icon: SwiftIcons.usersRound,
                ),
                items: const [
                  DropdownMenuItem(value: 'MALE', child: Text('Male')),
                  DropdownMenuItem(value: 'FEMALE', child: Text('Female')),
                ],
                onChanged: (value) => setState(() => _gender = value),
              ),
              const SizedBox(height: 16),
              Semantics(
                button: true,
                label:
                    'Date of birth. ${_dob == null ? 'Not selected' : '${_dob!.day}/${_dob!.month}/${_dob!.year}'}. Double tap to choose a date.',
                child: Material(
                  color: Colors.transparent,
                  child: InkWell(
                    borderRadius: BorderRadius.circular(16),
                    onTap: () async {
                      final now = DateTime.now();
                      final firstDate = DateTime(now.year - 80);
                      final lastDate = DateTime(now.year - 18);
                      final preferredDate = _dob ?? DateTime(now.year - 21);
                      final picked = await showDatePicker(
                        context: context,
                        initialDate:
                            preferredDate.isBefore(firstDate) ||
                                preferredDate.isAfter(lastDate)
                            ? DateTime(now.year - 21)
                            : preferredDate,
                        firstDate: firstDate,
                        lastDate: lastDate,
                      );
                      if (picked != null) setState(() => _dob = picked);
                    },
                    child: InputDecorator(
                      decoration: _profileInputDecoration(
                        context,
                        label: _label('dateOfBirth', 'Date of birth'),
                        icon: SwiftIcons.cake,
                        suffixIcon: const AppIcon(
                          SwiftIcons.calendarDays,
                          size: 19,
                        ),
                      ),
                      child: Text(
                        _dob == null
                            ? 'Select date'
                            : '${_dob!.day}/${_dob!.month}/${_dob!.year}',
                        style: theme.textTheme.bodyLarge?.copyWith(
                          color: _dob == null
                              ? theme.colorScheme.onSurfaceVariant
                              : theme.colorScheme.onSurface,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 24),
              GlassButton(
                width: double.infinity,
                filled: true,
                onPressed: _saving ? null : _save,
                semanticLabel: 'Save personal details',
                child: _saving
                    ? const _ActionContent(
                        label: 'Saving details',
                        icon: SwiftIcons.refreshCw,
                        loading: true,
                      )
                    : const _ActionContent(
                        label: 'Save details',
                        icon: SwiftIcons.check,
                      ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

// ----------------------------------------------------------------- Vehicle

class _VehicleTab extends StatefulWidget {
  final DriverRemoteDatasource datasource;
  final Future<void> Function() onSaved;
  final void Function(String) onMessage;

  const _VehicleTab({
    required this.datasource,
    required this.onSaved,
    required this.onMessage,
  });

  @override
  State<_VehicleTab> createState() => _VehicleTabState();
}

class _VehicleTabState extends State<_VehicleTab>
    with AutomaticKeepAliveClientMixin<_VehicleTab> {
  final _brand = TextEditingController();
  final _model = TextEditingController();
  final _registration = TextEditingController();
  final _color = TextEditingController();
  final _year = TextEditingController();

  List<VehicleTypeOption> _vehicleTypes = const [];
  DriverVehicleModel? _savedVehicle;
  int? _vehicleId;
  String? _selectedVehicleType;
  String? _loadError;
  String? _saveError;
  bool _loading = true;
  bool _saving = false;

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _brand.dispose();
    _model.dispose();
    _registration.dispose();
    _color.dispose();
    _year.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _loadError = null;
    });
    final typesFuture = widget.datasource.getVehicleTypes();
    final vehiclesFuture = widget.datasource.getVehicles();
    final typesResponse = await typesFuture;
    final vehiclesResponse = await vehiclesFuture;
    if (!mounted) return;
    if (!typesResponse.success || !vehiclesResponse.success) {
      setState(() {
        _loading = false;
        _loadError = !typesResponse.success
            ? typesResponse.message
            : vehiclesResponse.message;
      });
      return;
    }

    final vehicles = vehiclesResponse.data ?? const <DriverVehicleModel>[];
    final saved = vehicles.isEmpty
        ? null
        : vehicles.firstWhere(
            (vehicle) => vehicle.active,
            orElse: () => vehicles.first,
          );
    setState(() {
      _vehicleTypes = typesResponse.data ?? const [];
      _savedVehicle = saved;
      _vehicleId = saved?.id;
      _selectedVehicleType = saved?.vehicleType;
      _brand.text = saved?.brand ?? '';
      _model.text = saved?.model ?? '';
      _registration.text = saved?.registrationNumber ?? '';
      _color.text = saved?.color ?? '';
      _year.text = saved?.year?.toString() ?? '';
      _loading = false;
      _loadError = null;
    });
  }

  Future<void> _save() async {
    if (_saving || _loading || _loadError != null) return;
    final type = _selectedVehicleType;
    if (type == null || !_vehicleTypes.any((option) => option.code == type)) {
      setState(() => _saveError = 'Choose a vehicle type from the list.');
      return;
    }
    if (_brand.text.trim().isEmpty ||
        _model.text.trim().isEmpty ||
        _registration.text.trim().isEmpty) {
      setState(
        () => _saveError = 'Enter brand, model and registration number.',
      );
      return;
    }
    final year = int.tryParse(_year.text.trim());
    if (year == null || year < 1980 || year > DateTime.now().year + 1) {
      setState(() => _saveError = 'Enter a valid vehicle year.');
      return;
    }

    setState(() {
      _saving = true;
      _saveError = null;
    });
    final data = <String, dynamic>{
      'vehicleType': type,
      'brand': _brand.text.trim(),
      'model': _model.text.trim(),
      'registrationNumber': _registration.text.trim(),
      'year': year,
      'color': _color.text.trim(),
    };
    try {
      final response = _vehicleId == null
          ? await widget.datasource.addVehicle(data)
          : await widget.datasource.updateVehicle(_vehicleId!, data);
      if (!mounted) return;
      if (!response.success || response.data?.id == null) {
        setState(
          () => _saveError = response.success
              ? 'Vehicle saved, but its ID was not returned. Reload this tab before editing again.'
              : response.message,
        );
        return;
      }
      setState(() {
        _vehicleId = response.data!.id;
        _savedVehicle = response.data;
      });
      await widget.onSaved();
      if (mounted) widget.onMessage('Vehicle saved.');
    } catch (_) {
      if (mounted) {
        setState(() => _saveError = 'Could not save vehicle. Please retry.');
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final theme = Theme.of(context);
    final availableTypes = _vehicleTypes;
    final selectedType =
        availableTypes.any((option) => option.code == _selectedVehicleType)
        ? _selectedVehicleType
        : null;
    final textScale = MediaQuery.textScalerOf(context).scale(1.0);
    final stackSmallFields =
        MediaQuery.of(context).size.width < 430 || textScale >= 1.25;

    return ListView(
      keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
      physics: const BouncingScrollPhysics(
        parent: AlwaysScrollableScrollPhysics(),
      ),
      padding: const EdgeInsets.fromLTRB(12, 4, 12, 28),
      children: [
        if (_savedVehicle != null && !_loading && _loadError == null) ...[
          AppCard(
            margin: EdgeInsets.zero,
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Container(
                  width: 72,
                  height: 72,
                  decoration: BoxDecoration(
                    color: theme.colorScheme.primary.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(18),
                  ),
                  child: AppIcon(
                    SwiftIcons.car,
                    color: theme.colorScheme.primary,
                    size: 20,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        _vehicleTypes
                                .where(
                                  (option) =>
                                      option.code == _savedVehicle!.vehicleType,
                                )
                                .map((option) => option.name)
                                .firstOrNull ??
                            _savedVehicle!.vehicleType,
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      Text(
                        _savedVehicle!.displayName,
                        style: theme.textTheme.bodyMedium,
                      ),
                      if (_savedVehicle!.registrationNumber.isNotEmpty)
                        Text(
                          _savedVehicle!.registrationNumber,
                          style: theme.textTheme.bodySmall,
                        ),
                      if (_savedVehicle!.verified != null) ...[
                        const SizedBox(height: 6),
                        Text(
                          _savedVehicle!.verified!
                              ? 'Verified'
                              : 'Review pending',
                          style: theme.textTheme.labelMedium?.copyWith(
                            color: _savedVehicle!.verified!
                                ? theme.colorScheme.primary
                                : theme.colorScheme.onSurfaceVariant,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
        ],
        AppCard(
          margin: EdgeInsets.zero,
          padding: const EdgeInsets.fromLTRB(16, 18, 16, 16),
          opacity: 0.84,
          semanticLabel: 'Vehicle details form',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _SectionHeading(
                icon: SwiftIcons.car,
                title: 'Vehicle details',
                subtitle: _vehicleId == null
                    ? 'Add the vehicle you will drive.'
                    : 'Update your saved vehicle.',
              ),
              const SizedBox(height: 22),
              if (_loading)
                Semantics(
                  label: 'Loading vehicle details',
                  child: AppShimmer(
                    child: const Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        ShimmerBlock(width: 150, height: 16),
                        SizedBox(height: 16),
                        ShimmerBlock(width: double.infinity, height: 54),
                        SizedBox(height: 16),
                        ShimmerBlock(width: double.infinity, height: 54),
                      ],
                    ),
                  ),
                )
              else if (_loadError != null)
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _loadError!,
                      style: TextStyle(color: theme.colorScheme.error),
                    ),
                    const SizedBox(height: 8),
                    OutlinedButton.icon(
                      onPressed: _load,
                      icon: const AppIcon(SwiftIcons.refreshCw, size: 19),
                      label: const Text('Retry loading vehicle'),
                    ),
                  ],
                )
              else ...[
                DropdownButtonFormField<String>(
                  icon: const AppIcon(SwiftIcons.chevronDown, size: 18),
                  key: ValueKey('vehicle-type-${selectedType ?? 'none'}'),
                  initialValue: selectedType,
                  isExpanded: true,
                  decoration: _profileInputDecoration(
                    context,
                    label: 'Vehicle type *',
                    icon: SwiftIcons.car,
                  ),
                  hint: const Text('Select vehicle type'),
                  items: availableTypes
                      .map(
                        (option) => DropdownMenuItem(
                          value: option.code,
                          child: Text(option.name),
                        ),
                      )
                      .toList(),
                  onChanged: _saving
                      ? null
                      : (value) => setState(() => _selectedVehicleType = value),
                ),
                if (availableTypes.isEmpty ||
                    (_selectedVehicleType != null && selectedType == null)) ...[
                  const SizedBox(height: 8),
                  Text(
                    availableTypes.isEmpty
                        ? 'No vehicle types are available. Retry loading.'
                        : 'The saved type is no longer available. Choose another type.',
                    style: TextStyle(color: theme.colorScheme.error),
                  ),
                  TextButton.icon(
                    onPressed: _load,
                    icon: const AppIcon(SwiftIcons.refreshCw, size: 19),
                    label: const Text('Reload types'),
                  ),
                ],
                const SizedBox(height: 16),
                TextField(
                  controller: _brand,
                  decoration: _profileInputDecoration(
                    context,
                    label: 'Brand *',
                    icon: SwiftIcons.tag,
                  ),
                  textCapitalization: TextCapitalization.words,
                ),
                const SizedBox(height: 16),
                TextField(
                  controller: _model,
                  decoration: _profileInputDecoration(
                    context,
                    label: 'Model *',
                    icon: SwiftIcons.car,
                  ),
                  textCapitalization: TextCapitalization.words,
                ),
                const SizedBox(height: 16),
                TextField(
                  controller: _registration,
                  decoration: _profileInputDecoration(
                    context,
                    label: 'Registration number *',
                    icon: SwiftIcons.ticket,
                  ),
                  textCapitalization: TextCapitalization.characters,
                ),
                const SizedBox(height: 16),
                if (stackSmallFields) ...[
                  TextField(
                    controller: _color,
                    decoration: _profileInputDecoration(
                      context,
                      label: 'Color',
                      icon: SwiftIcons.palette,
                    ),
                    textCapitalization: TextCapitalization.words,
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _year,
                    decoration: _profileInputDecoration(
                      context,
                      label: 'Year *',
                      icon: SwiftIcons.calendarDays,
                    ),
                    keyboardType: TextInputType.number,
                  ),
                ] else
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: _color,
                          decoration: _profileInputDecoration(
                            context,
                            label: 'Color',
                            icon: SwiftIcons.palette,
                          ),
                          textCapitalization: TextCapitalization.words,
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: _year,
                          decoration: _profileInputDecoration(
                            context,
                            label: 'Year *',
                            icon: SwiftIcons.calendarDays,
                          ),
                          keyboardType: TextInputType.number,
                        ),
                      ),
                    ],
                  ),
                if (_saveError != null) ...[
                  const SizedBox(height: 12),
                  Text(
                    _saveError!,
                    key: const Key('vehicle-save-error'),
                    style: TextStyle(color: theme.colorScheme.error),
                  ),
                ],
                const SizedBox(height: 24),
                GlassButton(
                  width: double.infinity,
                  filled: true,
                  onPressed: _saving ? null : _save,
                  semanticLabel: _vehicleId == null
                      ? 'Add vehicle'
                      : 'Save vehicle',
                  child: _saving
                      ? const _ActionContent(
                          label: 'Saving vehicle',
                          icon: SwiftIcons.refreshCw,
                          loading: true,
                        )
                      : _ActionContent(
                          label: _vehicleId == null
                              ? 'Add vehicle'
                              : 'Save vehicle',
                          icon: SwiftIcons.check,
                        ),
                ),
              ],
            ],
          ),
        ),
      ],
    );
  }
}

// --------------------------------------------------------------- Document URL dialog
//
// Own StatefulWidget so the TextEditingController lives and dies with the
// dialog route: disposing it synchronously after showDialog returns races the
// route's deactivation (IME hide) and throws "used after being disposed".
class _DocumentUrlDialog extends StatefulWidget {
  final String label;
  final String? existingUrl;

  const _DocumentUrlDialog({required this.label, this.existingUrl});

  @override
  State<_DocumentUrlDialog> createState() => _DocumentUrlDialogState();
}

class _DocumentUrlDialogState extends State<_DocumentUrlDialog> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.existingUrl);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _close(String? value) {
    // Unfocus before popping: the IME-hide animation rebuilds the
    // deactivating subtree, which otherwise trips framework assertions.
    FocusManager.instance.primaryFocus?.unfocus();
    Navigator.of(context).pop(value);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    return AlertDialog(
      shape: RoundedRectangleBorder(borderRadius: AppRadii.dialogRadius),
      titlePadding: const EdgeInsets.fromLTRB(20, 20, 20, 8),
      contentPadding: const EdgeInsets.fromLTRB(20, 8, 20, 4),
      actionsPadding: const EdgeInsets.fromLTRB(12, 4, 12, 12),
      title: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: scheme.primary.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(13),
            ),
            child: AppIcon(SwiftIcons.upload, color: scheme.primary, size: 19),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              widget.existingUrl == null
                  ? 'Upload ${widget.label}'
                  : 'Replace ${widget.label}',
            ),
          ),
        ],
      ),
      content: TextField(
        controller: _controller,
        autofocus: true,
        decoration: _profileInputDecoration(
          context,
          label: 'Document URL',
          hintText: 'https://…',
          icon: SwiftIcons.externalLink,
        ),
        keyboardType: TextInputType.url,
      ),
      actions: [
        TextButton(onPressed: () => _close(null), child: const Text('Cancel')),
        FilledButton(
          onPressed: () => _close(_controller.text.trim()),
          child: const Text('Save URL'),
        ),
      ],
    );
  }
}

// --------------------------------------------------------------- Documents

class _DocumentsTab extends StatefulWidget {
  final DriverRemoteDatasource datasource;
  final List<String> missing;
  final Future<void> Function() onSaved;
  final void Function(String) onMessage;

  const _DocumentsTab({
    required this.datasource,
    required this.missing,
    required this.onSaved,
    required this.onMessage,
  });

  @override
  State<_DocumentsTab> createState() => _DocumentsTabState();
}

class _DocumentsTabState extends State<_DocumentsTab>
    with AutomaticKeepAliveClientMixin<_DocumentsTab> {
  static const _requiredDocuments = [
    ('DRIVING_LICENCE', 'Driving licence'),
    ('ADHAAR_CARD', 'Aadhaar card'),
    ('PAN_CARD', 'PAN card'),
  ];

  final Map<String, DriverDocumentModel> _documents = {};
  final Set<String> _uploading = {};
  final Map<String, String> _errors = {};
  bool _loading = true;
  String? _loadError;

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    _loadDocuments();
  }

  Future<void> _loadDocuments() async {
    setState(() {
      _loading = true;
      _loadError = null;
    });
    final response = await widget.datasource.getDocuments();
    if (!mounted) return;
    if (!response.success) {
      setState(() {
        _loading = false;
        _loadError = response.message;
      });
      return;
    }
    setState(() {
      _documents
        ..clear()
        ..addEntries(
          (response.data ?? const <DriverDocumentModel>[]).map(
            (document) => MapEntry(document.documentType, document),
          ),
        );
      _loading = false;
      _loadError = null;
    });
  }

  Future<void> _pickAndUpload(String type, String label) async {
    if (_loading || _loadError != null || _uploading.contains(type)) return;
    try {
      final file = await FilePicker.pickFile(
        type: FileType.custom,
        allowedExtensions: const ['pdf', 'jpg', 'jpeg', 'png', 'webp'],
      );
      if (!mounted || file == null) return;
      final length = file.lengthSync() ?? await file.length();
      if (length == null) {
        setState(
          () => _errors[type] = 'Could not read this file. Try another.',
        );
        return;
      }
      if (length > 10 * 1024 * 1024) {
        setState(() => _errors[type] = 'Choose a file smaller than 10 MB.');
        return;
      }
      final bytes = await file.readAsBytes();
      if (bytes.isEmpty) {
        setState(
          () => _errors[type] = 'Could not read this file. Try another.',
        );
        return;
      }
      setState(() {
        _uploading.add(type);
        _errors.remove(type);
      });
      final response = await widget.datasource.uploadDocumentFile(
        documentType: type,
        fileName: file.name,
        bytes: bytes,
      );
      if (!mounted) return;
      if (!response.success || response.data == null) {
        setState(() => _errors[type] = response.message);
        return;
      }
      setState(() => _documents[type] = response.data!);
      await widget.onSaved();
      if (mounted) widget.onMessage('$label uploaded. Review is pending.');
    } catch (_) {
      if (mounted) {
        setState(
          () => _errors[type] = 'Could not upload $label. Please retry.',
        );
      }
    } finally {
      if (mounted) setState(() => _uploading.remove(type));
    }
  }

  Future<void> _uploadUrl(String type, String label) async {
    if (_loading || _loadError != null || _uploading.contains(type)) return;
    final existingUrl = _documents[type]?.documentUrl;
    final url = await showDialog<String>(
      context: context,
      builder: (context) =>
          _DocumentUrlDialog(label: label, existingUrl: existingUrl),
    );
    if (!mounted || url == null) return;
    final uri = Uri.tryParse(url);
    if (uri == null ||
        !uri.hasAuthority ||
        (uri.scheme != 'https' && uri.scheme != 'http')) {
      setState(
        () => _errors[type] = 'Enter a valid http or https document URL.',
      );
      return;
    }

    setState(() {
      _uploading.add(type);
      _errors.remove(type);
    });
    try {
      final response = await widget.datasource.uploadDocument(
        documentType: type,
        documentUrl: url,
      );
      if (!mounted) return;
      if (!response.success || response.data == null) {
        setState(() => _errors[type] = response.message);
        return;
      }
      setState(() => _documents[type] = response.data!);
      await widget.onSaved();
      if (mounted) widget.onMessage('$label saved.');
    } catch (_) {
      if (mounted) {
        setState(() => _errors[type] = 'Could not save $label. Please retry.');
      }
    } finally {
      if (mounted) setState(() => _uploading.remove(type));
    }
  }

  String _statusLabel(DriverDocumentModel? document) {
    if (document == null) return 'Not uploaded';
    switch (document.verificationStatus.toUpperCase()) {
      case 'APPROVED':
      case 'VERIFIED':
        return 'Verified';
      case 'REJECTED':
        return 'Rejected — replace this document';
      case 'PENDING':
      default:
        return 'Uploaded · Pending review';
    }
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final textScale = MediaQuery.textScalerOf(context).scale(1.0);
    return ListView(
      keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
      physics: const BouncingScrollPhysics(
        parent: AlwaysScrollableScrollPhysics(),
      ),
      padding: const EdgeInsets.fromLTRB(12, 4, 12, 28),
      children: [
        Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Padding(
              padding: EdgeInsets.fromLTRB(4, 6, 4, 0),
              child: _SectionHeading(
                icon: SwiftIcons.fileText,
                title: 'Documents',
                subtitle:
                    'Upload a PDF or image for each required document. Files must be under 10 MB.',
              ),
            ),
            const SizedBox(height: 16),
            if (_loading)
              Semantics(
                label: 'Loading saved documents',
                child: AppShimmer(
                  child: const Column(
                    children: [
                      ShimmerBlock(width: double.infinity, height: 64),
                      SizedBox(height: 14),
                      ShimmerBlock(width: double.infinity, height: 64),
                      SizedBox(height: 14),
                      ShimmerBlock(width: double.infinity, height: 64),
                    ],
                  ),
                ),
              )
            else if (_loadError != null) ...[
              Text(_loadError!, style: TextStyle(color: scheme.error)),
              const SizedBox(height: 8),
              OutlinedButton.icon(
                onPressed: _loadDocuments,
                icon: const AppIcon(SwiftIcons.refreshCw, size: 19),
                label: const Text('Retry loading documents'),
              ),
            ] else
              ..._requiredDocuments.map((doc) {
                final type = doc.$1;
                final label = doc.$2;
                final document = _documents[type];
                final uploaded =
                    document != null && document.documentUrl.isNotEmpty;
                final rejected =
                    document?.verificationStatus.toUpperCase() == 'REJECTED';
                final approved = const {
                  'APPROVED',
                  'VERIFIED',
                }.contains(document?.verificationStatus.toUpperCase());
                final statusColor = rejected
                    ? scheme.error
                    : approved
                    ? scheme.primary
                    : scheme.onSurfaceVariant;
                final uploading = _uploading.contains(type);
                final action = uploading
                    ? Semantics(
                        liveRegion: true,
                        label: 'Saving $label',
                        child: const SizedBox(
                          width: 22,
                          height: 22,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        ),
                      )
                    : OutlinedButton.icon(
                        onPressed: () => _pickAndUpload(type, label),
                        icon: AppIcon(
                          uploaded ? SwiftIcons.refreshCw : SwiftIcons.upload,
                          size: 18,
                        ),
                        label: Text(uploaded ? 'Replace' : 'Upload'),
                      );

                return Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: AppCard(
                    margin: EdgeInsets.zero,
                    padding: const EdgeInsets.all(16),
                    child: LayoutBuilder(
                      builder: (context, constraints) {
                        final stacked =
                            constraints.maxWidth < 430 || textScale >= 1.25;
                        final status = Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            AppIcon(
                              rejected
                                  ? SwiftIcons.circleAlert
                                  : approved
                                  ? SwiftIcons.circleCheck
                                  : SwiftIcons.clock3,
                              color: statusColor,
                              size: 19,
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    label,
                                    style: theme.textTheme.titleSmall?.copyWith(
                                      fontWeight: FontWeight.w700,
                                    ),
                                  ),
                                  const SizedBox(height: 2),
                                  Text(
                                    _statusLabel(document),
                                    style: theme.textTheme.bodySmall?.copyWith(
                                      color: statusColor,
                                    ),
                                  ),
                                  if (uploaded) ...[
                                    const SizedBox(height: 4),
                                    SelectableText(
                                      document.documentUrl.startsWith('/api/')
                                          ? 'Stored securely in Swift'
                                          : document.documentUrl,
                                      maxLines: 2,
                                      style: theme.textTheme.bodySmall
                                          ?.copyWith(color: scheme.primary),
                                    ),
                                  ],
                                  if (rejected &&
                                      (document?.remarks?.isNotEmpty ??
                                          false)) ...[
                                    const SizedBox(height: 8),
                                    Container(
                                      width: double.infinity,
                                      padding: const EdgeInsets.all(10),
                                      decoration: BoxDecoration(
                                        color: scheme.error.withValues(
                                          alpha: 0.08,
                                        ),
                                        borderRadius: BorderRadius.circular(12),
                                      ),
                                      child: Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            'Review reason',
                                            style: theme.textTheme.labelMedium
                                                ?.copyWith(
                                                  color: scheme.error,
                                                  fontWeight: FontWeight.w600,
                                                ),
                                          ),
                                          const SizedBox(height: 2),
                                          Text(
                                            document!.remarks!,
                                            style: theme.textTheme.bodySmall,
                                          ),
                                        ],
                                      ),
                                    ),
                                  ],
                                  if (_errors[type] != null) ...[
                                    const SizedBox(height: 6),
                                    Text(
                                      _errors[type]!,
                                      key: Key('document-error-$type'),
                                      style: theme.textTheme.bodySmall
                                          ?.copyWith(color: scheme.error),
                                    ),
                                  ],
                                ],
                              ),
                            ),
                          ],
                        );
                        if (stacked) {
                          return Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              status,
                              const SizedBox(height: 10),
                              Wrap(
                                alignment: WrapAlignment.end,
                                spacing: 6,
                                runSpacing: 4,
                                children: [
                                  TextButton(
                                    onPressed: uploading
                                        ? null
                                        : () => _uploadUrl(type, label),
                                    child: const Text('Use URL'),
                                  ),
                                  action,
                                ],
                              ),
                            ],
                          );
                        }
                        return Row(
                          crossAxisAlignment: CrossAxisAlignment.center,
                          children: [
                            Expanded(child: status),
                            const SizedBox(width: 12),
                            TextButton(
                              onPressed: uploading
                                  ? null
                                  : () => _uploadUrl(type, label),
                              child: const Text('Use URL'),
                            ),
                            action,
                          ],
                        );
                      },
                    ),
                  ),
                );
              }),
          ],
        ),
      ],
    );
  }
}
