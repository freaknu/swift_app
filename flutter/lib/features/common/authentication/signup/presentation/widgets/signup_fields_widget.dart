import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/authentication/signup/presentation/pages/signup_page.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class SignupFieldsWidget extends StatelessWidget {
  final SignUpControllers signUpControllers;
  final bool enabled;
  final VoidCallback? onSubmit;

  const SignupFieldsWidget({
    super.key,
    required this.signUpControllers,
    this.enabled = true,
    this.onSubmit,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _IOSSignupField(
          label: 'Name',
          hint: 'Full name',
          controller: signUpControllers.nameController,
          icon: SwiftIcons.userRound,
          enabled: enabled,
          textCapitalization: TextCapitalization.words,
          keyboardType: TextInputType.name,
          textInputAction: TextInputAction.next,
          autofillHints: const [AutofillHints.name],
          validator: (value) {
            if (value == null || value.trim().isEmpty) {
              return 'Enter your name';
            }

            if (value.trim().length < 2) {
              return 'Enter your full name';
            }

            return null;
          },
        ),

        const SizedBox(height: 16),

        _IOSSignupField(
          label: 'Email',
          hint: 'name@example.com',
          controller: signUpControllers.emailController,
          icon: SwiftIcons.mail,
          enabled: enabled,
          keyboardType: TextInputType.emailAddress,
          textInputAction: TextInputAction.next,
          autofillHints: const [AutofillHints.email],
          autocorrect: false,
          enableSuggestions: false,
          validator: _validateEmail,
        ),

        const SizedBox(height: 16),

        _IOSSignupField(
          label: 'Phone',
          hint: '10-digit number',
          controller: signUpControllers.phoneController,
          icon: SwiftIcons.phone,
          enabled: enabled,
          keyboardType: TextInputType.phone,
          textInputAction: TextInputAction.done,
          autofillHints: const [AutofillHints.telephoneNumberNational],
          maxLength: 10,
          inputFormatters: [
            FilteringTextInputFormatter.digitsOnly,
            LengthLimitingTextInputFormatter(10),
          ],
          validator: (value) {
            final phone = value?.trim() ?? '';

            if (!RegExp(r'^\d{10}$').hasMatch(phone)) {
              return 'Enter a 10-digit mobile number';
            }

            return null;
          },
          onSubmitted: (_) {
            onSubmit?.call();
          },
        ),
      ],
    );
  }

  static String? _validateEmail(String? value) {
    final String email = value?.trim() ?? '';

    if (email.isEmpty) {
      return 'Enter your email address';
    }

    final emailRegex = RegExp(
      r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$',
    );

    if (!emailRegex.hasMatch(email)) {
      return 'Enter a valid email address';
    }

    return null;
  }
}

/// =============================================================
/// IOS SIGNUP FIELD
/// =============================================================

class _IOSSignupField extends StatefulWidget {
  final String label;
  final String hint;

  final TextEditingController controller;

  final SwiftIcons icon;

  final bool enabled;

  final TextInputType? keyboardType;

  final TextInputAction? textInputAction;

  final TextCapitalization textCapitalization;

  final Iterable<String>? autofillHints;

  final bool autocorrect;

  final bool enableSuggestions;

  final int? maxLength;

  final List<TextInputFormatter>? inputFormatters;

  final String? Function(String?)? validator;

  final ValueChanged<String>? onSubmitted;

  const _IOSSignupField({
    required this.label,
    required this.hint,
    required this.controller,
    required this.icon,
    required this.enabled,
    this.keyboardType,
    this.textInputAction,
    this.textCapitalization = TextCapitalization.none,
    this.autofillHints,
    this.autocorrect = true,
    this.enableSuggestions = true,
    this.maxLength,
    this.inputFormatters,
    this.validator,
    this.onSubmitted,
  });

  @override
  State<_IOSSignupField> createState() => _IOSSignupFieldState();
}

class _IOSSignupFieldState extends State<_IOSSignupField> {
  final FocusNode _focusNode = FocusNode();

  bool _focused = false;

  @override
  void initState() {
    super.initState();

    _focusNode.addListener(() {
      if (!mounted) return;

      setState(() {
        _focused = _focusNode.hasFocus;
      });
    });
  }

  @override
  void dispose() {
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          widget.label,
          style: TextStyle(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
            fontSize: 13,
            fontWeight: FontWeight.w600,
            letterSpacing: -0.05,
          ),
        ),

        const SizedBox(height: 8),

        AnimatedContainer(
          duration: MediaQuery.disableAnimationsOf(context)
              ? Duration.zero
              : const Duration(milliseconds: 220),
          curve: Curves.easeOutCubic,
          decoration: BoxDecoration(
            borderRadius: AppRadii.button,

            color: Theme.of(context).colorScheme.surface,

            border: Border.all(
              color: _focused
                  ? Theme.of(
                      context,
                    ).colorScheme.primary.withValues(alpha: 0.67)
                  : Theme.of(context).colorScheme.outlineVariant,

              width: _focused ? 1.4 : 1,
            ),
          ),

          child: TextFormField(
            controller: widget.controller,

            focusNode: _focusNode,

            enabled: widget.enabled,

            keyboardType: widget.keyboardType,

            textInputAction: widget.textInputAction,

            textCapitalization: widget.textCapitalization,

            autofillHints: widget.autofillHints,

            autocorrect: widget.autocorrect,

            enableSuggestions: widget.enableSuggestions,

            maxLength: widget.maxLength,

            inputFormatters: widget.inputFormatters,

            validator: widget.validator,

            onFieldSubmitted: widget.onSubmitted,

            cursorColor: Theme.of(context).colorScheme.primary,

            style: TextStyle(
              color: Theme.of(context).colorScheme.onSurface,
              fontSize: 16,
              fontWeight: FontWeight.w500,
              letterSpacing: -0.2,
            ),

            decoration: InputDecoration(
              hintText: widget.hint,

              counterText: '',

              hintStyle: TextStyle(
                color: Theme.of(
                  context,
                ).colorScheme.onSurfaceVariant.withValues(alpha: 0.75),
                fontWeight: FontWeight.w400,
              ),

              prefixIcon: Padding(
                padding: const EdgeInsets.only(left: 14, right: 12),
                child: AppIcon(
                  widget.icon,
                  size: 18,
                  color: _focused
                      ? Theme.of(context).colorScheme.primary
                      : Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
              prefixIconConstraints: const BoxConstraints(minWidth: 48),

              border: InputBorder.none,

              enabledBorder: InputBorder.none,

              focusedBorder: InputBorder.none,

              disabledBorder: InputBorder.none,

              errorBorder: InputBorder.none,

              focusedErrorBorder: InputBorder.none,

              contentPadding: const EdgeInsets.symmetric(
                horizontal: 16,
                vertical: 18,
              ),

              errorStyle: const TextStyle(
                color: Color(0xFFFF3B30),
                fontSize: 12,
                height: 1.3,
                fontWeight: FontWeight.w500,
              ),

              errorMaxLines: 2,
            ),
          ),
        ),
      ],
    );
  }
}
