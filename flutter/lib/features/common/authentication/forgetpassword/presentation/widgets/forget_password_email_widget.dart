import 'package:swift_app/features/common/shared/widgets/custom_text_field_widget.dart';
import 'package:flutter/material.dart';

class ForgetPasswordEmailWidget extends StatelessWidget {
  final TextEditingController emailController;

  const ForgetPasswordEmailWidget({super.key, required this.emailController});

  @override
  Widget build(BuildContext context) {
    return CustomTextFieldWidget(
      props: CustomTextFieldProps(
        fieldName: 'Email',
        placeHolder: 'name@example.com',
        isSecure: false,
        isPhone: false,
        textEditingController: emailController,
        keyboardType: TextInputType.emailAddress,
        validator: validateEmail,
      ),
    );
  }
}
