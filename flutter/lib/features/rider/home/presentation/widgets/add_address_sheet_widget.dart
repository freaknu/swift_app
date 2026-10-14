import 'package:swift_app/constants/enums/address_type_enum.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/rider/home/domain/entity/address_create.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_event.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_state.dart';
import 'package:swift_app/features/rider/home/presentation/models/location_selection.dart';
import 'package:swift_app/features/rider/home/presentation/pages/select_location_page.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:flutter/cupertino.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:geocoding/geocoding.dart' as geocoding;
import 'package:geolocator/geolocator.dart';

class AddAddressSheet extends StatefulWidget {
  const AddAddressSheet({super.key});

  @override
  State<AddAddressSheet> createState() => _AddAddressSheetState();
}

class _AddAddressSheetState extends State<AddAddressSheet> {
  final _formKey = GlobalKey<FormState>();
  final _labelController = TextEditingController();
  final _addressController = TextEditingController();
  late final _geocoding = geocoding.Geocoding();
  AddressTypeEnum _addressType = AddressTypeEnum.HOME;
  bool _loadingLocation = true;
  bool _saving = false;
  int _locationRequest = 0;
  Position? _position;
  String? _locationError;
  String _landmark = '';
  String _city = '';
  String _state = '';
  String _country = '';
  String _pinCode = '';

  @override
  void initState() {
    super.initState();
    _getCurrentLocation();
  }

  @override
  void dispose() {
    _labelController.dispose();
    _addressController.dispose();
    super.dispose();
  }

