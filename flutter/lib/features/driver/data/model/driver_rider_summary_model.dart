class DriverRiderSummaryModel {
  final int riderId;
  final String? riderName;

  const DriverRiderSummaryModel({required this.riderId, this.riderName});

  factory DriverRiderSummaryModel.fromJson(Map<String, dynamic> json) {
    final rawName = json['riderName']?.toString().trim();
    return DriverRiderSummaryModel(
      riderId: (json['riderId'] as num).toInt(),
      riderName: rawName == null || rawName.isEmpty ? null : rawName,
    );
  }
}
