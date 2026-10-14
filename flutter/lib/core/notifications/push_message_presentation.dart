class PushMessagePresentation {
  static const _rideTypes = {
    'RIDE_OFFER',
    'DRIVER_ASSIGNED',
    'DRIVER_ARRIVED',
    'STARTED',
    'COMPLETED',
    'CANCELLED',
    'MATCH_FAILED',
  };

  static bool isChat(Map<String, dynamic> data) {
    final type = data['type']?.toString().toUpperCase();
    return type == 'CHAT' ||
        type == 'CHAT_MESSAGE' ||
        data['sessionId'] != null ||
        data['messageContent'] != null;
  }

  static bool supportsDataOnly(Map<String, dynamic> data) =>
      isChat(data) ||
      _rideTypes.contains(data['type']?.toString().toUpperCase());

  static bool isExpired(Map<String, dynamic> data, DateTime now) {
    final expiresAt = DateTime.tryParse(data['expiresAt']?.toString() ?? '');
    return expiresAt != null && !expiresAt.isAfter(now);
  }

  static int notificationId(Map<String, dynamic> data) {
    final key = isChat(data)
        ? 'chat:${data['messageId'] ?? data['sessionId'] ?? data['rideId'] ?? ''}'
        : data['rideId'] != null
        ? 'ride:${data['rideId']}'
        : 'event:${data['eventId'] ?? data['documentId'] ?? data['vehicleId'] ?? data['ratingId'] ?? data['type'] ?? ''}';
    // FNV-1a gives a stable Android notification ID across process launches.
    var hash = 0x811c9dc5;
    for (final unit in key.codeUnits) {
      hash = ((hash ^ unit) * 0x01000193) & 0xffffffff;
    }
    return hash & 0x7fffffff;
  }

  static String title(Map<String, dynamic> data) {
    final supplied = data['title']?.toString().trim();
    if (supplied != null && supplied.isNotEmpty) return supplied;
    if (isChat(data)) return 'New chat message';
    return switch (data['type']?.toString().toUpperCase()) {
      'RIDE_OFFER' => 'New ride request',
      'DRIVER_ASSIGNED' => 'Driver found',
      'DRIVER_ARRIVED' => 'Your driver has arrived',
      'STARTED' => 'Ride started',
      'COMPLETED' => 'Ride completed',
      'CANCELLED' => 'Ride cancelled',
      'MATCH_FAILED' => 'No drivers available',
      _ => 'Ride update',
    };
  }

  static String body(Map<String, dynamic> data) {
    final supplied = data['body']?.toString().trim();
    if (supplied != null && supplied.isNotEmpty) return supplied;
    if (isChat(data)) {
      return data['messageContent']?.toString() ?? 'Open Swift to reply.';
    }
    return switch (data['type']?.toString().toUpperCase()) {
      'RIDE_OFFER' => 'Review the trip and respond before it expires.',
      'MATCH_FAILED' => 'Please try another ride.',
      _ => 'Open Swift for the latest trip details.',
    };
  }
}
