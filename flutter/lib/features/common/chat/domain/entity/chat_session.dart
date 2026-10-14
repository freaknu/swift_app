class ChatSession {
  final int? sessionId;
  final String? sessionNameForDriver;
  final String? sessionNameForRider;
  final int? driverId;
  final int? riderId;
  final int? rideId;
  final String? rideStatus;
  final bool active;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const ChatSession({
    this.sessionId,
    this.sessionNameForDriver,
    this.sessionNameForRider,
    this.driverId,
    this.riderId,
    this.rideId,
    this.rideStatus,
    this.active = true,
    this.createdAt,
    this.updatedAt,
  });

  factory ChatSession.fromJson(Map<String, dynamic> json) {
    return ChatSession(
      sessionId: _asInt(json['sessionId'] ?? json['id']),
      sessionNameForDriver: json['sessionNameForDriver']?.toString(),
      sessionNameForRider: json['sessionNameForRider']?.toString(),
      driverId: _asInt(json['driverId']),
      riderId: _asInt(json['riderId']),
      rideId: _asInt(json['rideId']),
      rideStatus: json['rideStatus']?.toString(),
      active: _asBool(json['active'], fallback: true),
      createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? ''),
      updatedAt: DateTime.tryParse(json['updatedAt']?.toString() ?? ''),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'sessionId': sessionId,
      'sessionNameForDriver': sessionNameForDriver,
      'sessionNameForRider': sessionNameForRider,
      'driverId': driverId,
      'riderId': riderId,
      'rideId': rideId,
      'rideStatus': rideStatus,
      'active': active,
      'createdAt': createdAt?.toIso8601String(),
      'updatedAt': updatedAt?.toIso8601String(),
    };
  }

  static int? _asInt(dynamic value) {
    if (value is num) return value.toInt();
    return int.tryParse(value?.toString() ?? '');
  }

  static bool _asBool(dynamic value, {required bool fallback}) {
    if (value is bool) return value;
    if (value == null) return fallback;
    return value.toString().toLowerCase() == 'true';
  }
}
