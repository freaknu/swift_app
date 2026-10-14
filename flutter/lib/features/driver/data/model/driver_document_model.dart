class DriverDocumentModel {
  final int? id;
  final String documentType;
  final String documentUrl;
  final String verificationStatus;
  final String? remarks;

  const DriverDocumentModel({
    this.id,
    required this.documentType,
    required this.documentUrl,
    required this.verificationStatus,
    this.remarks,
  });

  factory DriverDocumentModel.fromJson(Map<String, dynamic> json) {
    return DriverDocumentModel(
      id: (json['id'] as num?)?.toInt(),
      documentType: json['documentType']?.toString() ?? '',
      documentUrl: json['documentUrl']?.toString() ?? '',
      verificationStatus: json['verificationStatus']?.toString() ?? '',
      remarks: json['remarks']?.toString(),
    );
  }
}
