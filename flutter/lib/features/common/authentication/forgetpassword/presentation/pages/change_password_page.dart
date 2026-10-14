import 'package:swift_app/features/common/authentication/forgetpassword/presentation/pages/forget_password_page.dart';
import 'package:flutter/material.dart';

class ChangePasswordController {
  final TextEditingController newPasswordController = TextEditingController();
  final TextEditingController confirmPasswordController =
      TextEditingController();

  void dispose() {
    newPasswordController.dispose();
    confirmPasswordController.dispose();
  }
}

/// The app authenticates with email codes; legacy reset links lead to guidance.
class ChangePasswordPage extends StatelessWidget {
  const ChangePasswordPage({super.key});

  @override
  Widget build(BuildContext context) => const ForgetPasswordPage();
}
