import 'package:swift_app/themes/app_radii.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/themes/app_spacing.dart';
import 'package:flutter/material.dart';

class OauthButtonFields {
  final String text;
  final String iconPath;
  final VoidCallback? onClick;

  const OauthButtonFields({
    required this.text,
    required this.iconPath,
    this.onClick,
  });
}

class OauthButton extends StatelessWidget {
  final OauthButtonFields props;

  const OauthButton({super.key, required this.props});

  @override
  Widget build(BuildContext context) {
    return GlassSurface(
      borderRadius: AppRadii.button,
      boxShadow: const [],
      child: SizedBox(
        width: double.infinity,
        height: AppSpacing.outlinedButtonHeight,
        child: OutlinedButton(
          onPressed: props.onClick,
          style: OutlinedButton.styleFrom(
            backgroundColor: Colors.transparent,
            foregroundColor: Theme.of(context).colorScheme.onSurface,
            elevation: 0,
            side: BorderSide.none,
            shape: RoundedRectangleBorder(borderRadius: AppRadii.button),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Image.asset(props.iconPath, height: 22, width: 22),
              const SizedBox(width: 12),
              Flexible(
                child: Text(
                  props.text,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(
                    context,
                  ).textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w600),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
