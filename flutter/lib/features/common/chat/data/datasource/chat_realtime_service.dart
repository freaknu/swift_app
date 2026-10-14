import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import 'package:swift_app/API/api_config.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/common/chat/domain/entity/chat_message.dart';

abstract interface class ChatRealtimeTransport {
  Stream<ChatMessage> get messages;

  Future<bool> send(ChatMessage message);

  void connect(int sessionId);

  void disconnect();
}

/// STOMP transport for the chat topic exposed by websocket-service.
class ChatRealtimeService implements ChatRealtimeTransport {
  final AuthLocalDatasource auth;
  final StreamController<ChatMessage> _messages =
      StreamController<ChatMessage>.broadcast();
  StompClient? _client;
  int? _sessionId;

  ChatRealtimeService({required this.auth});

  @override
  Stream<ChatMessage> get messages => _messages.stream;

  @override
  void connect(int sessionId) {
    if (_sessionId == sessionId && _client?.connected == true) return;
    disconnect();
    _sessionId = sessionId;
    final token = auth.accessToken;
    final headers = <String, String>{
      if (auth.userId case final userId?) 'x-user-id': userId.toString(),
      if (token != null && token.isNotEmpty) 'Authorization': 'Bearer $token',
    };
    _client = StompClient(
      config: StompConfig(
        url: ApiConfig.webSocketUrl,
        stompConnectHeaders: headers,
        webSocketConnectHeaders: headers,
        reconnectDelay: const Duration(seconds: 3),
        onConnect: (_) => _subscribe(sessionId),
        onWebSocketError: (error) => debugPrint('Chat websocket error: $error'),
        onStompError: (frame) => debugPrint('Chat STOMP error: ${frame.body}'),
      ),
    );
    _client!.activate();
  }

  @override
  Future<bool> send(ChatMessage message) async {
    for (var attempt = 0; attempt < 20; attempt++) {
      if (_client?.connected == true) break;
      await Future<void>.delayed(const Duration(milliseconds: 250));
    }
    final client = _client;
    if (client == null || client.connected != true) return false;
    client.send(
      destination: message.messageType == 'REPLY'
          ? '/app/chat.reply'
          : '/app/chat.send',
      headers: const {'content-type': 'application/json'},
      body: jsonEncode(message.toJson()),
    );
    return true;
  }

  @override
  void disconnect() {
    _client?.deactivate();
    _client = null;
    _sessionId = null;
  }

  Future<void> dispose() async {
    disconnect();
    await _messages.close();
  }

  void _subscribe(int sessionId) {
    final client = _client;
    if (client == null) return;
    client.subscribe(
      destination: '/topic/chat/session/$sessionId',
      callback: _onFrame,
    );
  }

  void _onFrame(StompFrame frame) {
    final body = frame.body;
    if (body == null || body.isEmpty) return;
    try {
      final decoded = jsonDecode(body);
      if (decoded is Map) {
        final message = ChatMessage.fromJson(
          Map<String, dynamic>.from(decoded),
        );
        if (message.messageId.isNotEmpty) _messages.add(message);
      }
    } catch (error) {
      debugPrint('Unable to parse chat message: $error');
    }
  }
}
