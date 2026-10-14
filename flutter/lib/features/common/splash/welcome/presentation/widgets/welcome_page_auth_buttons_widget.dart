import 'package:swift_app/constants/enums/role_enum.dart';
import 'package:swift_app/features/common/shared/widgets/buttons/outlined_button.dart';
import 'package:swift_app/features/common/shared/widgets/buttons/primary_button.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class WelcomePageAuthButtonsWidget extends StatelessWidget {
  final RoleEnum selectedRole;

  const WelcomePageAuthButtonsWidget({
    super.key,
    this.selectedRole = RoleEnum.RIDER,
  });

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      PrimaryButton(
        primaryButtonProps: PrimaryButtonProps(
          text: 'Get started',
          onClick: () => Get.toNamed('/signup', arguments: selectedRole),
        ),
      ),
      const SizedBox(height: 12),
      CustomOutlinedButton(
        props: OutLinedButtonProps(
          text: 'Sign in',
          onClick: () => Get.toNamed('/login', arguments: selectedRole),
        ),
      ),
    ],
  );
}
