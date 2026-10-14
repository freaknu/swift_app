import 'package:flutter/material.dart';

class SignupDescTextWidget extends StatelessWidget {
  const SignupDescTextWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Get started',
          style: TextStyle(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
            fontSize: 14,
            fontWeight: FontWeight.w600,
            letterSpacing: -0.1,
          ),
        ),

        const SizedBox(height: 7),

        Text(
          'Create account',
          style: TextStyle(
            color: Theme.of(context).colorScheme.onSurface,
            fontSize: 34,
            height: 1.05,
            fontWeight: FontWeight.w700,
            letterSpacing: -1.25,
          ),
        ),

        const SizedBox(height: 10),

        Text(
          'Enter your details and we’ll send you a secure verification code.',
          style: TextStyle(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
            fontSize: 15,
            height: 1.45,
            fontWeight: FontWeight.w400,
            letterSpacing: -0.15,
          ),
        ),
      ],
    );
  }
}
