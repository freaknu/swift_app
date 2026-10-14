import 'package:swift_app/features/common/authentication/forgetpassword/presentation/pages/change_password_page.dart';
import 'package:swift_app/features/common/shared/widgets/custom_text_field_widget.dart';
import 'package:flutter/material.dart';

class ForgetPasswordConfirmProps {
  final String email;
  ForgetPasswordConfirmProps({required this.email});
}

class ForgetPasswordConfirmWidget extends StatelessWidget {
  final ChangePasswordController controller;
  const ForgetPasswordConfirmWidget({super.key, required this.controller});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        CustomTextFieldWidget(
          props: CustomTextFieldProps(
            fieldName: 'New password',
            placeHolder: 'Enter password',
            isSecure: true,
            textEditingController: controller.newPasswordController,
          ),
        ),

        const SizedBox(height: 16),
        CustomTextFieldWidget(
          props: CustomTextFieldProps(
            fieldName: 'Confirm password',
            placeHolder: 'Enter again',
            isSecure: true,
            textEditingController: controller.confirmPasswordController,
          ),
        ),
      ],
    );
  }
}