  Future<void> _getCurrentLocation() async {
    final request = ++_locationRequest;
    setState(() {
      _loadingLocation = true;
      _locationError = null;
    });
    try {
      if (!await Geolocator.isLocationServiceEnabled()) {
        throw Exception(
          'Turn on location services or choose a place on the map.',
        );
      }
      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever) {
        throw Exception(
          'Location access is unavailable. You can still choose a place on the map.',
        );
      }
      final position = await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.high,
          timeLimit: Duration(seconds: 15),
        ),
      );
      if (!mounted || request != _locationRequest) return;
      await _setLocation(position, request);
    } catch (_) {
      if (!mounted || request != _locationRequest) return;
      setState(() {
        _loadingLocation = false;
        _locationError = 'Location unavailable. Choose on map or try again.';
      });
    }
  }

  Future<void> _setLocation(Position position, int request) async {
    geocoding.Placemark? place;
    try {
      final places = await _geocoding
          .placemarkFromCoordinates(position.latitude, position.longitude)
          .timeout(const Duration(seconds: 8));
      if (places.isNotEmpty) place = places.first;
    } catch (_) {
      // The user can supply an address if reverse lookup is unavailable.
    }
    if (!mounted || request != _locationRequest) return;
    final parts =
        [
              place?.name,
              place?.street,
              place?.subLocality,
              place?.locality,
              place?.administrativeArea,
              place?.postalCode,
            ]
            .whereType<String>()
            .map((value) => value.trim())
            .where((value) => value.isNotEmpty)
            .toSet();
    setState(() {
      _position = position;
      _addressController.text = parts.join(', ');
      _landmark = place?.name?.trim() ?? '';
      _city = place?.locality?.trim() ?? '';
      _state = place?.administrativeArea?.trim() ?? '';
      _country = place?.country?.trim() ?? '';
      _pinCode = place?.postalCode?.trim() ?? '';
      _loadingLocation = false;
      _locationError = place == null ? 'Enter the address below.' : null;
    });
  }

  Future<void> _openMapSelector() async {
    final selection = await Navigator.of(context).push<LocationSelection>(
      CupertinoPageRoute(
        builder: (_) => SelectLocationPage(initialPosition: _position),
      ),
    );
    if (!mounted || selection == null) return;
    final request = ++_locationRequest;
    setState(() {
      _loadingLocation = true;
      _locationError = null;
    });
    await _setLocation(selection.position, request);
  }

  void _saveAddress() {
    if (!_formKey.currentState!.validate()) return;
    if (_position == null) {
      setState(() => _locationError = 'Choose a location first.');
      return;
    }
    FocusScope.of(context).unfocus();
    setState(() => _saving = true);
    context.read<UserBloc>().add(
      CreateUserAddress(
        address: AddressCreate(
          addressType: _addressType,
          label: _labelController.text.trim(),
          fullAddress: _addressController.text.trim(),
          landmark: _landmark,
          city: _city,
          state: _state,
          country: _country,
          pinCode: _pinCode,
          latitude: _position!.latitude,
          longitude: _position!.longitude,
          defaultAddress: false,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return BlocListener<UserBloc, UserState>(
      listenWhen: (previous, current) =>
          _saving && previous.status != current.status,
      listener: (context, state) {
        if (state.status == UserStatus.success &&
            state.createdAddress != null) {
          setState(() => _saving = false);
          Navigator.of(context).pop();
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(const SnackBar(content: Text('Place saved')));
        } else if (state.status == UserStatus.failure) {
          setState(() => _saving = false);
          final failureMessage = state.message;
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                failureMessage == null || failureMessage.isEmpty
                    ? 'Couldn’t save this place. Try again.'
                    : failureMessage,
              ),
            ),
          );
        }
      },
      child: PopScope(
        canPop: !_saving,
        child: SafeArea(
          top: false,
          child: GlassSurface(
            borderRadius: AppRadii.sheetRadius,
            tint: theme.colorScheme.surface,
            child: SingleChildScrollView(
              keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
              padding: EdgeInsets.fromLTRB(
                20,
                8,
                20,
                MediaQuery.viewInsetsOf(context).bottom + 20,
              ),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Center(
                      child: Container(
                        width: 38,
                        height: 4,
                        decoration: BoxDecoration(
                          color: theme.dividerColor,
                          borderRadius: BorderRadius.circular(4),
                        ),
                      ),
                    ),
                    const SizedBox(height: 14),
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            'Add place',
                            style: theme.textTheme.titleLarge?.copyWith(
                              fontSize: 20,
                              fontWeight: FontWeight.w600,
                              letterSpacing: -0.4,
                            ),
                          ),
                        ),
                        GlassIconButton(
                          icon: SwiftIcons.x,
                          onPressed: _saving
                              ? null
                              : () => Navigator.of(context).pop(),
                          tooltip: 'Close',
                          size: 42,
                          iconSize: 18,
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    SizedBox(
                      width: double.infinity,
                      child: CupertinoSlidingSegmentedControl<AddressTypeEnum>(
                        groupValue: _addressType,
                        backgroundColor: theme.colorScheme.onSurface.withValues(
                          alpha: 0.055,
                        ),
                        thumbColor: theme.colorScheme.surfaceContainerHighest,
                        padding: const EdgeInsets.all(3),
                        children: {
                          for (final type in AddressTypeEnum.values)
                            type: Padding(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 6,
                                vertical: 7,
                              ),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  AppIcon(
                                    type == AddressTypeEnum.HOME
                                        ? SwiftIcons.house
                                        : type == AddressTypeEnum.WORK
                                        ? SwiftIcons.briefcase
                                        : SwiftIcons.bookmark,
                                    size: 15,
                                    color: type == _addressType
                                        ? theme.colorScheme.primary
                                        : theme.colorScheme.onSurfaceVariant,
                                  ),
                                  const SizedBox(width: 5),
                                  Flexible(
                                    child: Text(
                                      type.name[0] +
                                          type.name.substring(1).toLowerCase(),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: theme.textTheme.labelMedium
                                          ?.copyWith(
                                            color: type == _addressType
                                                ? theme.colorScheme.primary
                                                : theme.colorScheme.onSurface,
                                            fontWeight: FontWeight.w600,
                                          ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                        },
                        onValueChanged: (type) {
                          if (!_saving && type != null) {
                            setState(() => _addressType = type);
                          }
                        },
                      ),
                    ),
                    const SizedBox(height: 14),
                    TextFormField(
                      controller: _labelController,
                      enabled: !_saving,
                      textCapitalization: TextCapitalization.words,
                      textInputAction: TextInputAction.next,
                      decoration: InputDecoration(
                        labelText: 'Name',
                        hintText: 'Home or office',
                        prefixIcon: const AppIcon(
                          SwiftIcons.bookmark,
                          size: 19,
                        ),
                        filled: true,
                      ),
                      validator: (value) =>
                          value == null || value.trim().isEmpty
                          ? 'Add a name'
                          : null,
                    ),
                    const SizedBox(height: 14),
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            'Location',
                            style: theme.textTheme.titleSmall?.copyWith(
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                        TextButton.icon(
                          onPressed: _saving ? null : _openMapSelector,
                          icon: const AppIcon(SwiftIcons.map, size: 16),
                          label: const Text('Map'),
                          style: TextButton.styleFrom(
                            minimumSize: const Size(44, 36),
                            tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          ),
                        ),
                      ],
                    ),
                    GlassSurface(
                      borderRadius: BorderRadius.circular(16),
                      blur: 0,
                      boxShadow: const [],
                      padding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 10,
                      ),
                      child: Row(
                        children: [
                          if (_loadingLocation)
                            const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(
                                strokeWidth: 1.8,
                              ),
                            )
                          else
                            AppIcon(
                              _position == null
                                  ? SwiftIcons.mapPin
                                  : SwiftIcons.locateFixed,
                              color: theme.colorScheme.primary,
                              size: 20,
                            ),
                          const SizedBox(width: 9),
                          Expanded(
                            child: Text(
                              _loadingLocation
                                  ? 'Finding location…'
                                  : _position == null
                                  ? 'Choose a location'
                                  : '${_position!.latitude.toStringAsFixed(5)}, ${_position!.longitude.toStringAsFixed(5)}',
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: theme.textTheme.bodySmall?.copyWith(
                                color: theme.colorScheme.onSurface,
                              ),
                            ),
                          ),
                          IconButton(
                            onPressed: _loadingLocation || _saving
                                ? null
                                : _getCurrentLocation,
                            tooltip: 'Use current location',
                            visualDensity: VisualDensity.compact,
                            icon: const AppIcon(SwiftIcons.mapPin, size: 18),
                          ),
                        ],
                      ),
                    ),
                    if (_locationError != null)
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: Text(
                          _locationError!,
                          style: theme.textTheme.bodySmall,
                        ),
                      ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _addressController,
                      enabled: !_loadingLocation && !_saving,
                      maxLines: 2,
                      minLines: 2,
                      textCapitalization: TextCapitalization.sentences,
                      decoration: InputDecoration(
                        labelText: 'Address',
                        alignLabelWithHint: true,
                        hintText: 'Building, street, area',
                        filled: true,
                      ),
                      validator: (value) =>
                          value == null || value.trim().isEmpty
                          ? 'Add an address'
                          : null,
                    ),
                    const SizedBox(height: 18),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton(
                        onPressed: _loadingLocation || _saving
                            ? null
                            : _saveAddress,
                        child: _saving
                            ? SizedBox(
                                width: 18,
                                height: 18,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Theme.of(
                                    context,
                                  ).colorScheme.onSurfaceVariant,
                                ),
                              )
                            : const Text('Save'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
