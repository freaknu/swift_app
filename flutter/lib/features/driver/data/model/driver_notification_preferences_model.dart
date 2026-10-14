class DriverNotificationPreferencesModel {
  final bool pushEnabled;
  final bool smsEnabled;
  final bool emailEnabled;
  final bool rideAlertsEnabled;
  final bool surgeAlertsEnabled;

  const DriverNotificationPreferencesModel({
    required this.pushEnabled,
    required this.smsEnabled,
    required this.emailEnabled,
    required this.rideAlertsEnabled,
    required this.surgeAlertsEnabled,
  });

  factory DriverNotificationPreferencesModel.fromJson(
    Map<String, dynamic> json,
  ) {
    return DriverNotificationPreferencesModel(
      pushEnabled: json['pushEnabled'] == true,
      smsEnabled: json['smsEnabled'] == true,
      emailEnabled: json['emailEnabled'] == true,
      rideAlertsEnabled: json['rideAlertsEnabled'] == true,
      surgeAlertsEnabled: json['surgeAlertsEnabled'] == true,
    );
  }

  Map<String, dynamic> toJson() => {
    'pushEnabled': pushEnabled,
    'smsEnabled': smsEnabled,
    'emailEnabled': emailEnabled,
    'rideAlertsEnabled': rideAlertsEnabled,
    'surgeAlertsEnabled': surgeAlertsEnabled,
  };

  DriverNotificationPreferencesModel copyWith({
    bool? pushEnabled,
    bool? smsEnabled,
    bool? emailEnabled,
    bool? rideAlertsEnabled,
    bool? surgeAlertsEnabled,
  }) {
    return DriverNotificationPreferencesModel(
      pushEnabled: pushEnabled ?? this.pushEnabled,
      smsEnabled: smsEnabled ?? this.smsEnabled,
      emailEnabled: emailEnabled ?? this.emailEnabled,
      rideAlertsEnabled: rideAlertsEnabled ?? this.rideAlertsEnabled,
      surgeAlertsEnabled: surgeAlertsEnabled ?? this.surgeAlertsEnabled,
    );
  }
}
