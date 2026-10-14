import 'package:dio/dio.dart';

class InboxNotification {
  const InboxNotification({
    required this.id,
    required this.type,
    required this.title,
    required this.body,
    required this.read,
    this.rideId,
    this.sessionId,
    this.messageId,
    this.createdAt,
  });

  final int id;
  final int? rideId;
  final int? sessionId;
  final String? messageId;
  final String type;
  final String title;
  final String body;
  final bool read;
  final DateTime? createdAt;

  InboxNotification markRead() => InboxNotification(
    id: id,
    rideId: rideId,
    sessionId: sessionId,
    messageId: messageId,
    type: type,
    title: title,
    body: body,
    read: true,
    createdAt: createdAt,
  );

  factory InboxNotification.fromJson(Map<String, dynamic> json) {
    final id = int.tryParse(json['id']?.toString() ?? '');
    if (id == null) throw const FormatException('Notification ID is missing');
    return InboxNotification(
      id: id,
      rideId: int.tryParse(json['rideId']?.toString() ?? ''),
      sessionId: int.tryParse(json['sessionId']?.toString() ?? ''),
      messageId: json['messageId']?.toString(),
      type: json['type']?.toString() ?? 'RIDE_UPDATE',
      title: json['title']?.toString() ?? 'Ride update',
      body: json['body']?.toString() ?? '',
      read: json['read'] == true,
      createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? ''),
    );
  }
}

class NotificationInboxException implements Exception {
  const NotificationInboxException(this.message);
  final String message;

  @override
  String toString() => message;
}

class NotificationInboxDatasource {
  const NotificationInboxDatasource(this.dio);

  final Dio dio;

  Future<List<InboxNotification>> fetch(String role) async {
    final data = await _data(
      () => dio.get<dynamic>(
        'api/v1/notifications',
        queryParameters: {'role': role.toUpperCase(), 'limit': 50},
      ),
    );
    if (data is! List) {
      throw const NotificationInboxException('Invalid notification response');
    }
    return data
        .map(
          (item) => InboxNotification.fromJson(
            Map<String, dynamic>.from(item as Map),
          ),
        )
        .toList();
  }

  Future<int> unreadCount(String role) async {
    final data = await _data(
      () => dio.get<dynamic>(
        'api/v1/notifications/unread-count',
        queryParameters: {'role': role.toUpperCase()},
      ),
    );
    final count = int.tryParse(data?.toString() ?? '');
    if (count == null) {
      throw const NotificationInboxException('Invalid unread count');
    }
    return count;
  }

  Future<void> markRead(int id, String role) async {
    await _data(
      () => dio.patch<dynamic>(
        'api/v1/notifications/$id/read',
        queryParameters: {'role': role.toUpperCase()},
      ),
    );
  }

  Future<dynamic> _data(Future<Response<dynamic>> Function() request) async {
    try {
      final response = await request();
      final payload = response.data;
      if (response.statusCode != null &&
          response.statusCode! >= 200 &&
          response.statusCode! < 300 &&
          payload is Map &&
          payload['success'] == true) {
        return payload['data'];
      }
      final message = payload is Map ? payload['message']?.toString() : null;
      throw NotificationInboxException(
        message != null && message.isNotEmpty
            ? message
            : 'Could not load notifications. Please retry.',
      );
    } on DioException catch (error) {
      final payload = error.response?.data;
      final message = payload is Map ? payload['message']?.toString() : null;
      throw NotificationInboxException(
        message != null && message.isNotEmpty
            ? message
            : 'Could not connect to notifications. Please retry.',
      );
    }
  }
}
