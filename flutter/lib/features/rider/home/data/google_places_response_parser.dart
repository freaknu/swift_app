class PlaceSuggestion {
  final String placeId;
  final String mainText;
  final String secondaryText;
  final String fullText;

  const PlaceSuggestion({
    required this.placeId,
    required this.mainText,
    required this.secondaryText,
    required this.fullText,
  });
}

class PlaceCoordinates {
  final double latitude;
  final double longitude;

  const PlaceCoordinates({required this.latitude, required this.longitude});
}

List<PlaceSuggestion> parseAutocompleteResponse(dynamic response) {
  if (response is! Map<String, dynamic>) {
    return const [];
  }

  final dynamic rawSuggestions = response['suggestions'];
  if (rawSuggestions is! List) {
    return const [];
  }

  final results = <PlaceSuggestion>[];
  for (final dynamic item in rawSuggestions) {
    if (item is! Map<String, dynamic>) {
      continue;
    }

    final dynamic prediction = item['placePrediction'];
    if (prediction is! Map<String, dynamic>) {
      continue;
    }

    final String placeId = prediction['placeId']?.toString() ?? '';
    final dynamic text = prediction['text'];
    final dynamic structured = prediction['structuredFormat'];

    String fullText = '';
    String mainText = '';
    String secondaryText = '';

    if (text is Map<String, dynamic>) {
      fullText = text['text']?.toString() ?? '';
    }

    if (structured is Map<String, dynamic>) {
      final dynamic main = structured['mainText'];
      final dynamic secondary = structured['secondaryText'];

      if (main is Map<String, dynamic>) {
        mainText = main['text']?.toString() ?? '';
      }
      if (secondary is Map<String, dynamic>) {
        secondaryText = secondary['text']?.toString() ?? '';
      }
    }

    if (mainText.isEmpty) {
      mainText = fullText;
    }

    if (placeId.isEmpty || mainText.isEmpty) {
      continue;
    }

    results.add(
      PlaceSuggestion(
        placeId: placeId,
        mainText: mainText,
        secondaryText: secondaryText,
        fullText: fullText,
      ),
    );
  }

  return results;
}

PlaceCoordinates? parsePlaceDetailsResponse(dynamic response) {
  if (response is! Map<String, dynamic>) {
    return null;
  }

  final dynamic location = response['location'];
  if (location is! Map<String, dynamic>) {
    return null;
  }

  final dynamic rawLatitude = location['latitude'];
  final dynamic rawLongitude = location['longitude'];
  if (rawLatitude is! num || rawLongitude is! num) {
    return null;
  }

  return PlaceCoordinates(
    latitude: rawLatitude.toDouble(),
    longitude: rawLongitude.toDouble(),
  );
}
