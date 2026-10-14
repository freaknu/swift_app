import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class SignupLoginTextWidget extends StatelessWidget {
  final bool enabled;

  const SignupLoginTextWidget({super.key, this.enabled = true});

  @override
  Widget build(BuildContext context) {
    return Wrap(
      alignment: WrapAlignment.center,
      crossAxisAlignment: WrapCrossAlignment.center,
      spacing: 4,
      children: [
        Text(
          'Have an account?',
          style: TextStyle(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),

        CupertinoButton(
          padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 4),
          minimumSize: const Size(44, 44),

          onPressed: enabled
              ? () {
                  Get.offNamed('/login');
                }
              : null,

          child: Text(
            'Sign in',
            style: TextStyle(
              color: enabled
                  ? Theme.of(context).colorScheme.primary
                  : Theme.of(
                      context,
                    ).colorScheme.primary.withValues(alpha: 0.35),

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
