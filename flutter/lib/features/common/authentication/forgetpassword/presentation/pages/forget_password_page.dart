import 'package:swift_app/features/common/shared/widgets/auth_frame.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/buttons/primary_button.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

/// Keeps older links useful without pretending a password-reset API exists.
class ForgetPasswordPage extends StatelessWidget {
  const ForgetPasswordPage({super.key});

  @override
  Widget build(BuildContext context) => AuthFrame(
    icon: SwiftIcons.mail,
    header: const AuthHeading(
      eyebrow: '',
      title: 'Passwordless sign in',
      description: 'Request a one-time code from the sign-in screen.',
    ),
    child: PrimaryButton(
      primaryButtonProps: PrimaryButtonProps(
        text: 'Go to sign in',
        onClick: () => Get.offNamed('/login'),
      ),
    ),
  );
}
