import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_bloc.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_event.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_state.dart';
import 'package:swift_app/features/common/authentication/login/presentation/widgets/login_desc_text_widget.dart';
import 'package:swift_app/features/common/authentication/login/presentation/widgets/login_fields_widget.dart';
import 'package:swift_app/features/common/authentication/login/presentation/widgets/login_signup_text_widget.dart';
import 'package:swift_app/features/common/authentication/otp/presentation/pages/otp_page.dart';
import 'package:swift_app/features/common/shared/widgets/auth_scaffold.dart';
import 'package:swift_app/features/common/shared/widgets/buttons/primary_button.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:get/get.dart';

class LoginControllers {
  final TextEditingController emailController = TextEditingController();

  void dispose() {
    emailController.dispose();
  }
}

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final LoginControllers controller = LoginControllers();
  final _formKey = GlobalKey<FormState>();

  bool _requestPending = false;

  @override
  void initState() {
    super.initState();

    final previous = Get.arguments;

    if (previous is String) {
      controller.emailController.text = previous;
    }
  }

  void handleSendOtp(BuildContext context) {
    if (_requestPending || context.read<AuthBloc>().state is AuthLoading) {
      return;
    }

    if (!(_formKey.currentState?.validate() ?? false)) {
      return;
    }

    FocusScope.of(context).unfocus();

    setState(() {
      _requestPending = true;
    });

    context.read<AuthBloc>().add(
      AuthLoginOtpRequested(controller.emailController.text.trim()),
    );
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<AuthBloc, AuthState>(
      listener: (context, state) {
        if (ModalRoute.of(context)?.isCurrent != true) {
          return;
        }

        if (state is! AuthLoading && _requestPending) {
          setState(() {
            _requestPending = false;
          });
        }

        if (state is AuthOtpSent && state.session.isLogin) {
          Get.offNamed(
            '/otpPage',
            arguments: OtpPageProps(email: state.session.email, pageType: 2),
          );
        }
      },
      builder: (context, state) {
        final busy = _requestPending || state is AuthLoading;

        return AuthScaffold(
          leading: const _IOSBackButton(),
          child: AutofillGroup(
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const LoginDescTextWidget(),

                  const SizedBox(height: 34),

                  AnimatedSwitcher(
                    duration: MediaQuery.disableAnimationsOf(context)
                        ? Duration.zero
                        : const Duration(milliseconds: 250),
                    child:
                        state is AuthFailure &&
                            state.action == AuthAction.sendOtp
                        ? Padding(
                            key: const ValueKey('error'),
                            padding: const EdgeInsets.only(bottom: 14),
                            child: _IOSErrorBox(message: state.message),
                          )
                        : const SizedBox.shrink(key: ValueKey('no_error')),
                  ),

                  LoginFieldsWidget(
                    loginController: controller,
                    enabled: !busy,
                    onSubmit: () => handleSendOtp(context),
                  ),

                  const SizedBox(height: 18),

                  PrimaryButton(
                    primaryButtonProps: PrimaryButtonProps(
                      text: busy ? 'Sending…' : 'Continue',
                      loading: busy,
                      icon: busy
                          ? null
                          : const AppIcon(SwiftIcons.arrowRight, size: 18),
                      onClick: busy ? null : () => handleSendOtp(context),
                    ),
                  ),

                  const SizedBox(height: 24),

                  const _IOSDivider(),

                  const SizedBox(height: 19),

                  LoginSignupTextWidget(
                    onSignup: busy ? null : () => Get.offNamed('/signup'),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

class _IOSBackButton extends StatelessWidget {
  const _IOSBackButton();

  @override
  Widget build(BuildContext context) {
    return SwiftAppBarAction(
      icon: SwiftIcons.chevronLeft,
      tooltip: 'Back',
      onPressed: () => Get.back(),
    );
  }
}

class _IOSDivider extends StatelessWidget {
  const _IOSDivider();

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Container(
            height: 1,
            color: Theme.of(context).colorScheme.outlineVariant,
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12),
          child: Text(
            'or',
            style: TextStyle(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
              fontSize: 13,
              fontWeight: FontWeight.w500,
            ),
          ),
        ),
        Expanded(
          child: Container(
            height: 1,
            color: Theme.of(context).colorScheme.outlineVariant,
          ),
        ),
      ],
    );
  }
}

class _IOSErrorBox extends StatelessWidget {
  final String message;

  const _IOSErrorBox({required this.message});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: const Color(0xFFFF3B30).withValues(alpha: 0.10),
        borderRadius: AppRadii.cardRadius,
        border: Border.all(
          color: const Color(0xFFFF3B30).withValues(alpha: 0.18),
        ),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const AppIcon(
            SwiftIcons.circleAlert,
            color: Color(0xFFFF3B30),
            size: 18,
          ),
          const SizedBox(width: 9),
          Expanded(
            child: Text(
              message,
              style: const TextStyle(
                color: Color(0xFFFF3B30),
                fontWeight: FontWeight.w500,
                fontSize: 13,
                height: 1.35,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
