class VehicleTypeOption {
  final String code;
  final String name;

  const VehicleTypeOption({required this.code, required this.name});

  factory VehicleTypeOption.fromJson(Map<String, dynamic> json) {
    return VehicleTypeOption(
      code: json['code']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
    );
  }
}
