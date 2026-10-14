import 'dart:async';
import 'dart:convert';

import 'package:swift_app/API/api_config.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/driver/data/model/realtime_ride_event.dart';
import 'package:flutter/foundation.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';

class DriverRealtimeService {
  final AuthLocalDatasource _authLocalDatasource;
  final StreamController<RealtimeRideEvent> _rideEvents =
      StreamController<RealtimeRideEvent>.broadcast();
  final ValueNotifier<bool> connected = ValueNotifier<bool>(false);

  StompClient? _client;
  int? _driverId;

  DriverRealtimeService({required AuthLocalDatasource authLocalDatasource})
    : _authLocalDatasource = authLocalDatasource;

  Stream<RealtimeRideEvent> get rideEvents => _rideEvents.stream;

  void connect({required int driverId}) {
    if (_driverId == driverId && _client?.connected == true) {
      return;
    }

    disconnect();
    _driverId = driverId;

    final token = _authLocalDatasource.accessToken;
    final headers = <String, String>{
      'x-user-id': _authLocalDatasource.userId?.toString() ?? '',
      if (token != null && token.isNotEmpty) 'Authorization': 'Bearer $token',
    };

    _client = StompClient(
      config: StompConfig(
        url: ApiConfig.webSocketUrl,
        stompConnectHeaders: headers,
        webSocketConnectHeaders: headers,
        reconnectDelay: const Duration(seconds: 3),
        onConnect: (frame) {
          connected.value = true;
          _subscribe(driverId);
        },
        onWebSocketDone: () => connected.value = false,
        onWebSocketError: (error) {
          connected.value = false;
          debugPrint('Driver websocket error: $error');
        },
        onStompError: (frame) {
          connected.value = false;
          debugPrint('Driver STOMP error: ${frame.body}');
        },
      ),
    );

    _client?.activate();
  }

  /// Sends an accepted-ride GPS fix through the WebSocket/Kafka pipeline.
  /// Availability fixes continue to use the location REST endpoint because
  /// they feed the nearby-driver geo index.
  Future<bool> publishLocation({
    required int driverId,
    required int rideId,
    required double latitude,
    required double longitude,
  }) async {
    for (var attempt = 0; attempt < 20; attempt++) {
      if (_client?.connected == true && connected.value) break;
      await Future<void>.delayed(const Duration(milliseconds: 250));
    }

    final client = _client;
    if (client == null || client.connected != true || !connected.value) {
      return false;
    }

    client.send(
      destination: '/app/location',
      headers: const {'content-type': 'application/json'},
      body: jsonEncode({
        'driverId': driverId,
        'rideId': rideId,
        'latitude': latitude,
        'longitude': longitude,
      }),
    );
    return true;
  }

  void disconnect() {
    connected.value = false;
    _client?.deactivate();
    _client = null;
    _driverId = null;
  }

  Future<void> dispose() async {
    disconnect();
    await _rideEvents.close();
    connected.dispose();
  }

  void _subscribe(int driverId) {
    final client = _client;
    if (client == null) return;

    client.subscribe(
      destination: '/topic/drivers/$driverId',
      callback: _handleFrame,
    );
  }

  void _handleFrame(StompFrame frame) {
    final body = frame.body;
    if (body == null || body.isEmpty) return;

    try {
      final decoded = jsonDecode(body);
      if (decoded is! Map<String, dynamic>) return;

      _rideEvents.add(RealtimeRideEvent.fromJson(decoded));
    } catch (e) {
      debugPrint('Unable to parse realtime ride event: $e');
    }
  }
}
