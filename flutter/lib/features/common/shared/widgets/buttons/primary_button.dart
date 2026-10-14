import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:swift_app/themes/app_spacing.dart';

class PrimaryButtonProps {
  final String text;
  final VoidCallback? onClick;
  final bool loading;
  final Widget? icon;

  const PrimaryButtonProps({
    required this.text,
    this.onClick,
    this.loading = false,
    this.icon,
  });
}

/// Native press feedback, correct gesture cancellation, and accessible sizing.
class PrimaryButton extends StatelessWidget {
  final PrimaryButtonProps primaryButtonProps;

  const PrimaryButton({super.key, required this.primaryButtonProps});

  @override
  Widget build(BuildContext context) {
    final props = primaryButtonProps;
    final theme = Theme.of(context);
    final enabled = props.onClick != null && !props.loading;
    return CupertinoButton(
      onPressed: enabled ? props.onClick : null,
      color: theme.colorScheme.primary,
      disabledColor: props.loading
          ? theme.colorScheme.primary
          : theme.colorScheme.primary.withValues(alpha: 0.15),
      borderRadius: AppRadii.button,
      minimumSize: const Size(44, AppSpacing.largeControlHeight),
      padding: AppSpacing.primaryButton,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          if (props.loading) ...[
            CupertinoActivityIndicator(color: theme.colorScheme.onPrimary),
            const SizedBox(width: 10),
          ],
          Flexible(
            child: Text(
              props.text,
              textAlign: TextAlign.center,
              style: theme.textTheme.titleSmall?.copyWith(
                color: props.loading
                    ? theme.colorScheme.onPrimary
                    : enabled
                    ? theme.colorScheme.onPrimary
                    : theme.colorScheme.primary,
                fontSize: 17,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          if (props.icon != null && !props.loading) ...[
            const SizedBox(width: 10),
            IconTheme(
              data: IconThemeData(
                color: enabled
                    ? theme.colorScheme.onPrimary
                    : theme.disabledColor,
              ),
              child: props.icon!,
            ),
          ],
        ],
      ),
    );
  }
}
