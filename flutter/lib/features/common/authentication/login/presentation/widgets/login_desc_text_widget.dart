import 'package:swift_app/features/common/shared/widgets/auth_frame.dart';
import 'package:flutter/material.dart';

class LoginDescTextWidget extends StatelessWidget {
  const LoginDescTextWidget({super.key});

  @override
  Widget build(BuildContext context) => const AuthHeading(
    eyebrow: '',
    title: 'Sign in',
    description: 'We’ll email you a code.',
  );
}
