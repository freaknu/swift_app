import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/constants/enums/role_enum.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_bloc.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_event.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_state.dart';
import 'package:swift_app/features/common/authentication/otp/presentation/pages/otp_page.dart';
import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_request.dart';
import 'package:swift_app/features/common/authentication/signup/presentation/widgets/signup_desc_text_widget.dart';
import 'package:swift_app/features/common/authentication/signup/presentation/widgets/signup_fields_widget.dart';
import 'package:swift_app/features/common/authentication/signup/presentation/widgets/signup_login_text_widget.dart';
import 'package:swift_app/features/common/shared/widgets/auth_frame.dart';
import 'package:swift_app/features/common/shared/widgets/auth_scaffold.dart';
import 'package:swift_app/features/common/shared/widgets/buttons/primary_button.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:get/get.dart';

class SignUpControllers {
  final TextEditingController nameController = TextEditingController();
  final TextEditingController emailController = TextEditingController();
  final TextEditingController phoneController = TextEditingController();

  void dispose() {
    nameController.dispose();
    emailController.dispose();
    phoneController.dispose();
  }
}

class SignupPage extends StatefulWidget {
  final RoleEnum initialRole;

  const SignupPage({super.key, this.initialRole = RoleEnum.RIDER});

  @override
  State<SignupPage> createState() => _SignupPageState();
}

class _SignupPageState extends State<SignupPage> {
  final SignUpControllers controllers = SignUpControllers();
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  bool _requestPending = false;
  late RoleEnum _selectedRole = widget.initialRole;

  @override
  void initState() {
    super.initState();
    final previous = Get.arguments;
    if (previous is SignupRequest) {
      controllers.nameController.text = previous.name;
      controllers.emailController.text = previous.email;
      controllers.phoneController.text = previous.phoneNumber;
      _selectedRole = previous.role;
    } else if (previous is RoleEnum) {
      _selectedRole = previous;
    } else if (previous is String) {
      _selectedRole = RoleEnum.values.firstWhere(
        (role) => role.name == previous.toUpperCase(),
        orElse: () => widget.initialRole,
      );
    }
  }

  void _handleSendOtp(BuildContext context) {
    if (_requestPending || context.read<AuthBloc>().state is AuthLoading) {
      return;
    }
    if (!(_formKey.currentState?.validate() ?? false)) return;
    FocusScope.of(context).unfocus();
    setState(() => _requestPending = true);
    context.read<AuthBloc>().add(
      AuthSignupOtpRequested(
        SignupRequest(
          email: controllers.emailController.text.trim(),
          name: controllers.nameController.text.trim(),
          phoneNumber: controllers.phoneController.text.trim(),
          role: _selectedRole,
        ),
      ),
    );
  }

  @override
  void dispose() {
    controllers.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<AuthBloc, AuthState>(
      listener: (context, state) {
        if (ModalRoute.of(context)?.isCurrent != true) return;
        if (state is! AuthLoading && _requestPending) {
          setState(() => _requestPending = false);
        }
        if (state is AuthOtpSent && state.session.isSignup) {
          Get.offNamed(
            '/otpPage',
            arguments: OtpPageProps(
              email: state.session.email,
              pageType: 1,
              signupRequest: state.session.signupRequest,
            ),
          );
        }
      },
      builder: (context, state) {
        final busy = _requestPending || state is AuthLoading;
        return AuthScaffold(
          leading: SwiftAppBarAction(
            icon: SwiftIcons.chevronLeft,
            tooltip: 'Back',
            onPressed: () => Get.back(),
          ),
          child: AutofillGroup(
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const SignupDescTextWidget(),
                  const SizedBox(height: 24),
                  if (state is AuthFailure &&
                      state.action == AuthAction.sendOtp)
                    AuthErrorMessage(message: state.message),
                  _SelectedRoleBanner(role: _selectedRole),
                  const SizedBox(height: 20),
                  SignupFieldsWidget(
                    signUpControllers: controllers,
                    enabled: !busy,
                    onSubmit: () => _handleSendOtp(context),
                  ),
                  const SizedBox(height: 20),
                  PrimaryButton(
                    primaryButtonProps: PrimaryButtonProps(
                      text: busy ? 'Sending…' : 'Continue',
                      loading: busy,
                      icon: busy
                          ? null
                          : const AppIcon(SwiftIcons.arrowRight, size: 18),
                      onClick: busy ? null : () => _handleSendOtp(context),
                    ),
                  ),
                  const SizedBox(height: 24),
                  SignupLoginTextWidget(enabled: !busy),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

class _SelectedRoleBanner extends StatelessWidget {
  final RoleEnum role;
  const _SelectedRoleBanner({required this.role});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDriver = role == RoleEnum.DRIVER;
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: theme.colorScheme.primary.withValues(alpha: 0.08),
        borderRadius: AppRadii.cardRadius,
      ),
      child: Row(
        children: [
          AppIcon(
            isDriver ? SwiftIcons.car : SwiftIcons.userRound,
            size: 20,
            color: theme.colorScheme.primary,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Signing up as ${isDriver ? 'Driver' : 'Rider'}',
                  style: theme.textTheme.titleSmall,
                ),
                const SizedBox(height: 3),
                Text(
                  isDriver
                      ? 'You’ll use Swift to drive'
                      : 'You’ll use Swift to book rides',
                  style: theme.textTheme.bodySmall,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
