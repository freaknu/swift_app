import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:flutter/material.dart';

class PrimaryBackButtonProps {
  final String text;
  final VoidCallback? onClick;
  const PrimaryBackButtonProps({required this.text, this.onClick});
}

class PrimaryBackButton extends StatelessWidget {
  final PrimaryBackButtonProps props;
  const PrimaryBackButton({super.key, required this.props});

  @override
  Widget build(BuildContext context) {
    final navigator = Navigator.of(context);
    if (props.onClick == null && !navigator.canPop()) {
      return const SizedBox(width: 44, height: 44);
    }
    return SwiftAppBarAction(
      icon: SwiftIcons.chevronLeft,
      onPressed: props.onClick ?? () => navigator.maybePop(),
      tooltip: props.text,
    );
  }
}
