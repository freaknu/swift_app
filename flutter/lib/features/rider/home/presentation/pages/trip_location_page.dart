import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'dart:async';

import 'package:dio/dio.dart';
import 'package:swift_app/core/config/google_maps_api_config.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/rider/home/data/google_places_response_parser.dart';
import 'package:swift_app/features/rider/home/presentation/models/location_selection.dart';
import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';
import 'package:swift_app/constants/enums/address_type_enum.dart';
import 'package:swift_app/features/rider/home/presentation/pages/select_location_page.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/google_map_widget.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:geocoding/geocoding.dart' as geocoding;
import 'package:geolocator/geolocator.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:uuid/uuid.dart';

enum _TripLocationField { pickup, destination }

class TripLocationPage extends StatefulWidget {
  final LocationSelection? initialPickup;
  final LocationSelection? initialDestination;
  final List<UserAddress> savedAddresses;

  const TripLocationPage({
    super.key,
    this.initialPickup,
    this.initialDestination,
    this.savedAddresses = const [],
  });

  @override
  State<TripLocationPage> createState() => _TripLocationPageState();
}

class _TripLocationPageState extends State<TripLocationPage> {
  final Dio _dio = Dio(
    BaseOptions(
      connectTimeout: const Duration(seconds: 15),
      receiveTimeout: const Duration(seconds: 15),
    ),
  );
  geocoding.Geocoding? _geocoding;
  final TextEditingController _pickupController = TextEditingController();
  final TextEditingController _destinationController = TextEditingController();
  final FocusNode _pickupFocusNode = FocusNode();
  final FocusNode _destinationFocusNode = FocusNode();

  CancelToken? _searchCancelToken;
  CancelToken? _detailsCancelToken;
  Timer? _debounce;

  GoogleMapsApiConfig _mapsConfig = const GoogleMapsApiConfig();
  LocationSelection? _pickupSelection;
  LocationSelection? _destinationSelection;
  List<PlaceSuggestion> _suggestions = [];
  _TripLocationField _activeField = _TripLocationField.destination;
  String? _placesSessionToken;
  int _addressSearchRequest = 0;
  bool _loadingPickup = false;
  bool _searching = false;
  bool _showSuggestions = false;
  bool _hasAutocompleteResult = false;
  bool _autocompleteUnavailable = false;
  bool _placesUnavailableForPage = false;

  geocoding.Geocoding get _geocoder =>
      _geocoding ??= geocoding.Geocoding(locale: const Locale('en', 'IN'));

  @override
  void initState() {
    super.initState();

    _pickupSelection = widget.initialPickup;
    _destinationSelection = widget.initialDestination;
    _pickupController.text = widget.initialPickup?.label ?? '';
    _destinationController.text = widget.initialDestination?.label ?? '';

    _pickupFocusNode.addListener(() {
      if (_pickupFocusNode.hasFocus) _activateField(_TripLocationField.pickup);
    });
    _destinationFocusNode.addListener(() {
      if (_destinationFocusNode.hasFocus) {
        _activateField(_TripLocationField.destination);
      }
    });

    _loadMapsConfig();
    if (_pickupSelection == null) _loadCurrentPickup();
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _searchCancelToken?.cancel();
    _detailsCancelToken?.cancel();
    _pickupController.dispose();
    _destinationController.dispose();
    _pickupFocusNode.dispose();
    _destinationFocusNode.dispose();
    _dio.close();
    super.dispose();
  }

  Future<void> _loadMapsConfig() async {
    final config = await GoogleMapsApiConfig.load();
    if (!mounted) return;
    setState(() => _mapsConfig = config);

    final query = _controllerFor(_activeField).text.trim();
    if (config.isConfigured && query.length >= 2) {
      _onAddressChanged(_activeField, query);
    }
  }

