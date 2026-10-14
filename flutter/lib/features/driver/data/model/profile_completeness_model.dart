/// Result of GET /api/v1/driver/profile/completeness.
///
/// The driver home page calls this when the app opens and judges whether the
/// driver may go online. `complete` is true only when the personal,
/// documents, and vehicle sections are all complete.
class ProfileCompletenessModel {
  final bool complete;
  final CompletenessSection personal;
  final CompletenessSection documents;
  final CompletenessSection vehicle;
  final List<String> missing;

  const ProfileCompletenessModel({
    required this.complete,
    required this.personal,
    required this.documents,
    required this.vehicle,
    required this.missing,
  });

  factory ProfileCompletenessModel.fromJson(Map<String, dynamic> json) {
    return ProfileCompletenessModel(
      complete: json['complete'] == true,
      personal: CompletenessSection.fromJson(
        json['personal'] as Map<String, dynamic>?,
      ),
      documents: CompletenessSection.fromJson(
        json['documents'] as Map<String, dynamic>?,
      ),
      vehicle: CompletenessSection.fromJson(
        json['vehicle'] as Map<String, dynamic>?,
      ),
      missing:
          (json['missing'] as List?)?.map((item) => item.toString()).toList() ??
          const [],
    );
  }
}

class CompletenessSection {
  final bool complete;
  final List<String> missing;

  const CompletenessSection({required this.complete, required this.missing});

  factory CompletenessSection.fromJson(Map<String, dynamic>? json) {
    if (json == null) {
      return const CompletenessSection(complete: false, missing: []);
    }
    return CompletenessSection(
      complete: json['complete'] == true,
      missing:
          (json['missing'] as List?)?.map((item) => item.toString()).toList() ??
          const [],
    );
  }
}
