import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/authentication/login/presentation/pages/login_page.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:flutter/material.dart';

class LoginFieldsWidget extends StatefulWidget {
  final LoginControllers loginController;
  final bool enabled;
  final VoidCallback? onSubmit;

  const LoginFieldsWidget({
    super.key,
    required this.loginController,
    this.enabled = true,
    this.onSubmit,
  });

  @override
  State<LoginFieldsWidget> createState() => _LoginFieldsWidgetState();
}

class _LoginFieldsWidgetState extends State<LoginFieldsWidget> {
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

  String? _validateEmail(String? value) {
    final email = value?.trim() ?? '';

    if (email.isEmpty) {
      return 'Enter your email address';
    }

    final emailRegex = RegExp(
      r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$',
    );

    if (!emailRegex.hasMatch(email)) {
      return 'Enter a valid email, like name@example.com';
    }

    return null;
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Email',
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
                    ).colorScheme.primary.withValues(alpha: 0.65)
                  : Theme.of(context).colorScheme.outlineVariant,
              width: _focused ? 1.4 : 1,
            ),
          ),
          child: TextFormField(
            controller: widget.loginController.emailController,
            focusNode: _focusNode,
            enabled: widget.enabled,
            validator: _validateEmail,
            keyboardType: TextInputType.emailAddress,
            textInputAction: TextInputAction.done,
            autofillHints: const [AutofillHints.email],
            autocorrect: false,
            enableSuggestions: false,
            onFieldSubmitted: (_) {
              widget.onSubmit?.call();
            },
            style: TextStyle(
              color: Theme.of(context).colorScheme.onSurface,
              fontSize: 16,
              fontWeight: FontWeight.w500,
              letterSpacing: -0.2,
            ),
            cursorColor: Theme.of(context).colorScheme.primary,
            decoration: InputDecoration(
              hintText: 'name@example.com',
              hintStyle: TextStyle(
                color: Theme.of(
                  context,
                ).colorScheme.onSurfaceVariant.withValues(alpha: 0.75),
                fontWeight: FontWeight.w400,
              ),

              prefixIcon: Padding(
                padding: const EdgeInsets.only(left: 14, right: 12),
                child: AppIcon(
                  SwiftIcons.mail,
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
