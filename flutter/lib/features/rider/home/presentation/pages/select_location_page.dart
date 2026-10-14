import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'dart:async';

import 'package:dio/dio.dart';
import 'package:swift_app/core/config/google_maps_api_config.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/app_shimmer.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/rider/home/data/google_places_response_parser.dart';
import 'package:swift_app/features/rider/home/presentation/models/location_selection.dart';
import 'package:swift_app/features/rider/home/presentation/widgets/google_map_style.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:flutter/material.dart';
import 'package:geocoding/geocoding.dart' as geocoding;
import 'package:geolocator/geolocator.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:uuid/uuid.dart';

class SelectLocationPage extends StatefulWidget {
  final Position? initialPosition;
  final String? initialLabel;
  final String fieldLabel;

  const SelectLocationPage({
    super.key,
    this.initialPosition,
    this.initialLabel,
    this.fieldLabel = 'location',
  });

  @override
  State<SelectLocationPage> createState() => _SelectLocationPageState();
}

class _SelectLocationPageState extends State<SelectLocationPage> {
  final Dio _dio = Dio(
    BaseOptions(
      connectTimeout: const Duration(seconds: 15),
      receiveTimeout: const Duration(seconds: 15),
    ),
  );
  final geocoding.Geocoding _geocoding = geocoding.Geocoding(
    locale: const Locale('en', 'IN'),
  );
  CancelToken? _searchCancelToken;
  CancelToken? _detailsCancelToken;
  bool _locationChosen = false;
  bool _mapTouched = false;
  bool _locationAllowed = false;
  bool _locating = false;

  final TextEditingController _searchController = TextEditingController();

  final FocusNode _searchFocusNode = FocusNode();

  GoogleMapController? _mapController;

  Timer? _debounce;

  late LatLng _selectedLocation;

  List<PlaceSuggestion> _suggestions = [];

  GoogleMapsApiConfig _mapsConfig = const GoogleMapsApiConfig();
  String? _placesSessionToken;
  String? _selectedLabel;
  int _addressSearchRequest = 0;
  bool _searching = false;
  bool _moving = false;
  bool _showSuggestions = false;
  bool _hasAutocompleteResult = false;
  bool _autocompleteUnavailable = false;
  bool _placesUnavailableForPage = false;

  @override
  void initState() {
    super.initState();
    final Position? position = widget.initialPosition;
    _selectedLabel = widget.initialLabel;

    if (position != null) {
      _selectedLocation = LatLng(position.latitude, position.longitude);
      _locationChosen = true;
    } else {
      _selectedLocation = const LatLng(28.6139, 77.2090);
    }
    _searchFocusNode.addListener(_onSearchFocusChanged);
    _loadMapsConfig();
    _checkLocationPermission();
  }

  Future<void> _loadMapsConfig() async {
    final config = await GoogleMapsApiConfig.load();
    if (!mounted) return;
    setState(() => _mapsConfig = config);

    final query = _searchController.text.trim();
    if (config.isConfigured && query.length >= 2) {
      _onSearchChanged(query);
    }
  }

  void _onSearchFocusChanged() {
    if (!mounted || !_searchFocusNode.hasFocus) return;
    if (_searchController.text.trim().length >= 2) {
      setState(() => _showSuggestions = true);
    }
  }

  void _showSearchSuggestions() {
    if (_searchController.text.trim().length >= 2 && mounted) {
      setState(() => _showSuggestions = true);
    }
  }

