import 'package:swift_app/features/common/shared/widgets/glass_ui.dart';
import 'package:swift_app/features/common/shared/widgets/auth_frame.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:flutter/material.dart';

/// One keyboard-safe layout for authentication, without nested blur layers.
class AuthScaffold extends StatelessWidget {
  final Widget child;
  final Widget leading;

  const AuthScaffold({super.key, required this.child, required this.leading});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: SwiftAppBar(
        toolbarHeight: SwiftAppBar.toolbarHeightFor(context),
        title: const SwiftBrand(onBar: true),
        automaticallyImplyLeading: false,
        leading: leading,
      ),
      body: AppGlassBackground(
        child: SafeArea(
          top: false,
          child: LayoutBuilder(
            builder: (context, constraints) => SingleChildScrollView(
              keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
              child: ConstrainedBox(
                constraints: BoxConstraints(
                  minHeight: (constraints.maxHeight - 40).clamp(
                    0,
                    double.infinity,
                  ),
                ),
                child: Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 520),
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(0, 18, 0, 24),
                      child: child,
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
}
