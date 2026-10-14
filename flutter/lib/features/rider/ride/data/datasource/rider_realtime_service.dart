import 'dart:async';
import 'dart:convert';

import 'package:swift_app/API/api_config.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:flutter/foundation.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';

/// Realtime feed for the rider's active ride.
///
/// The websocket service pushes matching search progress
/// (`SEARCHING` with attempt / radiusKm / driverCount) and lifecycle
/// events (`DRIVER_ASSIGNED`, ...) to `/topic/rides/{rideId}` and
/// `/topic/riders/{riderId}`. Accepted-driver GPS fixes use the same ride
/// topic and include a calculated pickup ETA. The REST poll on the status
/// page stays as the fallback for dropped frames.
class RiderRealtimeService {
  final AuthLocalDatasource _authLocalDatasource;
  final StreamController<Map<String, dynamic>> _events =
      StreamController<Map<String, dynamic>>.broadcast();

  StompClient? _client;
  int? _rideId;
  final ValueNotifier<bool> connected = ValueNotifier<bool>(false);

  RiderRealtimeService({required AuthLocalDatasource authLocalDatasource})
    : _authLocalDatasource = authLocalDatasource;

  Stream<Map<String, dynamic>> get events => _events.stream;

  void connect({required int rideId, required int riderId}) {
    if (_rideId == rideId && _client?.connected == true) {
      return;
    }

    disconnect();
    _rideId = rideId;

    final token = _authLocalDatasource.accessToken;
    final headers = <String, String>{
      if (_authLocalDatasource.userId case final authUserId?)
        'x-user-id': authUserId.toString(),
      if (token != null && token.isNotEmpty) 'Authorization': 'Bearer $token',
    };

    _client = StompClient(
      config: StompConfig(
        url: ApiConfig.webSocketUrl,
        stompConnectHeaders: headers,
        webSocketConnectHeaders: headers,
        reconnectDelay: const Duration(seconds: 3),
        onConnect: (frame) {
          _subscribe(rideId, riderId);
          connected.value = true;
        },
        onWebSocketDone: () {
          connected.value = false;
          debugPrint('Rider websocket disconnected; reconnecting');
        },
        onWebSocketError: (error) {
          connected.value = false;
          debugPrint('Rider websocket error: $error');
        },
        onStompError: (frame) {
          connected.value = false;
          debugPrint('Rider STOMP error: ${frame.body}');
        },
      ),
    );

    _client?.activate();
  }

  void disconnect() {
    connected.value = false;
    _client?.deactivate();
    _client = null;
    _rideId = null;
  }

  Future<void> dispose() async {
    disconnect();
    await _events.close();
    connected.dispose();
  }

  void _subscribe(int rideId, int riderId) {
    final client = _client;
    if (client == null) return;

    client.subscribe(
      destination: '/topic/rides/$rideId',
      callback: _handleFrame,
    );
    client.subscribe(
      destination: '/topic/riders/$riderId',
      callback: _handleFrame,
    );
  }

  void _handleFrame(StompFrame frame) {
    final body = frame.body;
    if (body == null || body.isEmpty) return;

    try {
      final decoded = jsonDecode(body);
      if (decoded is! Map<String, dynamic>) return;

      _events.add(decoded);
    } catch (e) {
      debugPrint('Unable to parse rider realtime event: $e');
    }
  }
}