  Future<void> _checkLocationPermission() async {
    try {
      final permission = await Geolocator.checkPermission();
      if (!mounted) return;
      setState(
        () => _locationAllowed =
            permission == LocationPermission.always ||
            permission == LocationPermission.whileInUse,
      );
      if (!_locationAllowed ||
          widget.initialPosition != null ||
          !await Geolocator.isLocationServiceEnabled()) {
        return;
      }
      final position =
          await Geolocator.getLastKnownPosition().timeout(
            const Duration(seconds: 5),
          ) ??
          await Geolocator.getCurrentPosition(
            locationSettings: const LocationSettings(
              accuracy: LocationAccuracy.medium,
              timeLimit: Duration(seconds: 15),
            ),
          );
      // Do not replace a point the rider has already selected while GPS loads.
      if (!mounted || _mapTouched || _locationChosen) return;
      final location = LatLng(position.latitude, position.longitude);
      setState(() {
        _selectedLocation = location;
        _locationChosen = true;
      });
      await _mapController?.animateCamera(
        CameraUpdate.newLatLngZoom(location, 16),
      );
    } catch (_) {
      // Manual map selection remains available without device location.
    }
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _searchCancelToken?.cancel();
    _detailsCancelToken?.cancel();
    _mapController?.dispose();
    _searchController.dispose();
    _searchFocusNode.dispose();
    _dio.close();

    super.dispose();
  }

  void _onSearchChanged(String value) {
    final String query = value.trim();

    _addressSearchRequest++;
    _debounce?.cancel();
    _searchCancelToken?.cancel();
    _detailsCancelToken?.cancel();

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

    _debounce = Timer(const Duration(milliseconds: 400), () {
      _getSuggestions(query);
    });
  }

  void _submitSearch(String value) {
    final query = value.trim();
    if (query.length < 2) return;
    _debounce?.cancel();
    _searchCancelToken?.cancel();
    _detailsCancelToken?.cancel();
    _searchAddress(query);
  }

  Future<void> _getSuggestions(String query) async {
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
      if (_locationChosen) {
        requestBody['locationBias'] = {
          'circle': {
            'center': {
              'latitude': _selectedLocation.latitude,
              'longitude': _selectedLocation.longitude,
            },
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
          _searchController.text.trim() != query) {
        return;
      }

      final results = parseAutocompleteResponse(response.data);

      if (!mounted) {
        return;
      }

      setState(() {
        _suggestions = results;
        _searching = false;
        _showSuggestions = true;
        _hasAutocompleteResult = true;
      });
    } on DioException catch (e) {
      if (CancelToken.isCancel(e)) return;
      debugPrint(
        'Places autocomplete unavailable (${e.response?.statusCode ?? e.type.name})',
      );

      if (!mounted) {
        return;
      }

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
    } catch (e) {
      debugPrint('Places autocomplete unavailable');

      if (!mounted) {
        return;
      }

      setState(() {
        _suggestions = [];
        _searching = false;
        _showSuggestions = true;
        _hasAutocompleteResult = false;
        _autocompleteUnavailable = true;
      });
    }
  }

  Future<void> _searchAddress(String query, {String? displayText}) async {
    final request = ++_addressSearchRequest;
    _placesSessionToken = null;
    FocusScope.of(context).unfocus();
    setState(() {
      _searching = true;
      _showSuggestions = false;
      _suggestions = [];
      _hasAutocompleteResult = false;
      _autocompleteUnavailable = false;
    });

    try {
      final locations = await _geocoding
          .locationFromAddress(query)
          .timeout(const Duration(seconds: 12));
      if (!mounted || request != _addressSearchRequest) return;
      if (locations.isEmpty) {
        throw StateError('No locations found');
      }

      final first = locations.first;
      final selected = LatLng(first.latitude, first.longitude);
      setState(() {
        _selectedLocation = selected;
        _locationChosen = true;
        _searching = false;
        _searchController.text = displayText ?? query;
        _selectedLabel = displayText ?? query;
        _placesSessionToken = null;
      });

      await _mapController?.animateCamera(
        CameraUpdate.newCameraPosition(
          CameraPosition(target: selected, zoom: 17),
        ),
      );
    } catch (_) {
      if (!mounted || request != _addressSearchRequest) return;
      setState(() {
        _searching = false;
        _showSuggestions = true;
        _hasAutocompleteResult = true;
      });
      _showMessage('No places found. Try another search.');
    }
  }

  Future<void> _selectSuggestion(PlaceSuggestion suggestion) async {
    _addressSearchRequest++;
    FocusScope.of(context).unfocus();
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
      _searchController.text = suggestion.mainText;
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

      final LatLng selected = LatLng(
        coordinates.latitude,
        coordinates.longitude,
      );

      if (!mounted || detailsToken.isCancelled) {
        return;
      }

      setState(() {
        _selectedLocation = selected;
        _locationChosen = true;
        _selectedLabel = suggestion.mainText;
        _placesSessionToken = null;
      });

      await _mapController?.animateCamera(
        CameraUpdate.newCameraPosition(
          CameraPosition(target: selected, zoom: 17),
        ),
      );

      if (!mounted || detailsToken.isCancelled) {
        return;
      }

      setState(() {
        _searching = false;
      });
    } on DioException catch (e) {
      if (CancelToken.isCancel(e)) return;
      debugPrint(
        'Place details unavailable (${e.response?.statusCode ?? e.type.name})',
      );

      if (!mounted || detailsToken.isCancelled) {
        return;
      }

      await _searchAddress(fallbackQuery, displayText: suggestion.mainText);
    } catch (e) {
      debugPrint('Place details unavailable');

      if (!mounted || detailsToken.isCancelled) {
        return;
      }

      await _searchAddress(fallbackQuery, displayText: suggestion.mainText);
    }
  }

