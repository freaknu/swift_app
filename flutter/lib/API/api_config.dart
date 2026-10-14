class ApiConfig {
  static const _endpoints = ApiEndpointConfig(
    apiBaseUrl: String.fromEnvironment('API_BASE_URL'),
    webSocketUrl: String.fromEnvironment('WS_URL'),
    apiHost: String.fromEnvironment('API_HOST'),
  );

  static String get baseUrl => _endpoints.baseUrl;

  static String get webSocketUrl => _endpoints.webSocketUrl;
}

/// Resolves explicit build settings while keeping the hosted gateway default.
class ApiEndpointConfig {
  const ApiEndpointConfig({
    String apiBaseUrl = '',
    String webSocketUrl = '',
    String apiHost = '',
  }) : _apiBaseUrl = apiBaseUrl,
       _webSocketUrl = webSocketUrl,
       _apiHost = apiHost;

  final String _apiBaseUrl;
  final String _webSocketUrl;
  final String _apiHost;

  String get baseUrl {
    if (_apiBaseUrl.trim().isNotEmpty) {
      return _withTrailingSlash(_apiBaseUrl.trim());
    }

    if (_apiHost.trim().isNotEmpty) {
      return Uri(
        scheme: 'http',
        host: _apiHost.trim(),
        port: 8080,
        path: '/',
      ).toString();
    }

    return 'https://swift-app.duckdns.org/';
  }

  String get webSocketUrl {
    if (_webSocketUrl.trim().isNotEmpty) {
      return _webSocketUrl.trim();
    }

    final uri = Uri.parse(baseUrl);
    final scheme = uri.scheme == 'https' ? 'wss' : 'ws';

    return Uri(
      scheme: scheme,
      userInfo: uri.userInfo,
      host: uri.host,
      port: uri.hasPort ? uri.port : null,
      path: '/ws-native',
    ).toString();
  }

  static String _withTrailingSlash(String value) {
    final uri = Uri.parse(value);
    return uri
        .replace(path: uri.path.endsWith('/') ? uri.path : '${uri.path}/')
        .toString();
  }
}
