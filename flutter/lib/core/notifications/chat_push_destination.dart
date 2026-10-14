/// A chat notification identifies the signed-in account separately from its
/// rider or driver profile. Chat messages use the profile ID as sender ID.
class ChatPushDestination {
  const ChatPushDestination({
    required this.rideId,
    required this.currentProfileId,
    required this.role,
    this.riderId,
    this.driverId,
    this.rideStatus,
  });

  final int rideId;
  final int currentProfileId;
  final String role;
  final int? riderId;
  final int? driverId;
  final String? rideStatus;

  static ChatPushDestination? fromData(
    Map<String, dynamic> data,
    String? role,
  ) {
    final normalizedRole = role?.toUpperCase();
    if (normalizedRole != 'RIDER' && normalizedRole != 'DRIVER') return null;
    final rideId = int.tryParse(data['rideId']?.toString() ?? '');
    final riderId = int.tryParse(data['riderId']?.toString() ?? '');
    final driverId = int.tryParse(data['driverId']?.toString() ?? '');
    final profileId = normalizedRole == 'RIDER' ? riderId : driverId;
    if (rideId == null || profileId == null) return null;
    return ChatPushDestination(
      rideId: rideId,
      currentProfileId: profileId,
      role: normalizedRole!,
      riderId: riderId,
      driverId: driverId,
      rideStatus: data['rideStatus']?.toString(),
    );
  }
}
