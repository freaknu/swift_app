import 'package:swift_app/features/common/shared/widgets/auth_frame.dart';
import 'package:flutter/material.dart';

class OtpDescriptionWidget extends StatelessWidget {
  const OtpDescriptionWidget({super.key});

  @override
  Widget build(BuildContext context) => const AuthHeading(
    eyebrow: '',
    title: 'Enter code',
    description: 'Enter the 6-digit code from your email.',
  );
}
