import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class OtpResendWidget extends StatelessWidget {
  final bool isLoading;
  final int secondsRemaining;
  final VoidCallback? onResend;

  const OtpResendWidget({
    super.key,
    required this.isLoading,
    required this.secondsRemaining,
    this.onResend,
  });

  @override
  Widget build(BuildContext context) {
    final canResend = secondsRemaining <= 0 && !isLoading && onResend != null;

    return Column(
      children: [
        Text(
          'Didn’t receive the code?',
          style: TextStyle(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
            fontSize: 13.5,
            fontWeight: FontWeight.w500,
          ),
        ),

        const SizedBox(height: 4),

        AnimatedSwitcher(
          duration: MediaQuery.disableAnimationsOf(context)
              ? Duration.zero
              : const Duration(milliseconds: 180),
          child: isLoading
              ? const Padding(
                  key: ValueKey('loading'),
                  padding: EdgeInsets.all(8),
                  child: CupertinoActivityIndicator(radius: 9),
                )
              : secondsRemaining > 0
              ? Text(
                  'Resend in ${secondsRemaining}s',
                  style: TextStyle(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                  ),
                )
              : TextButton(
                  key: const ValueKey('resend'),
                  style: TextButton.styleFrom(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 10,
                      vertical: 5,
                    ),
                    minimumSize: const Size(44, 44),
                  ),
                  onPressed: canResend ? onResend : null,
                  child: Text(
                    'Resend code',
                    style: TextStyle(
                      color: canResend
                          ? Theme.of(context).colorScheme.primary
                          : Theme.of(
                              context,
                            ).colorScheme.primary.withValues(alpha: 0.35),
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
        ),
      ],
    );
  }
}
