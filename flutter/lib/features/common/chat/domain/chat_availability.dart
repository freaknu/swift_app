/// Chat is enabled only after a driver has accepted a ride and while that
/// accepted ride is still in progress.
class ChatAvailability {
  static const availableStatuses = <String>{
    'DRIVER_ASSIGNED',
    'DRIVER_ARRIVED',
    'STARTED',
  };

  static const terminalStatuses = <String>{
    'COMPLETED',
    'CANCELLED',
    'MATCH_FAILED',
  };

  static bool isAvailable(String? status) {
    final normalized = status?.trim().toUpperCase();
    return normalized != null && availableStatuses.contains(normalized);
  }

  static bool isTerminal(String? status) {
    final normalized = status?.trim().toUpperCase();
    return normalized != null && terminalStatuses.contains(normalized);
  }
}
