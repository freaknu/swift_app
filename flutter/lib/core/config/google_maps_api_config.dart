import 'package:flutter/services.dart';

class GoogleMapsApiConfig {
  static const MethodChannel _channel = MethodChannel('swift_app/maps_config');
  static const String _dartDefineKey = String.fromEnvironment(
    'GOOGLE_PLACES_API_KEY',
  );

  final String apiKey;
  final String platform;
  final String applicationId;
  final String certificateSha1;

  const GoogleMapsApiConfig({
    this.apiKey = '',
    this.platform = '',
    this.applicationId = '',
    this.certificateSha1 = '',
  });

  bool get isConfigured => apiKey.isNotEmpty;

  static Future<GoogleMapsApiConfig> load() async {
    Map<dynamic, dynamic>? nativeConfig;
    try {
      nativeConfig = await _channel.invokeMapMethod<dynamic, dynamic>(
        'getApiKey',
      );
    } on MissingPluginException {
      // A dart-define can still configure unsupported/test platforms.
    } on PlatformException {
      // Manual map selection remains usable if native configuration fails.
    }

    String value(String key) => nativeConfig?[key]?.toString().trim() ?? '';

    final configuredKey = _dartDefineKey.isNotEmpty
        ? _dartDefineKey
        : value('apiKey');
    final apiKey =
        configuredKey == 'DEFAULT_API_KEY' || configuredKey.startsWith(r'$(')
        ? ''
        : configuredKey;

    return GoogleMapsApiConfig(
      apiKey: apiKey,
      platform: value('platform'),
      applicationId: value('applicationId'),
      certificateSha1: value('certificateSha1'),
    );
  }

  Map<String, String> headersFor(String fieldMask) {
    final headers = <String, String>{
      'Content-Type': 'application/json',
      'X-Goog-Api-Key': apiKey,
      'X-Goog-FieldMask': fieldMask,
    };

    if (platform == 'android' && applicationId.isNotEmpty) {
      headers['X-Android-Package'] = applicationId;
      if (certificateSha1.isNotEmpty) {
        headers['X-Android-Cert'] = certificateSha1;
      }
    } else if (platform == 'ios' && applicationId.isNotEmpty) {
      headers['X-Ios-Bundle-Identifier'] = applicationId;
    }

    return headers;
  }
}
