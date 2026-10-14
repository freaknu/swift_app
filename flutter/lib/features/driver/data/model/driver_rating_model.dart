class DriverRatingModel {
  final int? id;
  final double averageRating;
  final List<DriverRatingEntry> entries;

  const DriverRatingModel({
    this.id,
    required this.averageRating,
    required this.entries,
  });

  factory DriverRatingModel.fromJson(Map<String, dynamic> json) {
    final rawEntries = json['allRatings'];
    return DriverRatingModel(
      id: _int(json['id']),
      averageRating: _double(json['averageRating']) ?? 0,
      entries: rawEntries is List
          ? rawEntries
                .whereType<Map<String, dynamic>>()
                .map(DriverRatingEntry.fromJson)
                .toList()
          : const [],
    );
  }
}

class DriverRatingEntry {
  final int? id;
  final int? rideId;
  final double score;
  final DateTime? createdAt;

  const DriverRatingEntry({
    this.id,
    this.rideId,
    required this.score,
    this.createdAt,
  });

  factory DriverRatingEntry.fromJson(Map<String, dynamic> json) {
    return DriverRatingEntry(
      id: _int(json['id']),
      rideId: _int(json['rideId']),
      score: _double(json['rate']) ?? 0,
      createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? ''),
    );
  }
}

int? _int(Object? value) =>
    value is num ? value.toInt() : int.tryParse(value?.toString() ?? '');

double? _double(Object? value) =>
    value is num ? value.toDouble() : double.tryParse(value?.toString() ?? '');
