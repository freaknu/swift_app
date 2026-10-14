import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class LoginSignupTextWidget extends StatelessWidget {
  final VoidCallback? onSignup;

  const LoginSignupTextWidget({super.key, this.onSignup});

  @override
  Widget build(BuildContext context) {
    return Wrap(
      alignment: WrapAlignment.center,
      crossAxisAlignment: WrapCrossAlignment.center,
      spacing: 4,
      children: [
        Text(
          'New here?',
          style: TextStyle(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),

        CupertinoButton(
          minimumSize: const Size(44, 44),
          padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 4),
          onPressed: onSignup,
          child: Text(
            'Create account',
            style: TextStyle(
              color: onSignup == null
                  ? Theme.of(context).colorScheme.primary.withValues(alpha: 0.4)
                  : Theme.of(context).colorScheme.primary,
              fontSize: 14,
              fontWeight: FontWeight.w700,
              letterSpacing: -0.15,
            ),
          ),
        ),
      ],
    );
  }
}
