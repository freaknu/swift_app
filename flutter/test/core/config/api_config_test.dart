import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/API/api_config.dart';

void main() {
  test('hosted defaults use HTTPS and native WSS without a local port', () {
    const config = ApiEndpointConfig();

    expect(config.baseUrl, 'https://swift-app.duckdns.org/');
    expect(config.webSocketUrl, 'wss://swift-app.duckdns.org/ws-native');
  });

  test('empty and whitespace-only overrides keep hosted defaults', () {
    const config = ApiEndpointConfig(
      apiBaseUrl: '  ',
      webSocketUrl: '\n',
      apiHost: ' ',
    );

    expect(config.baseUrl, 'https://swift-app.duckdns.org/');
    expect(config.webSocketUrl, 'wss://swift-app.duckdns.org/ws-native');
  });

  test('an explicit local host enables HTTP and WS on port 8080', () {
    const config = ApiEndpointConfig(apiHost: ' 10.0.2.2 ');

    expect(config.baseUrl, 'http://10.0.2.2:8080/');
    expect(config.webSocketUrl, 'ws://10.0.2.2:8080/ws-native');
  });

  test('an IPv6 local host keeps the URL authority valid', () {
    const config = ApiEndpointConfig(apiHost: '::1');

    expect(config.baseUrl, 'http://[::1]:8080/');
    expect(config.webSocketUrl, 'ws://[::1]:8080/ws-native');
  });

  test('a complete base URL takes precedence over the local host', () {
    const config = ApiEndpointConfig(
      apiBaseUrl: ' https://gateway.example.test ',
      apiHost: '10.0.2.2',
    );

    expect(config.baseUrl, 'https://gateway.example.test/');
    expect(config.webSocketUrl, 'wss://gateway.example.test/ws-native');
  });

  test('HTTP base override keeps its explicit port for the native socket', () {
    const config = ApiEndpointConfig(
      apiBaseUrl: 'http://localhost:18080/backend/',
    );

    expect(config.baseUrl, 'http://localhost:18080/backend/');
    expect(config.webSocketUrl, 'ws://localhost:18080/ws-native');
  });

  test('HTTPS base override keeps its explicit port and uses WSS', () {
    const config = ApiEndpointConfig(
      apiBaseUrl: 'https://gateway.example.test:18443/api',
    );

    expect(config.baseUrl, 'https://gateway.example.test:18443/api/');
    expect(config.webSocketUrl, 'wss://gateway.example.test:18443/ws-native');
  });

  test(
    'the base URL slash belongs to the path, ahead of query and fragment',
    () {
      const config = ApiEndpointConfig(
        apiBaseUrl: 'https://gateway.example.test/api?version=1#section',
      );

      expect(
        config.baseUrl,
        'https://gateway.example.test/api/?version=1#section',
      );
      expect(config.webSocketUrl, 'wss://gateway.example.test/ws-native');
    },
  );

  test('a trailing slash is not duplicated', () {
    const config = ApiEndpointConfig(
      apiBaseUrl: 'https://gateway.example.test/api/',
    );

    expect(config.baseUrl, 'https://gateway.example.test/api/');
  });

  test('explicit WebSocket URL takes precedence without changing HTTP', () {
    const config = ApiEndpointConfig(
      apiBaseUrl: 'https://gateway.example.test',
      apiHost: '10.0.2.2',
      webSocketUrl: ' wss://realtime.example.test:9443/native?transport=stomp ',
    );

    expect(config.baseUrl, 'https://gateway.example.test/');
    expect(
      config.webSocketUrl,
      'wss://realtime.example.test:9443/native?transport=stomp',
    );
  });

  test('a socket-only override retains the hosted HTTP default', () {
    const config = ApiEndpointConfig(
      webSocketUrl: 'ws://localhost:18080/custom-native',
    );

    expect(config.baseUrl, 'https://swift-app.duckdns.org/');
    expect(config.webSocketUrl, 'ws://localhost:18080/custom-native');
  });

  test('static API config applies the build-time endpoint settings', () {
    const expected = ApiEndpointConfig(
      apiBaseUrl: String.fromEnvironment('API_BASE_URL'),
      webSocketUrl: String.fromEnvironment('WS_URL'),
      apiHost: String.fromEnvironment('API_HOST'),
    );

    expect(ApiConfig.baseUrl, expected.baseUrl);
    expect(ApiConfig.webSocketUrl, expected.webSocketUrl);
  });
}
