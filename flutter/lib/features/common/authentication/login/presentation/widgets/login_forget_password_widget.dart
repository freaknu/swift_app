import 'package:flutter/material.dart';
import 'package:get/get.dart';

class LoginForgetPasswordWidget extends StatelessWidget {
  const LoginForgetPasswordWidget({super.key});

  @override
  Widget build(BuildContext context) => TextButton(
    onPressed: () => Get.toNamed('/forgetPassword'),
    child: const Text('About sign in'),
  );
}
