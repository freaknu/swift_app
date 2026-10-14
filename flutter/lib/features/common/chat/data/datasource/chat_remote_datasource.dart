import 'dart:developer' as developer;

import 'package:dio/dio.dart';
import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/features/common/chat/domain/entity/chat_message.dart';
import 'package:swift_app/features/common/chat/domain/entity/chat_session.dart';
import 'package:shared_preferences/shared_preferences.dart';

abstract interface class ChatRemoteService {
  Future<ChatSession?> getSessionByRide(int rideId);

  Future<List<ChatMessage>> getMessages(int sessionId);

  Future<void> sendMessage(ChatMessage message);
}

class ChatApiException implements Exception {
  final int statusCode;
  final String message;

  const ChatApiException(this.statusCode, this.message);

  @override
  String toString() => message;
}

/// REST adapter for chat-service. Messages are persisted by chat-service first;
/// its Kafka event is then fanned out by websocket-service and notification-
/// service. The chat service returns raw JSON, unlike ride APIs which use
/// [ApiResponseFormat].
class ChatRemoteDatasource implements ChatRemoteService {
  final DioClient dioClient;
  final RiderApiEndpoints endpoints;

  const ChatRemoteDatasource({
    required this.dioClient,
    required this.endpoints,
  });

  @override
  Future<ChatSession?> getSessionByRide(int rideId) async {
    final endpoint = endpoints.chatSessionByRide(rideId);
    final response = await dioClient.dio.get(endpoint);
    if (response.statusCode == 404) return null;
    _checkStatus(response, endpoint);
    final map = _map(response.data);
    return ChatSession.fromJson(map);
  }

  @override
  Future<List<ChatMessage>> getMessages(int sessionId) async {
    final endpoint = endpoints.chatMessages(sessionId);
    final response = await dioClient.dio.get(endpoint);
    _checkStatus(response, endpoint);
    if (response.data is! List) {
      throw ChatApiException(
        response.statusCode ?? 500,
        'Invalid chat history',
      );
    }
    return (response.data as List)
        .whereType<Map>()
        .map((item) => ChatMessage.fromJson(Map<String, dynamic>.from(item)))
        .where((message) => message.messageId.isNotEmpty)
        .toList()
        .reversed
        .toList();
  }

  @override
  Future<void> sendMessage(ChatMessage message) async {
    final sessionId = message.sessionId;
    if (sessionId == null) {
      throw const ChatApiException(400, 'Chat session is missing');
    }
    final endpoint = endpoints.chatMessageCreate(sessionId);
    // Keep the REST request limited to the server-owned write contract. The
    // same model is also used by STOMP and contains read/transport fields
    // that should never be trusted by chat-service.
    final response = await dioClient.dio.post(
      endpoint,
      data: {
        'messageId': message.messageId,
        'messageContent': message.messageContent,
        'messageUrl': message.messageUrl,
        'contentType': message.contentType,
        'replyOfMessageId': message.replyOfMessageId,
      },
    );
    _checkStatus(response, endpoint);
  }

  void _checkStatus(Response<dynamic> response, String endpoint) {
    final code = response.statusCode ?? 500;
    if (code < 200 || code >= 300) {
      developer.log(
        'Chat request failed [$endpoint] status=$code',
        name: 'ChatRemoteDatasource',
      );
      throw ChatApiException(code, 'Chat service unavailable right now.');
    }
  }

  Map<String, dynamic> _map(dynamic value) {
    if (value is Map<String, dynamic>) return value;
    if (value is Map) return Map<String, dynamic>.from(value);
    throw const ChatApiException(500, 'Invalid chat session');
  }
}

/// Sends an Android notification quick reply from a local-notification
/// background isolate. It reads the already persisted auth session and never
/// accepts credentials from notification data.
Future<void> sendChatNotificationReply({
  required Map<String, dynamic> payload,
  required String text,
  ChatRemoteService? datasource,
  int? authenticatedUserId,
}) async {
  final preferences = datasource == null
      ? await SharedPreferences.getInstance()
      : null;
  final userId = authenticatedUserId ?? preferences?.getInt('auth.userId');
  final token = preferences?.getString('auth.accessToken');
  if (userId == null ||
      (datasource == null && (token == null || token.isEmpty))) {
    return;
  }

  final client =
      datasource ??
      ChatRemoteDatasource(
        dioClient: DioClient(
          accessTokenReader: () async => token,
          authUserIdReader: () async => userId,
        ),
        endpoints: RiderApiEndpoints(),
      );
  var sessionId = _asInt(payload['sessionId']);
  final rideId = _asInt(payload['rideId']);
  if (sessionId == null && rideId != null) {
    sessionId = (await client.getSessionByRide(rideId))?.sessionId;
  }
  if (sessionId == null) return;

  await client.sendMessage(
    ChatMessage.create(
      sentById: userId,
      sessionId: sessionId,
      content: text,
      // The notification identifies the message being answered. Fall back to
      // its parent only for older payloads that did not include messageId.
      replyOfMessageId:
          payload['messageId']?.toString() ??
          payload['replyOfMessageId']?.toString(),
    ),
  );
}

int? _asInt(dynamic value) {
  if (value is num) return value.toInt();
  return int.tryParse(value?.toString() ?? '');
}
