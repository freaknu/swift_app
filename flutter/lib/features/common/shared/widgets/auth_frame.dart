import 'package:swift_app/features/common/shared/widgets/backbuttons/primary_back_button.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/common/shared/widgets/swift_brand_mark.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:flutter/material.dart';

class SwiftBrand extends StatelessWidget {
  const SwiftBrand({super.key, this.onBar = false});

  final bool onBar;

  @override
  Widget build(BuildContext context) => Row(
    mainAxisSize: MainAxisSize.min,
    children: [
      const SwiftBrandMark(excludeFromSemantics: true),
      const SizedBox(width: 8),
      Flexible(
        child: Text(
          'Swift',
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: Theme.of(context).textTheme.titleMedium?.copyWith(
            color: Theme.of(context).colorScheme.onSurface,
            fontWeight: FontWeight.w600,
            letterSpacing: -0.3,
          ),
        ),
      ),
    ],
  );
}

/// A bounded, keyboard-safe layout shared by the email sign-in journey.
class AuthFrame extends StatelessWidget {
  final Widget header;
  final Widget child;
  final SwiftIcons icon;

  const AuthFrame({
    super.key,
    required this.header,
    required this.child,
    this.icon = SwiftIcons.atSign,
  });

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: Colors.transparent,
    appBar: SwiftAppBar(
      toolbarHeight: SwiftAppBar.toolbarHeightFor(context),
      title: const SwiftBrand(onBar: true),
      automaticallyImplyLeading: false,
      leading: const PrimaryBackButton(
        props: PrimaryBackButtonProps(text: 'Back'),
      ),
    ),
    body: AppGlassBackground(
      child: SafeArea(
        top: false,
        child: LayoutBuilder(
          builder: (context, constraints) => SingleChildScrollView(
            physics: const BouncingScrollPhysics(),
            keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
            padding: const EdgeInsets.fromLTRB(16, 10, 16, 24),
            child: ConstrainedBox(
              constraints: BoxConstraints(
                minHeight: (constraints.maxHeight - 38).clamp(
                  0,
                  double.infinity,
                ),
              ),
              child: Align(
                alignment: Alignment.topCenter,
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 520),
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(0, 12, 0, 22),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const SizedBox(height: 14),
                        Align(
                          alignment: Alignment.centerLeft,
                          child: Container(
                            width: 36,
                            height: 36,
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(10),
                              color: Theme.of(
                                context,
                              ).colorScheme.primary.withValues(alpha: 0.12),
                            ),
                            child: AppIcon(
                              icon,
                              color: Theme.of(context).colorScheme.primary,
                              size: 20,
                            ),
                          ),
                        ),
                        const SizedBox(height: 18),
                        header,
                        const SizedBox(height: 24),
                        child,
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    ),
  );
}

class AuthHeading extends StatelessWidget {
  final String eyebrow;
  final String title;
  final String description;

  const AuthHeading({
    super.key,
    required this.eyebrow,
    required this.title,
    required this.description,
  });

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (eyebrow.isNotEmpty) ...[
          Text(
            eyebrow,
            style: textTheme.labelMedium?.copyWith(
              color: Theme.of(context).colorScheme.primary,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 6),
        ],
        Text(
          title,
          style: textTheme.headlineLarge?.copyWith(
            color: Theme.of(context).colorScheme.onSurface,
            fontWeight: FontWeight.w600,
            height: 1.2,
          ),
        ),
        if (description.isNotEmpty) ...[
          const SizedBox(height: 8),
          Text(
            description,
            style: textTheme.bodyMedium?.copyWith(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
              height: 1.45,
            ),
          ),
        ],
      ],
    );
  }
}

class AuthErrorMessage extends StatelessWidget {
  final String message;
  const AuthErrorMessage({super.key, required this.message});

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Semantics(
      liveRegion: true,
      child: Container(
        margin: const EdgeInsets.only(bottom: 14),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        decoration: BoxDecoration(
          color: colors.errorContainer,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Text(
          message.isEmpty ? 'Something went wrong. Please try again.' : message,
          style: Theme.of(
            context,
          ).textTheme.bodyMedium?.copyWith(color: colors.onErrorContainer),
        ),
      ),
    );
  }
}
