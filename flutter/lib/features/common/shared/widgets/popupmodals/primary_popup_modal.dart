import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:flutter/material.dart';

class PrimaryPopupModalProps {
  final String title;
  final String subTitle;
  final VoidCallback? onClick;
  PrimaryPopupModalProps({
    required this.title,
    required this.subTitle,
    this.onClick,
  });
}

class PrimaryPopupModal extends StatelessWidget {
  final PrimaryPopupModalProps props;
  const PrimaryPopupModal({super.key, required this.props});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Center(
      child: GlassSurface(
        margin: const EdgeInsets.all(24),
        padding: const EdgeInsets.all(22),
        borderRadius: AppRadii.dialogRadius,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: theme.colorScheme.primary.withValues(alpha: 0.12),
              ),
              child: AppIcon(
                SwiftIcons.check,
                color: theme.colorScheme.primary,
                size: 20,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              props.title,
              textAlign: TextAlign.center,
              style: theme.textTheme.titleLarge,
            ),
            const SizedBox(height: 7),
            Text(
              props.subTitle,
              textAlign: TextAlign.center,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed:
                    props.onClick ?? () => Navigator.of(context).maybePop(),
                child: const Text('Done'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
