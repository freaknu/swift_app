import 'package:swift_app/features/common/shared/widgets/auth_frame.dart';
import 'package:flutter/material.dart';

class ForgetPasswordDescriptionWidget extends StatelessWidget {
  const ForgetPasswordDescriptionWidget({super.key});

  @override
  Widget build(BuildContext context) => const AuthHeading(
    eyebrow: '',
    title: 'Sign-in help',
    description: 'Request a one-time code with your email.',
  );
}
