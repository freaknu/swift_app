import 'package:swift_app/features/rider/home/data/google_places_response_parser.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('parseAutocompleteResponse', () {
    test('parses structured suggestions and falls back to full text', () {
      final results = parseAutocompleteResponse({
        'suggestions': [
          {
            'placePrediction': {
              'placeId': 'place-1',
              'text': {'text': 'Cubbon Park, Bengaluru, Karnataka'},
              'structuredFormat': {
                'mainText': {'text': 'Cubbon Park'},
                'secondaryText': {'text': 'Bengaluru, Karnataka'},
              },
            },
          },
          {
            'placePrediction': {
              'placeId': 'place-2',
              'text': {'text': 'Bengaluru Airport'},
            },
          },
        ],
      });

      expect(results, hasLength(2));
      expect(results.first.placeId, 'place-1');
      expect(results.first.mainText, 'Cubbon Park');
      expect(results.first.secondaryText, 'Bengaluru, Karnataka');
      expect(results.first.fullText, 'Cubbon Park, Bengaluru, Karnataka');
      expect(results.last.placeId, 'place-2');
      expect(results.last.mainText, 'Bengaluru Airport');
      expect(results.last.secondaryText, isEmpty);
    });

    test('ignores malformed and unusable suggestions', () {
      final results = parseAutocompleteResponse({
        'suggestions': [
          null,
          'not a suggestion',
          <String, dynamic>{},
          {
            'placePrediction': {
              'placeId': '',
              'text': {'text': 'Missing ID'},
            },
          },
          {
            'placePrediction': {
              'placeId': 'missing-text',
              'text': <String, dynamic>{},
            },
          },
        ],
      });

      expect(results, isEmpty);
      expect(parseAutocompleteResponse(null), isEmpty);
      expect(parseAutocompleteResponse(<String, dynamic>{}), isEmpty);
      expect(parseAutocompleteResponse({'suggestions': 'not a list'}), isEmpty);
    });
  });

  group('parsePlaceDetailsResponse', () {
    test('converts numeric coordinates to doubles', () {
      final result = parsePlaceDetailsResponse({
        'location': {'latitude': 12, 'longitude': 77.5946},
      });

      expect(result, isNotNull);
      expect(result!.latitude, 12.0);
      expect(result.longitude, 77.5946);
    });

    test('returns null for malformed or incomplete details', () {
      expect(parsePlaceDetailsResponse(null), isNull);
      expect(parsePlaceDetailsResponse(<String, dynamic>{}), isNull);
      expect(
        parsePlaceDetailsResponse({'location': 'not coordinates'}),
        isNull,
      );
      expect(
        parsePlaceDetailsResponse({
          'location': {'latitude': 12.9716},
        }),
        isNull,
      );
      expect(
        parsePlaceDetailsResponse({
          'location': {'latitude': '12.9716', 'longitude': 77.5946},
        }),
        isNull,
      );
    });
  });
}
