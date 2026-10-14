class DriverJourneyPage {
  final List<DriverJourney> content;
  final int page;
  final int totalPages;
  final int totalElements;
  final bool last;

  const DriverJourneyPage({
    required this.content,
    required this.page,
    required this.totalPages,
    required this.totalElements,
    required this.last,
  });

  factory DriverJourneyPage.fromJson(Map<String, dynamic> json) {
    final content = json['content'];
    return DriverJourneyPage(
      content: content is List
          ? content
                .whereType<Map<String, dynamic>>()
                .map(DriverJourney.fromJson)
                .toList()
          : const [],
      page: _int(json['page']) ?? 0,
      totalPages: _int(json['totalPages']) ?? 0,
      totalElements: _int(json['totalElements']) ?? 0,
      last: json['last'] == true,
    );
  }
}

class DriverJourney {
  final int id;
  final String status;
  final String? rideTypeCode;
  final DateTime? requestedAt;
  final DateTime? completedAt;
  final DateTime? cancelledAt;
  final double? pickupLatitude;
  final double? pickupLongitude;
  final double? destinationLatitude;
  final double? destinationLongitude;
  final double? estimatedDistanceKm;
  final double? actualDistanceKm;
  final double? fare;
  final String? currency;

  const DriverJourney({
    required this.id,
    required this.status,
    this.rideTypeCode,
    this.requestedAt,
    this.completedAt,
    this.cancelledAt,
    this.pickupLatitude,
    this.pickupLongitude,
    this.destinationLatitude,
    this.destinationLongitude,
    this.estimatedDistanceKm,
    this.actualDistanceKm,
    this.fare,
    this.currency,
  });

  factory DriverJourney.fromJson(Map<String, dynamic> json) {
    final fareDetail = json['fareDetail'];
    final fareMap = fareDetail is Map<String, dynamic>
        ? fareDetail
        : const <String, dynamic>{};
    return DriverJourney(
      id: _int(json['id']) ?? 0,
      status: json['currentStatus']?.toString() ?? '',
      rideTypeCode: json['rideTypeCode']?.toString(),
      requestedAt: DateTime.tryParse(json['requestedAt']?.toString() ?? ''),
      completedAt: DateTime.tryParse(json['completedAt']?.toString() ?? ''),
      cancelledAt: DateTime.tryParse(json['cancelledAt']?.toString() ?? ''),
      pickupLatitude: _double(json['pickupLatitude']),
      pickupLongitude: _double(json['pickupLongitude']),
      destinationLatitude: _double(json['destinationLatitude']),
      destinationLongitude: _double(json['destinationLongitude']),
      estimatedDistanceKm: _double(json['estimatedDistance']),
      actualDistanceKm: _double(json['actualDistance']),
      fare: _double(fareMap['totalFare']),
      currency: fareMap['currency']?.toString(),
    );
  }
}

int? _int(Object? value) =>
    value is num ? value.toInt() : int.tryParse(value?.toString() ?? '');

double? _double(Object? value) =>
    value is num ? value.toDouble() : double.tryParse(value?.toString() ?? '');