  Future<void> _moveToCurrentLocation() async {
    if (_locating) return;
    _addressSearchRequest++;
    _placesSessionToken = null;
    _debounce?.cancel();
    _searchCancelToken?.cancel();
    _detailsCancelToken?.cancel();
    setState(() {
      _locating = true;
      _searching = false;
      _showSuggestions = false;
    });
    try {
      final bool serviceEnabled = await Geolocator.isLocationServiceEnabled();

      if (!serviceEnabled) {
        _showMessage('Turn on Location Services.');
        return;
      }

      LocationPermission permission = await Geolocator.checkPermission();

      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }

      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever) {
        _showMessage('Allow location access in Settings.');
        return;
      }

      final Position position = await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.high,
          timeLimit: Duration(seconds: 20),
        ),
      );

      final LatLng location = LatLng(position.latitude, position.longitude);

      if (!mounted) {
        return;
      }

      setState(() {
        _selectedLocation = location;
        _locationAllowed = true;
        _locationChosen = true;
        _selectedLabel = 'Current location';
      });

      await _mapController?.animateCamera(
        CameraUpdate.newCameraPosition(
          CameraPosition(target: location, zoom: 17),
        ),
      );
    } catch (e) {
      debugPrint('Current location error: $e');

      _showMessage('Couldn’t find your location.');
    } finally {
      if (mounted) setState(() => _locating = false);
    }
  }

  void _onCameraMove(CameraPosition position) {
    if (!mounted) {
      return;
    }

    // Updated on every frame: keep it out of setState. Only the boolean UI
    // state below needs a rebuild, and only when it actually transitions.
    _selectedLocation = position.target;

    var needsRebuild = false;

    if (!_moving) {
      _moving = true;
      needsRebuild = true;
    }

    if (_mapTouched && !_locationChosen) {
      _locationChosen = true;
      needsRebuild = true;
    }

    if (needsRebuild) {
      setState(() {});
    }
  }

  void _onCameraIdle() {
    if (!mounted || !_moving) {
      return;
    }

    setState(() {
      _moving = false;
    });
  }

  void _confirmLocation() {
    final Position position = Position(
      longitude: _selectedLocation.longitude,
      latitude: _selectedLocation.latitude,
      timestamp: DateTime.now(),
      accuracy: 0,
      altitude: 0,
      altitudeAccuracy: 0,
      heading: 0,
      headingAccuracy: 0,
      speed: 0,
      speedAccuracy: 0,
    );

    Navigator.of(
      context,
    ).pop(LocationSelection(position: position, label: _selectedLabel));
  }

  void _clearSearch() {
    _addressSearchRequest++;
    _placesSessionToken = null;
    _debounce?.cancel();
    _searchCancelToken?.cancel();
    _detailsCancelToken?.cancel();

    _searchController.clear();

    setState(() {
      _suggestions = [];
      _showSuggestions = false;
      _searching = false;
      _hasAutocompleteResult = false;
      _autocompleteUnavailable = false;
    });
  }

  void _showMessage(String message) {
    if (!mounted) {
      return;
    }

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
    final theme = Theme.of(context);
    final searchHeight = (MediaQuery.textScalerOf(context).scale(16) * 1.4 + 20)
        .clamp(54.0, double.infinity);
    final suggestionsTop =
        MediaQuery.paddingOf(context).top +
        SwiftAppBar.toolbarHeightFor(context) +
        searchHeight +
        24;

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      body: Stack(
        children: [
          Listener(
            onPointerDown: (_) {
              _mapTouched = true;
              _addressSearchRequest++;
              _placesSessionToken = null;
              _debounce?.cancel();
              _searchCancelToken?.cancel();
              _detailsCancelToken?.cancel();
              _searchFocusNode.unfocus();
              setState(() {
                _searchController.clear();
                _selectedLabel = null;
                _searching = false;
                _showSuggestions = false;
              });
            },
            child: GoogleMap(
              initialCameraPosition: CameraPosition(
                target: _selectedLocation,
                zoom: 17,
              ),
              style: AppGoogleMapStyle.forBrightness(theme.brightness),
              myLocationEnabled: _locationAllowed,
              myLocationButtonEnabled: false,
              zoomControlsEnabled: false,
              mapToolbarEnabled: false,
              compassEnabled: false,
              padding: const EdgeInsets.only(bottom: 218),
              onMapCreated: (controller) {
                _mapController = controller;
                controller.moveCamera(
                  CameraUpdate.newLatLng(_selectedLocation),
                );
              },
              onCameraMove: _onCameraMove,
              onCameraIdle: _onCameraIdle,
            ),
          ),

          SafeArea(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SwiftAppBar(
                    toolbarHeight: SwiftAppBar.toolbarHeightFor(context),
                    primary: false,
                    backgroundColor: theme.colorScheme.surface,
                    foregroundColor: theme.colorScheme.onSurface,
                    automaticallyImplyLeading: false,
                    leading: SwiftAppBarAction(
                      icon: SwiftIcons.chevronLeft,
                      onPressed: () => Navigator.of(context).pop(),
                      tooltip: 'Back',
                    ),
                    title: Text(
                      widget.fieldLabel == 'location'
                          ? 'Choose on map'
                          : 'Set ${widget.fieldLabel} point',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                  const SizedBox(height: 8),
                  SizedBox(
                    height: searchHeight,
                    child: _SearchContainer(
                      controller: _searchController,
                      focusNode: _searchFocusNode,
                      searching: _searching,
                      onChanged: _onSearchChanged,
                      onSubmitted: _submitSearch,
                      onTap: _showSearchSuggestions,
                      onClear: _clearSearch,
                    ),
                  ),
                ],
              ),
            ),
          ),

          if (_showSuggestions && _searchController.text.trim().isNotEmpty)
            Positioned(
              top: suggestionsTop,
              left: 16,
              right: 16,
              child: ConstrainedBox(
                constraints: BoxConstraints(
                  maxHeight:
                      (MediaQuery.sizeOf(context).height -
                              MediaQuery.viewInsetsOf(context).bottom -
                              suggestionsTop -
                              16)
                          .clamp(0.0, 320.0),
                ),
                child: _SuggestionsList(
                  query: _searchController.text.trim(),
                  suggestions: _suggestions,
                  searching: _searching,
                  showNoResults: _hasAutocompleteResult,
                  unavailable: _autocompleteUnavailable,
                  onSearch: _submitSearch,
                  onSelected: _selectSuggestion,
                ),
              ),
            ),

          Center(
            child: AnimatedScale(
              scale: _moving ? 1.08 : 1,
              duration: MediaQuery.disableAnimationsOf(context)
                  ? Duration.zero
                  : const Duration(milliseconds: 150),
              child: Padding(
                padding: EdgeInsets.only(bottom: 218),
                child: AppIcon(
                  SwiftIcons.mapPin,
                  size: 32,
                  color: Theme.of(context).colorScheme.primary,
                ),
              ),
            ),
          ),

          Positioned(
            right: 12,
            bottom: 228,
            child: _CircleButton(
              icon: SwiftIcons.locateFixed,
              tooltip: 'Use my current location',
              iconColor: Theme.of(context).colorScheme.primary,
              onTap: _moveToCurrentLocation,
            ),
          ),

          Positioned(
            left: 16,
            right: 16,
            bottom: 10,
            child: SafeArea(
              top: false,
              child: GlassSurface(
                borderRadius: AppRadii.sheetRadius,
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Row(
                      children: [
                        AppIcon(
                          SwiftIcons.mapPin,
                          color: Theme.of(context).colorScheme.primary,
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Text(
                            _selectedLabel?.trim().isNotEmpty == true
                                ? _selectedLabel!
                                : '${_selectedLocation.latitude.toStringAsFixed(5)}, '
                                      '${_selectedLocation.longitude.toStringAsFixed(5)}',
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.titleSmall?.copyWith(
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Text(
                      'Move the map to set your ${widget.fieldLabel}',
                      style: theme.textTheme.bodySmall,
                    ),
                    const SizedBox(height: 12),
                    SizedBox(
                      height: 52,
                      child: FilledButton(
                        onPressed:
                            !_locationChosen ||
                                _moving ||
                                _searching ||
                                _locating
                            ? null
                            : _confirmLocation,
                        style: FilledButton.styleFrom(
                          backgroundColor: Theme.of(
                            context,
                          ).colorScheme.primary,
                          elevation: 0,
                          shape: RoundedRectangleBorder(
                            borderRadius: AppRadii.button,
                          ),
                        ),
                        child: Text(
                          _locationChosen
                              ? widget.fieldLabel == 'location'
                                    ? 'Confirm location'
                                    : 'Confirm ${widget.fieldLabel}'
                              : 'Move the map',
                          style: Theme.of(context).textTheme.titleSmall
                              ?.copyWith(
                                color:
                                    _locationChosen &&
                                        !_moving &&
                                        !_searching &&
                                        !_locating
                                    ? theme.colorScheme.onPrimary
                                    : theme.colorScheme.onSurfaceVariant,
                                fontWeight: FontWeight.w600,
                              ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _SearchContainer extends StatelessWidget {
  final TextEditingController controller;
  final FocusNode focusNode;
  final bool searching;
  final ValueChanged<String> onChanged;
  final ValueChanged<String> onSubmitted;
  final VoidCallback onTap;
  final VoidCallback onClear;

  const _SearchContainer({
    required this.controller,
    required this.focusNode,
    required this.searching,
    required this.onChanged,
    required this.onSubmitted,
    required this.onTap,
    required this.onClear,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return GlassSurface(
      borderRadius: BorderRadius.circular(16),
      child: ConstrainedBox(
        constraints: BoxConstraints(
          minHeight: MediaQuery.textScalerOf(context).scale(13.5) * 1.4 + 18,
        ),
        child: Row(
          children: [
            Padding(
              padding: EdgeInsets.only(left: 14),
              child: AppIcon(
                SwiftIcons.search,
                size: 19,
                color: Theme.of(context).colorScheme.primary,
              ),
            ),
            const SizedBox(width: 9),
            Expanded(
              child: TextField(
                controller: controller,
                focusNode: focusNode,
                autofocus: true,
                onChanged: onChanged,
                onSubmitted: onSubmitted,
                onTap: onTap,
                keyboardType: TextInputType.streetAddress,
                textInputAction: TextInputAction.search,
                style: theme.textTheme.bodyMedium?.copyWith(
                  fontSize: 13.5,
                  fontWeight: FontWeight.w500,
                  color: theme.colorScheme.onSurface,
                ),
                decoration: InputDecoration(
                  border: InputBorder.none,
                  enabledBorder: InputBorder.none,
                  focusedBorder: InputBorder.none,
                  filled: false,
                  contentPadding: EdgeInsets.zero,
                  hintText: 'Search',
                  hintStyle: theme.textTheme.bodyMedium?.copyWith(
                    fontSize: 13.5,
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                  isDense: true,
                ),
              ),
            ),
            if (searching)
              SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: Theme.of(context).colorScheme.primary,
                ),
              )
            else if (controller.text.isNotEmpty)
              GestureDetector(
                onTap: onClear,
                child: Padding(
                  padding: EdgeInsets.all(6),
                  child: AppIcon(
                    SwiftIcons.circleX,
                    size: 18,
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
              ),
            const SizedBox(width: 8),
          ],
        ),
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
      return SingleChildScrollView(
        child: const GlassSurface(
          semanticLabel: 'Finding places',
          child: AppShimmer(
            child: Padding(
              padding: EdgeInsets.all(14),
              child: Column(
                children: [
                  Row(
                    children: [
                      ShimmerBlock(width: 34, height: 34, radius: 11),
                      SizedBox(width: 11),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            ShimmerBlock(width: 170, height: 13),
                            SizedBox(height: 7),
                            ShimmerBlock(width: double.infinity, height: 10),
                          ],
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: 15),
                  Row(
                    children: [
                      ShimmerBlock(width: 34, height: 34, radius: 11),
                      SizedBox(width: 11),
                      ShimmerBlock(width: 150, height: 13),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
      );
    }

    if (suggestions.isEmpty) {
      return SingleChildScrollView(
        child: GlassSurface(
          borderRadius: BorderRadius.circular(16),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(16),
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
                          unavailable
                              ? SwiftIcons.circleAlert
                              : SwiftIcons.search,
                          size: 16,
                          color: Theme.of(context).colorScheme.onSurfaceVariant,
                        ),
                        const SizedBox(width: 9),
                        Expanded(
                          child: Text(
                            unavailable
                                ? 'Suggestions unavailable'
                                : 'No places found',
                            style: Theme.of(context).textTheme.bodySmall,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      );
    }

    return GlassSurface(
      borderRadius: BorderRadius.circular(16),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(16),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxHeight: 320),
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

              final PlaceSuggestion suggestion = suggestions[index - 1];

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
                                  style: Theme.of(context).textTheme.bodySmall
                                      ?.copyWith(fontSize: 10.5),
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
      ),
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
                'Search “$query”',
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

class _CircleButton extends StatelessWidget {
  final SwiftIcons icon;
  final VoidCallback onTap;
  final Color iconColor;
  final String tooltip;

  const _CircleButton({
    required this.icon,
    required this.onTap,
    this.iconColor = const Color(0xFF202124),
    this.tooltip = 'Back',
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return GlassSurface(
      borderRadius: BorderRadius.circular(16),
      child: SizedBox(
        width: 46,
        height: 46,
        child: IconButton(
          onPressed: onTap,
          tooltip: tooltip,
          icon: AppIcon(
            icon,
            size: 20,
            color: iconColor == const Color(0xFF202124)
                ? theme.colorScheme.onSurface
                : iconColor,
          ),
        ),
      ),
    );
  }
}
