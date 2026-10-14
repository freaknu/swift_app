import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Secondary action with the same touch target and press feedback as the
/// primary action. Keep destructive intent out of this button.
class SecondaryButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;
  final Widget? icon;
  final bool loading;

  const SecondaryButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.loading = false,
  });

  void _press() {
    if (onPressed == null || loading) return;
    HapticFeedback.selectionClick();
    onPressed!();
  }

  @override
  Widget build(BuildContext context) {
    return OutlinedButton(
      onPressed: onPressed == null || loading ? null : _press,
      child: _ButtonContent(label: label, icon: icon, loading: loading),
    );
  }
}

/// Destructive action with a clear error color and an accessible, full-size
/// tap target.
class DangerButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;
  final Widget? icon;
  final bool loading;

  const DangerButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon,
    this.loading = false,
  });

  void _press() {
    if (onPressed == null || loading) return;
    HapticFeedback.mediumImpact();
    onPressed!();
  }

  @override
  Widget build(BuildContext context) {
    final error = Theme.of(context).colorScheme.error;
    return FilledButton(
      onPressed: onPressed == null || loading ? null : _press,
      style: FilledButton.styleFrom(
        backgroundColor: error,
        foregroundColor: Colors.white,
      ),
      child: _ButtonContent(label: label, icon: icon, loading: loading),
    );
  }
}

class _ButtonContent extends StatelessWidget {
  final String label;
  final Widget? icon;
  final bool loading;

  const _ButtonContent({
    required this.label,
    required this.icon,
    required this.loading,
  });

  @override
  Widget build(BuildContext context) {
    if (loading) {
      return const SizedBox(
        width: 20,
        height: 20,
        child: CircularProgressIndicator(strokeWidth: 2),
      );
    }
    return Row(
      mainAxisSize: MainAxisSize.min,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        if (icon != null) ...[icon!, const SizedBox(width: 8)],
        Flexible(child: Text(label, textAlign: TextAlign.center)),
      ],
    );
  }
}
