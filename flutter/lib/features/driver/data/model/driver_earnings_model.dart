class DriverEarningsModel {
  final double thisWeek;
  final double thisMonth;
  final double lifetime;
  final DateTime? updatedAt;

  const DriverEarningsModel({
    required this.thisWeek,
    required this.thisMonth,
    required this.lifetime,
    this.updatedAt,
  });

  factory DriverEarningsModel.fromJson(Map<String, dynamic> json) {
    return DriverEarningsModel(
      thisWeek: _double(json['thisWeekIncome']) ?? 0,
      thisMonth: _double(json['thisMonthIncome']) ?? 0,
      lifetime: _double(json['thisLifetimeIncome']) ?? 0,
      updatedAt: DateTime.tryParse(json['updatedAt']?.toString() ?? ''),
    );
  }
}

double? _double(Object? value) =>
    value is num ? value.toDouble() : double.tryParse(value?.toString() ?? '');
