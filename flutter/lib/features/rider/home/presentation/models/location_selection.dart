import 'package:geolocator/geolocator.dart';

class LocationSelection {
  final Position position;
  final String? label;

  const LocationSelection({required this.position, this.label});

  factory LocationSelection.fromCoordinates({
    required double latitude,
    required double longitude,
    String? label,
  }) => LocationSelection(
    position: Position(
      latitude: latitude,
      longitude: longitude,
      timestamp: DateTime.now(),
      accuracy: 0,
      altitude: 0,
      altitudeAccuracy: 0,
      heading: 0,
      headingAccuracy: 0,
      speed: 0,
      speedAccuracy: 0,
    ),
    label: label,
  );
}

class TripLocations {
  final LocationSelection pickup;
  final LocationSelection destination;

  const TripLocations({required this.pickup, required this.destination});
}
