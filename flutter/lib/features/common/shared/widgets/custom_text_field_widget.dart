import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

String? validateEmail(String? value) {
  final email = value?.trim() ?? '';
  if (email.isEmpty) return 'Enter your email address';
  if (!RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$').hasMatch(email)) {
    return 'Enter a valid email, like name@example.com';
  }
  return null;
}

class CustomTextFieldProps {
  final String fieldName;
  final String placeHolder;
  final bool isSecure;
  final bool? isPhone;
  final TextEditingController textEditingController;
  final FormFieldValidator<String>? validator;
  final TextInputType? keyboardType;
  final TextInputAction textInputAction;
  final Iterable<String>? autofillHints;
  final ValueChanged<String>? onSubmitted;
  final TextCapitalization textCapitalization;
  final bool enabled;
  final IconData? prefixIcon;

  const CustomTextFieldProps({
    required this.fieldName,
    required this.placeHolder,
    required this.isSecure,
    this.isPhone,
    required this.textEditingController,
    this.validator,
    this.keyboardType,
    this.textInputAction = TextInputAction.next,
    this.autofillHints,
    this.onSubmitted,
    this.textCapitalization = TextCapitalization.none,
    this.enabled = true,
    this.prefixIcon,
  });
}

class CustomTextFieldWidget extends StatefulWidget {
  final CustomTextFieldProps props;
  const CustomTextFieldWidget({super.key, required this.props});

  @override
  State<CustomTextFieldWidget> createState() => _CustomTextFieldWidgetState();
}

class _CustomTextFieldWidgetState extends State<CustomTextFieldWidget> {
  late bool _obscureText;

  @override
  void initState() {
    super.initState();
    _obscureText = widget.props.isSecure;
  }

  @override
  Widget build(BuildContext context) {
    final props = widget.props;
    final theme = Theme.of(context);
    final isPhone = props.isPhone ?? false;
    final isEmail = props.keyboardType == TextInputType.emailAddress;
    return TextFormField(
      controller: props.textEditingController,
      enabled: props.enabled,
      obscureText: _obscureText,
      validator: props.validator,
      autovalidateMode: AutovalidateMode.onUserInteraction,
      keyboardType:
          props.keyboardType ??
          (isPhone ? TextInputType.phone : TextInputType.text),
      textInputAction: props.textInputAction,
      autofillHints: props.autofillHints,
      textCapitalization: props.textCapitalization,
      autocorrect: !isEmail && !isPhone && !props.isSecure,
      enableSuggestions: !isEmail && !isPhone && !props.isSecure,
      onFieldSubmitted: props.onSubmitted,
      inputFormatters: isPhone
          ? [FilteringTextInputFormatter.digitsOnly]
          : null,
      maxLength: isPhone ? 10 : null,
      style: theme.textTheme.bodyMedium?.copyWith(
        color: Theme.of(context).colorScheme.onSurface,
      ),
      decoration: InputDecoration(
        labelText: props.fieldName,
        hintText: props.placeHolder,
        floatingLabelBehavior: FloatingLabelBehavior.auto,
        counterText: '',
        errorMaxLines: 3,
        prefixText: isPhone ? '+91  ' : null,
        prefixIcon: props.prefixIcon == null
            ? null
            : Icon(props.prefixIcon, size: 18),
        prefixIconConstraints: props.prefixIcon == null
            ? null
            : const BoxConstraints(minWidth: 44, minHeight: 44),
        suffixIcon: props.isSecure
            ? IconButton(
                tooltip: _obscureText ? 'Show password' : 'Hide password',
                icon: AppIcon(
                  _obscureText ? SwiftIcons.eyeOff : SwiftIcons.eye,
                  size: 19,
                ),
                onPressed: props.enabled
                    ? () => setState(() => _obscureText = !_obscureText)
                    : null,
              )
            : null,
      ),
    );
  }
}
