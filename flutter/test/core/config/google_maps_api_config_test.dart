import 'package:swift_app/core/config/google_maps_api_config.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('Android Places headers include the app and signing certificate', () {
    const config = GoogleMapsApiConfig(
      apiKey: 'test-key',
      platform: 'android',
      applicationId: 'com.example.app',
      certificateSha1: 'ABC123',
    );

    expect(config.headersFor('location'), {
      'Content-Type': 'application/json',
      'X-Goog-Api-Key': 'test-key',
      'X-Goog-FieldMask': 'location',
      'X-Android-Package': 'com.example.app',
      'X-Android-Cert': 'ABC123',
    });
  });

  test('iOS Places headers include the bundle identifier', () {
    const config = GoogleMapsApiConfig(
      apiKey: 'test-key',
      platform: 'ios',
      applicationId: 'com.example.app',
    );

    expect(config.headersFor('location'), {
      'Content-Type': 'application/json',
      'X-Goog-Api-Key': 'test-key',
      'X-Goog-FieldMask': 'location',
      'X-Ios-Bundle-Identifier': 'com.example.app',
    });
  });
}
