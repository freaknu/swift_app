import 'package:flutter/material.dart';
import 'package:swift_app/themes/app_radii.dart';

class WelcomePageImageWidget extends StatelessWidget {
  const WelcomePageImageWidget({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return RepaintBoundary(
      child: DecoratedBox(
        decoration: BoxDecoration(
          borderRadius: AppRadii.cardRadius,
          color: theme.colorScheme.surface,
          border: Border.all(color: theme.colorScheme.outlineVariant),
        ),
        child: AspectRatio(
          aspectRatio: 1.58,
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: LayoutBuilder(
              builder: (context, constraints) => Image.asset(
                'assets/images/welcome_image.png',
                cacheWidth:
                    (constraints.maxWidth *
                            MediaQuery.devicePixelRatioOf(context))
                        .round(),
                fit: BoxFit.contain,
                excludeFromSemantics: true,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
