import 'package:uuid/uuid.dart';

class ChatMessage {
  final String messageId;
  final int? sentById;
  final String messageContent;
  final int? sessionId;
  final String? messageUrl;
  final String? replyOfMessageId;
  final String contentType;
  final String messageType;
  final String? readByIds;
  final bool edited;
  final bool deleted;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const ChatMessage({
    required this.messageId,
    required this.messageContent,
    this.sentById,
    this.sessionId,
    this.messageUrl,
    this.replyOfMessageId,
    this.contentType = 'TEXT',
    this.messageType = 'MESSAGE',
    this.readByIds,
    this.edited = false,
    this.deleted = false,
    this.createdAt,
    this.updatedAt,
  });

  factory ChatMessage.create({
    required int sentById,
    required int sessionId,
    required String content,
    String? replyOfMessageId,
  }) {
    return ChatMessage(
      messageId: const Uuid().v4(),
      sentById: sentById,
      messageContent: content,
      sessionId: sessionId,
      replyOfMessageId: replyOfMessageId,
      messageType: replyOfMessageId == null ? 'MESSAGE' : 'REPLY',
    );
  }

  factory ChatMessage.fromJson(Map<String, dynamic> json) {
    return ChatMessage(
      messageId: json['messageId']?.toString() ?? '',
      sentById: _asInt(json['sentById']),
      messageContent: json['messageContent']?.toString() ?? '',
      sessionId: _asInt(json['sessionId']),
      messageUrl: json['messageUrl']?.toString(),
      replyOfMessageId: json['replyOfMessageId']?.toString(),
      contentType: json['contentType']?.toString() ?? 'TEXT',
      messageType: json['messageType']?.toString() ?? 'MESSAGE',
      readByIds: json['readByIds']?.toString(),
      edited: _asBool(json['edited']),
      deleted: _asBool(json['deleted']),
      createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? ''),
      updatedAt: DateTime.tryParse(json['updatedAt']?.toString() ?? ''),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'messageId': messageId,
      'sentById': sentById,
      'messageContent': messageContent,
      'sessionId': sessionId,
      'messageUrl': messageUrl,
      'replyOfMessageId': replyOfMessageId,
      'contentType': contentType,
      'messageType': messageType,
      'readByIds': readByIds,
      'edited': edited,
      'deleted': deleted,
    };
  }

  ChatMessage copyWith({
    String? messageContent,
    bool? edited,
    bool? deleted,
    DateTime? createdAt,
    DateTime? updatedAt,
  }) {
    return ChatMessage(
      messageId: messageId,
      sentById: sentById,
      messageContent: messageContent ?? this.messageContent,
      sessionId: sessionId,
      messageUrl: messageUrl,
      replyOfMessageId: replyOfMessageId,
      contentType: contentType,
      messageType: messageType,
      readByIds: readByIds,
      edited: edited ?? this.edited,
      deleted: deleted ?? this.deleted,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  static int? _asInt(dynamic value) {
    if (value is num) return value.toInt();
    return int.tryParse(value?.toString() ?? '');
  }

  static bool _asBool(dynamic value) {
    if (value is bool) return value;
    return value?.toString().toLowerCase() == 'true';
  }
}