  Future<void> _loadCurrentPickup() async {
    setState(() => _loadingPickup = true);
    try {
      if (!await Geolocator.isLocationServiceEnabled()) return;

      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever) {
        return;
      }

      final position =
          await Geolocator.getLastKnownPosition().timeout(
            const Duration(seconds: 5),
          ) ??
          await Geolocator.getCurrentPosition(
            locationSettings: const LocationSettings(
              accuracy: LocationAccuracy.high,
              timeLimit: Duration(seconds: 20),
            ),
          );

      if (!mounted ||
          _pickupSelection != null ||
          _pickupController.text.trim().isNotEmpty) {
        return;
      }

      var label = 'Current location';
      try {
        final placemarks = await _geocoder
            .placemarkFromCoordinates(position.latitude, position.longitude)
            .timeout(const Duration(seconds: 10));
        if (placemarks.isNotEmpty) {
          label = _formatPlacemark(placemarks.first);
        }
      } catch (_) {
        // Coordinates are enough to proceed when reverse geocoding is offline.
      }

      if (!mounted ||
          _pickupSelection != null ||
          _pickupController.text.trim().isNotEmpty) {
        return;
      }

      setState(() {
        _pickupSelection = LocationSelection(position: position, label: label);
        _pickupController.text = label;
      });
    } catch (e) {
      debugPrint('Current pickup unavailable: $e');
    } finally {
      if (mounted) setState(() => _loadingPickup = false);
    }
  }

  String _formatPlacemark(geocoding.Placemark placemark) {
    final seen = <String>{};
    final parts =
        <String?>[
              placemark.name,
              placemark.street,
              placemark.subLocality,
              placemark.locality,
              placemark.administrativeArea,
              placemark.country,
            ]
            .map((part) => part?.trim() ?? '')
            .where((part) => part.isNotEmpty)
            .where((part) => seen.add(part.toLowerCase()))
            .take(4)
            .toList();

    if (parts.isEmpty) return 'Current location';
    return parts.join(', ');
  }

  TextEditingController _controllerFor(_TripLocationField field) {
    return field == _TripLocationField.pickup
        ? _pickupController
        : _destinationController;
  }

  FocusNode _focusNodeFor(_TripLocationField field) {
    return field == _TripLocationField.pickup
        ? _pickupFocusNode
        : _destinationFocusNode;
  }

  LocationSelection? _selectionFor(_TripLocationField field) {
    return field == _TripLocationField.pickup
        ? _pickupSelection
        : _destinationSelection;
  }

  void _setSelection(_TripLocationField field, LocationSelection? selection) {
    if (field == _TripLocationField.pickup) {
      _pickupSelection = selection;
    } else {
      _destinationSelection = selection;
    }
  }

  void _selectSavedAddress(UserAddress address) {
    final selection = LocationSelection.fromCoordinates(
      latitude: address.latitude,
      longitude: address.longitude,
      label: address.label,
    );
    _destinationFocusNode.unfocus();
    setState(() {
      _setSelection(_TripLocationField.destination, selection);
      _destinationController.text = address.label;
      _activeField = _TripLocationField.destination;
      _showSuggestions = false;
      _suggestions = [];
    });
  }

  Future<void> _chooseOnMap() async {
    final field = _activeField;
    await _hideKeyboard();
    if (!mounted) return;
    final selection = await Navigator.of(context).push<LocationSelection>(
      CupertinoPageRoute(
        builder: (_) => SelectLocationPage(
          initialPosition: _selectionFor(field)?.position,
          initialLabel: _selectionFor(field)?.label,
          fieldLabel: field == _TripLocationField.pickup
              ? 'pickup'
              : 'destination',
        ),
      ),
    );
    if (!mounted || selection == null) return;
    setState(() {
      _setSelection(field, selection);
      _controllerFor(field).text =
          selection.label ??
          '${selection.position.latitude.toStringAsFixed(5)}, '
              '${selection.position.longitude.toStringAsFixed(5)}';
      _showSuggestions = false;
      _suggestions = [];
    });
    _focusNextAfterSelection(field);
  }

  LatLng? _biasFor(_TripLocationField field) {
    final selection = field == _TripLocationField.destination
        ? _pickupSelection
        : _selectionFor(field);
    final position = selection?.position;
    if (position == null) return null;
    return LatLng(position.latitude, position.longitude);
  }

  void _activateField(_TripLocationField field) {
    final query = _controllerFor(field).text.trim();
    setState(() {
      _activeField = field;
      _suggestions = [];
      _showSuggestions = query.length >= 2;
      _hasAutocompleteResult = false;
      _autocompleteUnavailable = _placesUnavailableForPage;
    });
  }

  void _onAddressChanged(_TripLocationField field, String value) {
    final query = value.trim();
    _activeField = field;
    _addressSearchRequest++;
    _debounce?.cancel();
    _searchCancelToken?.cancel();
    _detailsCancelToken?.cancel();

    final selectionLabel = _selectionFor(field)?.label?.trim();
    if (selectionLabel != query) {
      _setSelection(field, null);
    }

    if (query.length < 2) {
      if (query.isEmpty) _placesSessionToken = null;
      setState(() {
        _suggestions = [];
        _showSuggestions = false;
        _searching = false;
        _hasAutocompleteResult = false;
        _autocompleteUnavailable = false;
      });
      return;
    }

    setState(() {
      _suggestions = [];
      _searching = _mapsConfig.isConfigured && !_placesUnavailableForPage;
      _showSuggestions = true;
      _hasAutocompleteResult = false;
      _autocompleteUnavailable = _placesUnavailableForPage;
    });

    if (!_mapsConfig.isConfigured || _placesUnavailableForPage) return;

    final request = _addressSearchRequest;
    _debounce = Timer(const Duration(milliseconds: 400), () {
      _getSuggestions(query, field: field, request: request);
    });
  }

  void _submitSearch(String value) {
    final query = value.trim();
    if (query.length < 2) return;
    _debounce?.cancel();
    _searchCancelToken?.cancel();
    _detailsCancelToken?.cancel();
    _searchAddress(query, field: _activeField);
  }

  Future<void> _getSuggestions(
    String query, {
    required _TripLocationField field,
    required int request,
  }) async {
    if (query.length < 2 || !mounted || !_mapsConfig.isConfigured) return;

    setState(() {
      _searching = true;
      _showSuggestions = true;
      _hasAutocompleteResult = false;
      _autocompleteUnavailable = false;
    });

    final sessionToken = _placesSessionToken ??= const Uuid().v4();
    final cancelToken = CancelToken();
    _searchCancelToken = cancelToken;
    try {
      final requestBody = <String, dynamic>{
        'input': query,
        'includedRegionCodes': ['in'],
        'regionCode': 'in',
        'languageCode': 'en',
        'sessionToken': sessionToken,
      };

      final bias = _biasFor(field);
      if (bias != null) {
        requestBody['locationBias'] = {
          'circle': {
            'center': {'latitude': bias.latitude, 'longitude': bias.longitude},
            'radius': 50000.0,
          },
        };
      }

      final Response response = await _dio.post(
        'https://places.googleapis.com/v1/places:autocomplete',
        cancelToken: cancelToken,
        options: Options(
          headers: _mapsConfig.headersFor(
            'suggestions.placePrediction.placeId,'
            'suggestions.placePrediction.text.text,'
            'suggestions.placePrediction.structuredFormat.mainText.text,'
            'suggestions.placePrediction.structuredFormat.secondaryText.text',
          ),
        ),
        data: requestBody,
      );

      if (!mounted ||
          cancelToken.isCancelled ||
          request != _addressSearchRequest ||
          _activeField != field ||
          _controllerFor(field).text.trim() != query) {
        return;
      }

      setState(() {
        _suggestions = parseAutocompleteResponse(response.data);
        _searching = false;
        _showSuggestions = true;
        _hasAutocompleteResult = true;
      });
    } on DioException catch (e) {
      if (CancelToken.isCancel(e)) return;
      debugPrint(
        'Places autocomplete unavailable (${e.response?.statusCode ?? e.type.name})',
      );
      if (!mounted || request != _addressSearchRequest) return;

      setState(() {
        _suggestions = [];
        _searching = false;
        _showSuggestions = true;
        _hasAutocompleteResult = false;
        _autocompleteUnavailable = true;
        if (e.response?.statusCode == 401 || e.response?.statusCode == 403) {
          _placesUnavailableForPage = true;
        }
      });
    } catch (_) {
      debugPrint('Places autocomplete unavailable');
      if (!mounted || request != _addressSearchRequest) return;

      setState(() {
        _suggestions = [];
        _searching = false;
        _showSuggestions = true;
        _hasAutocompleteResult = false;
        _autocompleteUnavailable = true;
      });
    }
  }

  Future<void> _searchAddress(
    String query, {
    required _TripLocationField field,
    String? displayText,
  }) async {
    final request = ++_addressSearchRequest;
    _placesSessionToken = null;
    await _hideKeyboard();
    setState(() {
      _activeField = field;
      _searching = true;
      _showSuggestions = false;
      _suggestions = [];
      _hasAutocompleteResult = false;
      _autocompleteUnavailable = false;
    });

    try {
      final locations = await _geocoder
          .locationFromAddress(query)
          .timeout(const Duration(seconds: 12));
      if (!mounted || request != _addressSearchRequest) return;
      if (locations.isEmpty) {
        throw StateError('No locations found');
      }

      final first = locations.first;
      final label = displayText ?? query;
      setState(() {
        _setSelection(
          field,
          LocationSelection(
            position: _positionFromCoordinates(first.latitude, first.longitude),
            label: label,
          ),
        );
        _controllerFor(field).text = label;
        _searching = false;
      });
      _focusNextAfterSelection(field);
    } catch (_) {
      if (!mounted || request != _addressSearchRequest) return;
      setState(() {
        _searching = false;
        _showSuggestions = true;
        _hasAutocompleteResult = true;
      });
      _showMessage('No places found.');
    }
  }

  Future<void> _selectSuggestion(PlaceSuggestion suggestion) async {
    final field = _activeField;
    _addressSearchRequest++;
    await _hideKeyboard();
    _debounce?.cancel();
    _searchCancelToken?.cancel();
    _detailsCancelToken?.cancel();
    final sessionToken = _placesSessionToken;
    final fallbackQuery = suggestion.fullText.isNotEmpty
        ? suggestion.fullText
        : [
            suggestion.mainText,
            suggestion.secondaryText,
          ].where((part) => part.isNotEmpty).join(', ');

    setState(() {
      _searching = true;
      _showSuggestions = false;
      _suggestions = [];
      _controllerFor(field).text = suggestion.mainText;
    });

    final detailsToken = CancelToken();
    _detailsCancelToken = detailsToken;
    try {
      final Response response = await _dio.get(
        'https://places.googleapis.com/v1/places/${suggestion.placeId}',
        cancelToken: detailsToken,
        options: Options(headers: _mapsConfig.headersFor('location')),
        queryParameters: sessionToken == null
            ? null
            : {'sessionToken': sessionToken},
      );

      final coordinates = parsePlaceDetailsResponse(response.data);
      if (coordinates == null) {
        throw Exception('Coordinates not found');
      }

      if (!mounted || detailsToken.isCancelled) return;

      setState(() {
        _setSelection(
          field,
          LocationSelection(
            position: _positionFromCoordinates(
              coordinates.latitude,
              coordinates.longitude,
            ),
            label: suggestion.mainText,
          ),
        );
        _placesSessionToken = null;
        _searching = false;
      });
      _focusNextAfterSelection(field);
    } on DioException catch (e) {
      if (CancelToken.isCancel(e)) return;
      debugPrint(
        'Place details unavailable (${e.response?.statusCode ?? e.type.name})',
      );
      if (!mounted || detailsToken.isCancelled) return;

      await _searchAddress(
        fallbackQuery,
        field: field,
        displayText: suggestion.mainText,
      );
    } catch (_) {
      debugPrint('Place details unavailable');
      if (!mounted || detailsToken.isCancelled) return;

      await _searchAddress(
        fallbackQuery,
        field: field,
        displayText: suggestion.mainText,
      );
    }
  }

  Position _positionFromCoordinates(double latitude, double longitude) {
    return Position(
      longitude: longitude,
      latitude: latitude,
      timestamp: DateTime.now(),
      accuracy: 0,
      altitude: 0,
      altitudeAccuracy: 0,
      heading: 0,
      headingAccuracy: 0,
      speed: 0,
      speedAccuracy: 0,
    );
  }

  void _focusNextAfterSelection(_TripLocationField field) {
    if (field == _TripLocationField.pickup && _destinationSelection == null) {
      setState(() => _activeField = _TripLocationField.destination);
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) _destinationFocusNode.requestFocus();
      });
    }
  }

  void _clearField(_TripLocationField field) {
    _addressSearchRequest++;
    _placesSessionToken = null;
    _debounce?.cancel();
    _searchCancelToken?.cancel();
    _detailsCancelToken?.cancel();

    _controllerFor(field).clear();
    setState(() {
      _activeField = field;
      _setSelection(field, null);
      _suggestions = [];
      _showSuggestions = false;
      _searching = false;
      _hasAutocompleteResult = false;
      _autocompleteUnavailable = false;
    });
    _focusNodeFor(field).requestFocus();
  }

  Future<void> _openRouteMap() async {
    final pickup = _pickupSelection;
    final destination = _destinationSelection;
    if (pickup == null || destination == null) {
      _showMessage('Select both locations.');
      return;
    }

    final trip = TripLocations(pickup: pickup, destination: destination);
    await _hideKeyboard();
    if (!mounted) return;
    Navigator.of(context).pop(trip);
  }

  Future<void> _closeTextInput() async {
    await SystemChannels.textInput.invokeMethod<void>('TextInput.hide');
    try {
      await SystemChannels.textInput.invokeMethod<void>(
        'TextInput.clearClient',
      );
    } catch (_) {
      // Older Flutter engines may not expose clearClient to framework calls.
    }
  }

  Future<void> _hideKeyboard() async {
    _pickupFocusNode.unfocus(disposition: UnfocusDisposition.scope);
    _destinationFocusNode.unfocus(disposition: UnfocusDisposition.scope);
    if (mounted) {
      FocusScope.of(context).unfocus(disposition: UnfocusDisposition.scope);
    }
    FocusManager.instance.primaryFocus?.unfocus(
      disposition: UnfocusDisposition.scope,
    );
    await _closeTextInput();
    await Future<void>.delayed(const Duration(milliseconds: 180));
    await _closeTextInput();
  }

  void _showMessage(String message) {
    if (!mounted) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        behavior: SnackBarBehavior.floating,
        margin: const EdgeInsets.all(12),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final query = _controllerFor(_activeField).text.trim();
    final canContinue =
        _pickupSelection != null &&
        _destinationSelection != null &&
        !_searching &&
        !_loadingPickup;

    return Scaffold(
      backgroundColor: Colors.transparent,
      resizeToAvoidBottomInset: true,
      appBar: SwiftAppBar(
        toolbarHeight: SwiftAppBar.toolbarHeightFor(context),
        title: const Text('Plan your ride'),
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
          child: Column(
            children: [
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 18),
                  keyboardDismissBehavior:
                      ScrollViewKeyboardDismissBehavior.onDrag,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: _RouteInputCard(
                          pickupController: _pickupController,
                          destinationController: _destinationController,
                          pickupFocusNode: _pickupFocusNode,
                          destinationFocusNode: _destinationFocusNode,
                          activeField: _activeField,
                          pickupSelected: _pickupSelection != null,
                          destinationSelected: _destinationSelection != null,
                          loadingPickup: _loadingPickup,
                          searching: _searching,
                          onPickupChanged: (value) => _onAddressChanged(
                            _TripLocationField.pickup,
                            value,
                          ),
                          onDestinationChanged: (value) => _onAddressChanged(
                            _TripLocationField.destination,
                            value,
                          ),
                          onPickupSubmitted: (value) {
                            _activeField = _TripLocationField.pickup;
                            _submitSearch(value);
                          },
                          onDestinationSubmitted: (value) {
                            _activeField = _TripLocationField.destination;
                            _submitSearch(value);
                          },
                          onPickupTap: () =>
                              _activateField(_TripLocationField.pickup),
                          onDestinationTap: () =>
                              _activateField(_TripLocationField.destination),
                          onClearPickup: () =>
                              _clearField(_TripLocationField.pickup),
                          onClearDestination: () =>
                              _clearField(_TripLocationField.destination),
                        ),
                      ),
                      AnimatedSwitcher(
                        duration: const Duration(milliseconds: 180),
                        child: _showSuggestions && query.isNotEmpty
                            ? _SuggestionsList(
                                key: ValueKey(_activeField.name),
                                query: query,
                                suggestions: _suggestions,
                                searching: _searching,
                                showNoResults: _hasAutocompleteResult,
                                unavailable: _autocompleteUnavailable,
                                onSearch: _submitSearch,
                                onSelected: _selectSuggestion,
                              )
                            : Column(
                                key: const ValueKey('route-shortcuts'),
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: [
                                  if (widget.savedAddresses.isNotEmpty) ...[
                                    Row(
                                      children: [
                                        for (final type in [
                                          AddressTypeEnum.HOME,
                                          AddressTypeEnum.WORK,
                                        ]) ...[
                                          if (type != AddressTypeEnum.HOME)
                                            const SizedBox(width: 10),
                                          Expanded(
                                            child: _SavedDestinationTile(
                                              label:
                                                  type == AddressTypeEnum.HOME
                                                  ? 'Home'
                                                  : 'Work',
                                              icon: type == AddressTypeEnum.HOME
                                                  ? SwiftIcons.house
                                                  : SwiftIcons.briefcase,
                                              address: _findSavedAddress(type),
                                              onTap: _selectSavedAddress,
                                            ),
                                          ),
                                        ],
                                      ],
                                    ),
                                    const SizedBox(height: 16),
                                  ],
                                  ClipRRect(
                                    borderRadius: BorderRadius.circular(20),
                                    child: const SizedBox(
                                      height: 205,
                                      child: GoogleMapWidget(),
                                    ),
                                  ),
                                  const SizedBox(height: 18),
                                  OutlinedButton.icon(
                                    onPressed: _chooseOnMap,
                                    icon: const AppIcon(SwiftIcons.map),
                                    label: const Text('Choose on map'),
                                    style: OutlinedButton.styleFrom(
                                      minimumSize: const Size.fromHeight(52),
                                      side: BorderSide(
                                        color: Theme.of(
                                          context,
                                        ).colorScheme.primary,
                                      ),
                                      shape: RoundedRectangleBorder(
                                        borderRadius: BorderRadius.circular(16),
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                      ),
                    ],
                  ),
                ),
              ),
              SafeArea(
                top: false,
                child: GlassSurface(
                  margin: const EdgeInsets.fromLTRB(16, 0, 16, 10),
                  padding: const EdgeInsets.all(8),
                  borderRadius: AppRadii.cardRadius,
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(
                      minWidth: double.infinity,
                      minHeight: 52,
                    ),
                    child: FilledButton(
                      onPressed: canContinue ? _openRouteMap : null,
                      style: FilledButton.styleFrom(
                        backgroundColor: Theme.of(context).colorScheme.primary,
                        elevation: 0,
                        shape: RoundedRectangleBorder(
                          borderRadius: AppRadii.button,
                        ),
                      ),
                      child: const Text('Next'),
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

  UserAddress? _findSavedAddress(AddressTypeEnum type) {
    for (final address in widget.savedAddresses) {
      if (address.addressType == type) return address;
    }
    return null;
  }
}

class _SavedDestinationTile extends StatelessWidget {
  final String label;
  final SwiftIcons icon;
  final UserAddress? address;
  final ValueChanged<UserAddress> onTap;

  const _SavedDestinationTile({
    required this.label,
    required this.icon,
    required this.address,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final value = address;
    return Material(
      color: Theme.of(context).colorScheme.surface,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        onTap: value == null ? null : () => onTap(value),
        borderRadius: BorderRadius.circular(16),
        child: Container(
          constraints: const BoxConstraints(minHeight: 75),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: Theme.of(context).dividerColor),
          ),
          child: Row(
            children: [
              AppIcon(icon, color: Theme.of(context).colorScheme.primary),
              const SizedBox(width: 9),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      label,
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                    Text(
                      value?.label ?? 'Add in Saved',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _RouteInputCard extends StatelessWidget {
  final TextEditingController pickupController;
  final TextEditingController destinationController;
  final FocusNode pickupFocusNode;
  final FocusNode destinationFocusNode;
  final _TripLocationField activeField;
  final bool pickupSelected;
  final bool destinationSelected;
  final bool loadingPickup;
  final bool searching;
  final ValueChanged<String> onPickupChanged;
  final ValueChanged<String> onDestinationChanged;
  final ValueChanged<String> onPickupSubmitted;
  final ValueChanged<String> onDestinationSubmitted;
  final VoidCallback onPickupTap;
  final VoidCallback onDestinationTap;
  final VoidCallback onClearPickup;
  final VoidCallback onClearDestination;

  const _RouteInputCard({
    required this.pickupController,
    required this.destinationController,
    required this.pickupFocusNode,
    required this.destinationFocusNode,
    required this.activeField,
    required this.pickupSelected,
    required this.destinationSelected,
    required this.loadingPickup,
    required this.searching,
    required this.onPickupChanged,
    required this.onDestinationChanged,
    required this.onPickupSubmitted,
    required this.onDestinationSubmitted,
    required this.onPickupTap,
    required this.onDestinationTap,
    required this.onClearPickup,
    required this.onClearDestination,
  });

  @override
  Widget build(BuildContext context) {
    return GlassSurface(
      borderRadius: AppRadii.cardRadius,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      child: Material(
        color: Colors.transparent,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _RouteFieldRow(
              label: 'From',
              hint: loadingPickup ? 'Finding pickup' : 'Pickup',
              controller: pickupController,
              focusNode: pickupFocusNode,
              active: activeField == _TripLocationField.pickup,
              selected: pickupSelected,
              icon: SwiftIcons.locateFixed,
              iconColor: Theme.of(context).colorScheme.primary,
              busy:
                  loadingPickup ||
                  (searching &&
                      activeField == _TripLocationField.pickup &&
                      pickupController.text.trim().length >= 2),
              onChanged: onPickupChanged,
              onSubmitted: onPickupSubmitted,
              onTap: onPickupTap,
              onClear: onClearPickup,
            ),
            Divider(
              height: 1,
              indent: 42,
              color: Theme.of(context).dividerColor,
            ),
            _RouteFieldRow(
              label: 'To',
              hint: 'Destination',
              controller: destinationController,
              focusNode: destinationFocusNode,
              active: activeField == _TripLocationField.destination,
              selected: destinationSelected,
              icon: SwiftIcons.flag,
              iconColor: Theme.of(context).colorScheme.onSurfaceVariant,
              busy:
                  searching &&
                  activeField == _TripLocationField.destination &&
                  destinationController.text.trim().length >= 2,
              onChanged: onDestinationChanged,
              onSubmitted: onDestinationSubmitted,
              onTap: onDestinationTap,
              onClear: onClearDestination,
            ),
          ],
        ),
      ),
    );
  }
}

class _RouteFieldRow extends StatelessWidget {
  final String label;
  final String hint;
  final TextEditingController controller;
  final FocusNode focusNode;
  final bool active;
  final bool selected;
  final SwiftIcons icon;
  final Color iconColor;
  final bool busy;
  final ValueChanged<String> onChanged;
  final ValueChanged<String> onSubmitted;
  final VoidCallback onTap;
  final VoidCallback onClear;

  const _RouteFieldRow({
    required this.label,
    required this.hint,
    required this.controller,
    required this.focusNode,
    required this.active,
    required this.selected,
    required this.icon,
    required this.iconColor,
    required this.busy,
    required this.onChanged,
    required this.onSubmitted,
    required this.onTap,
    required this.onClear,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final value = controller.text.trim();
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 7),
      child: Row(
        children: [
          Container(
            width: 30,
            height: 30,
            decoration: BoxDecoration(
              color: iconColor.withValues(alpha: active ? 0.14 : 0.08),
              shape: BoxShape.circle,
            ),
            child: AppIcon(icon, color: iconColor, size: 16),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: theme.textTheme.bodySmall?.copyWith(
                    fontSize: 11,
                    color: active
                        ? theme.colorScheme.primary
                        : theme.colorScheme.onSurfaceVariant,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                ConstrainedBox(
                  constraints: BoxConstraints(
                    minHeight:
                        MediaQuery.textScalerOf(context).scale(14) * 1.4 + 8,
                  ),
                  child: selected && value.isNotEmpty
                      ? InkWell(
                          onTap: onClear,
                          child: Align(
                            alignment: Alignment.centerLeft,
                            child: Text(
                              value,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: theme.textTheme.bodyMedium?.copyWith(
                                fontSize: 14,
                                fontWeight: FontWeight.w600,
                                color: theme.colorScheme.onSurface,
                              ),
                            ),
                          ),
                        )
                      : TextField(
                          controller: controller,
                          focusNode: focusNode,
                          onChanged: onChanged,
                          onSubmitted: onSubmitted,
                          onTap: onTap,
                          keyboardType: TextInputType.text,
                          textInputAction: TextInputAction.search,
                          minLines: 1,
                          maxLines: 1,
                          style: theme.textTheme.bodyMedium?.copyWith(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            color: theme.colorScheme.onSurface,
                          ),
                          decoration: InputDecoration(
                            border: InputBorder.none,
                            enabledBorder: InputBorder.none,
                            focusedBorder: InputBorder.none,
                            filled: false,
                            hintText: hint,
                            hintStyle: theme.textTheme.bodyMedium?.copyWith(
                              fontSize: 14,
                              color: theme.colorScheme.onSurfaceVariant,
                              fontWeight: FontWeight.w500,
                            ),
                            isDense: true,
                            contentPadding: EdgeInsets.zero,
                          ),
                        ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          if (busy)
            SizedBox(
              width: 18,
              height: 18,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: Theme.of(context).colorScheme.primary,
              ),
            )
          else if (value.isNotEmpty)
            GestureDetector(
              onTap: onClear,
              child: Padding(
                padding: EdgeInsets.all(5),
                child: AppIcon(
                  SwiftIcons.circleX,
                  size: 18,
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _SuggestionsList extends StatelessWidget {
  final String query;
  final List<PlaceSuggestion> suggestions;
  final bool searching;
  final bool showNoResults;
  final bool unavailable;
  final ValueChanged<String> onSearch;
  final ValueChanged<PlaceSuggestion> onSelected;

  const _SuggestionsList({
    super.key,
    required this.query,
    required this.suggestions,
    required this.searching,
    required this.showNoResults,
    required this.unavailable,
    required this.onSearch,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    if (searching && suggestions.isEmpty) {
      return _SuggestionsShell(
        child: SizedBox(
          height: 50,
          child: Center(
            child: Text(
              'Finding places',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ),
        ),
      );
    }

    if (suggestions.isEmpty) {
      return _SuggestionsShell(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _SearchActionTile(query: query, onTap: () => onSearch(query)),
            if (unavailable || showNoResults) ...[
              Divider(
                height: 1,
                color: Theme.of(context).colorScheme.outlineVariant,
              ),
              Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 11,
                ),
                child: Row(
                  children: [
                    AppIcon(
                      unavailable ? SwiftIcons.circleAlert : SwiftIcons.search,
                      size: 16,
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                    const SizedBox(width: 9),
                    Expanded(
                      child: Text(
                        unavailable ? 'Suggestions unavailable' : 'No places',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      );
    }

    return _SuggestionsShell(
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxHeight: 360),
        child: ListView.separated(
          padding: const EdgeInsets.symmetric(vertical: 4),
          shrinkWrap: true,
          itemCount: suggestions.length + 1,
          separatorBuilder: (_, _) {
            return Divider(
              height: 1,
              indent: 58,
              endIndent: 12,
              color: Theme.of(context).colorScheme.outlineVariant,
            );
          },
          itemBuilder: (context, index) {
            if (index == 0) {
              return _SearchActionTile(
                query: query,
                onTap: () => onSearch(query),
              );
            }

            final suggestion = suggestions[index - 1];
            return InkWell(
              onTap: () => onSelected(suggestion),
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 13,
                  vertical: 9,
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      width: 32,
                      height: 32,
                      decoration: BoxDecoration(
                        color: Theme.of(
                          context,
                        ).colorScheme.primary.withValues(alpha: 0.09),
                        shape: BoxShape.circle,
                      ),
                      child: AppIcon(
                        SwiftIcons.mapPin,
                        size: 17,
                        color: Theme.of(context).colorScheme.primary,
                      ),
                    ),
                    const SizedBox(width: 11),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            suggestion.mainText,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: Theme.of(context).textTheme.bodyMedium
                                ?.copyWith(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                ),
                          ),
                          if (suggestion.secondaryText.isNotEmpty)
                            Padding(
                              padding: const EdgeInsets.only(top: 3),
                              child: Text(
                                suggestion.secondaryText,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: Theme.of(
                                  context,
                                ).textTheme.bodySmall?.copyWith(fontSize: 10.5),
                              ),
                            ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}

class _SuggestionsShell extends StatelessWidget {
  final Widget child;

  const _SuggestionsShell({required this.child});

  @override
  Widget build(BuildContext context) {
    return GlassSurface(
      borderRadius: AppRadii.cardRadius,
      child: Material(color: Colors.transparent, child: child),
    );
  }
}

class _SearchActionTile extends StatelessWidget {
  final String query;
  final VoidCallback onTap;

  const _SearchActionTile({required this.query, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 11),
        child: Row(
          children: [
            Container(
              width: 32,
              height: 32,
              decoration: BoxDecoration(
                color: Theme.of(
                  context,
                ).colorScheme.primary.withValues(alpha: 0.09),
                shape: BoxShape.circle,
              ),
              child: AppIcon(
                SwiftIcons.search,
                size: 16,
                color: Theme.of(context).colorScheme.primary,
              ),
            ),
            const SizedBox(width: 11),
            Expanded(
              child: Text(
                'Search "$query"',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
