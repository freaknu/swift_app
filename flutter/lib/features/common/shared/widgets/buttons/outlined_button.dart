import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:swift_app/themes/app_spacing.dart';
import 'package:flutter/material.dart';

class OutLinedButtonProps {
  final String text;
  final VoidCallback? onClick;
  const OutLinedButtonProps({required this.text, this.onClick});
}

class CustomOutlinedButton extends StatelessWidget {
  final OutLinedButtonProps props;
  const CustomOutlinedButton({super.key, required this.props});

  @override
  Widget build(BuildContext context) => GlassSurface(
    borderRadius: AppRadii.button,
    boxShadow: const [],
    child: SizedBox(
      width: double.infinity,
      child: OutlinedButton(
        onPressed: props.onClick,
        style: OutlinedButton.styleFrom(
          minimumSize: const Size(48, 54),
          padding: AppSpacing.outlinedButton,
          foregroundColor: Theme.of(context).colorScheme.onSurface,
          backgroundColor: Colors.transparent,
          side: BorderSide.none,
          shape: RoundedRectangleBorder(borderRadius: AppRadii.button),
        ),
        child: Text(
          props.text,
          style: Theme.of(
            context,
          ).textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w600),
        ),
      ),
    ),
  );
}
