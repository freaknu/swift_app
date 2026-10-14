import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// A real editable field keeps iOS code autofill, paste, selection and
/// VoiceOver working. Hiding the input behind painted boxes breaks those APIs.
class OtpInputWidget extends StatelessWidget {
  final TextEditingController controller;
  final bool enabled;
  final VoidCallback? onSubmit;

  const OtpInputWidget({
    super.key,
    required this.controller,
    this.enabled = true,
    this.onSubmit,
  });

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: controller,
      enabled: enabled,
      autofocus: true,
      keyboardType: TextInputType.number,
      textInputAction: TextInputAction.done,
      autofillHints: const [AutofillHints.oneTimeCode],
      textAlign: TextAlign.center,
      maxLength: 6,
      style: Theme.of(context).textTheme.headlineSmall?.copyWith(
        letterSpacing: 8,
        fontWeight: FontWeight.w600,
      ),
      inputFormatters: [
        FilteringTextInputFormatter.digitsOnly,
        LengthLimitingTextInputFormatter(6),
      ],
      decoration: const InputDecoration(
        labelText: 'Verification code',
        floatingLabelBehavior: FloatingLabelBehavior.always,
        hintText: '000000',
        counterText: '',
      ),
      validator: (value) =>
          value?.length == 6 ? null : 'Enter the complete 6-digit code',
      onFieldSubmitted: enabled ? (_) => onSubmit?.call() : null,
    );
  }
}
