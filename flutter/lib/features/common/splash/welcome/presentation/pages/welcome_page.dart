import 'package:swift_app/constants/enums/role_enum.dart';
import 'package:swift_app/features/common/shared/widgets/auth_frame.dart';
import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/splash/welcome/presentation/widgets/welcome_page_auth_buttons_widget.dart';
import 'package:swift_app/features/common/splash/welcome/presentation/widgets/welcome_page_image_widget.dart';
import 'package:swift_app/features/common/splash/welcome/presentation/widgets/welcome_text_widget.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class WelcomePage extends StatefulWidget {
  const WelcomePage({super.key});

  @override
  State<WelcomePage> createState() => _WelcomePageState();
}

class _WelcomePageState extends State<WelcomePage> {
  RoleEnum _selectedRole = RoleEnum.RIDER;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: SwiftAppBar(
        toolbarHeight: SwiftAppBar.toolbarHeightFor(context),
        title: const SwiftBrand(onBar: true),
        automaticallyImplyLeading: false,
      ),
      body: AppGlassBackground(
        child: SafeArea(
          top: false,
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 560),
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 28),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const WelcomePageImageWidget(),
                    const SizedBox(height: 32),
                    const WelcomeTextWidget(),
                    const SizedBox(height: 28),
                    Text(
                      'Choose your account',
                      textAlign: TextAlign.center,
                      style: theme.textTheme.titleSmall,
                    ),
                    const SizedBox(height: 12),
                    LayoutBuilder(
                      builder: (context, constraints) {
                        final choices = [
                          for (final role in RoleEnum.values)
                            _RoleLabel(
                              icon: role == RoleEnum.RIDER
                                  ? SwiftIcons.userRound
                                  : SwiftIcons.car,
                              label: role.name,
                              description: role == RoleEnum.RIDER
                                  ? 'Find your next ride'
                                  : 'Drive with Swift',
                              selected: role == _selectedRole,
                              onTap: () {
                                if (role == _selectedRole) return;
                                HapticFeedback.selectionClick();
                                setState(() => _selectedRole = role);
                              },
                            ),
                        ];
                        final scaledWidth =
                            constraints.maxWidth /
                            MediaQuery.textScalerOf(context).scale(1);
                        if (scaledWidth < 250) {
                          return Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              choices.first,
                              const SizedBox(height: 12),
                              choices.last,
                            ],
                          );
                        }
                        return IntrinsicHeight(
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              Expanded(child: choices.first),
                              const SizedBox(width: 12),
                              Expanded(child: choices.last),
                            ],
                          ),
                        );
                      },
                    ),
                    const SizedBox(height: 24),
                    WelcomePageAuthButtonsWidget(selectedRole: _selectedRole),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _RoleLabel extends StatelessWidget {
  final SwiftIcons icon;
  final String label;
  final String description;
  final bool selected;
  final VoidCallback onTap;

  const _RoleLabel({
    required this.icon,
    required this.label,
    required this.description,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) => Semantics(
    selected: selected,
    button: true,
    child: GlassCard(
      onTap: onTap,
      border: Border.all(
        color: selected
            ? Theme.of(context).colorScheme.primary
            : Theme.of(context).dividerColor,
        width: selected ? 1.5 : 1,
      ),
      tint: selected
          ? Theme.of(context).colorScheme.primary.withValues(alpha: .06)
          : Theme.of(context).colorScheme.surface,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          AppIcon(icon, size: 20, color: Theme.of(context).colorScheme.primary),
          const SizedBox(height: 10),
          Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(
              context,
            ).textTheme.labelLarge?.copyWith(fontSize: 13),
          ),
          const SizedBox(height: 6),
          Text(
            description,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ],
      ),
    ),
  );
}
