import 'package:swift_app/features/common/shared/widgets/app_icon.dart';
import 'package:swift_app/features/common/shared/widgets/swift_app_bar.dart';
import 'dart:async';

import 'package:swift_app/features/common/authentication/auth/domain/entity/auth_otp_session.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_bloc.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_event.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_state.dart';
import 'package:swift_app/features/common/authentication/otp/presentation/widgets/otp_input_widget.dart';
import 'package:swift_app/features/common/authentication/otp/presentation/widgets/otp_resend_widget.dart';
import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_request.dart';
import 'package:swift_app/features/common/shared/widgets/auth_scaffold.dart';
import 'package:swift_app/features/common/shared/widgets/buttons/primary_button.dart';
import 'package:swift_app/themes/app_radii.dart';
import 'package:swift_app/utils/app_messenger.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:get/get.dart';

class OtpPageProps {
  final String email;

  /// 1 = sign up
  /// 2 = sign in
  final int pageType;

  final SignupRequest? signupRequest;

  const OtpPageProps({
    required this.email,
    required this.pageType,
    this.signupRequest,
  });
}

class OtpPage extends StatefulWidget {
  const OtpPage({super.key});

  @override
  State<OtpPage> createState() => _OtpPageState();
}

class _OtpPageState extends State<OtpPage> with WidgetsBindingObserver {
  final TextEditingController otpController = TextEditingController();

  final _formKey = GlobalKey<FormState>();

  Timer? _resendTimer;
  DateTime? _resendAvailableAt;

  final ValueNotifier<int> _secondsRemaining = ValueNotifier<int>(30);

  bool _requestPending = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _startResendTimer();
  }

  void _startResendTimer() {
    _resendTimer?.cancel();

    // The notifier drives only the resend row, so the ticking countdown no
    // longer rebuilds the whole page on every second.
    _secondsRemaining.value = 30;
    _resendAvailableAt = DateTime.now().add(const Duration(seconds: 30));

    _scheduleResendTimer();
  }

  void _scheduleResendTimer() {
    _resendTimer?.cancel();
    if (_secondsRemaining.value == 0) return;

    _resendTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) {
        timer.cancel();
        return;
      }

      final int next = _secondsRemaining.value - 1;

      _secondsRemaining.value = next <= 0 ? 0 : next;

      if (next <= 0) {
        timer.cancel();
      }
    });
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state != AppLifecycleState.resumed) {
      _resendTimer?.cancel();
      return;
    }
    final deadline = _resendAvailableAt;
    if (deadline == null) return;
    final remaining =
        (deadline.difference(DateTime.now()).inMilliseconds / 1000).ceil();
    _secondsRemaining.value = remaining.clamp(0, _secondsRemaining.value);
    _scheduleResendTimer();
  }

  AuthOtpSession? _authSessionFromProps(OtpPageProps? props) {
    if (props == null) return null;

    if (props.pageType == 1 && props.signupRequest != null) {
      return AuthOtpSession.signup(props.signupRequest!);
    }

    if (props.pageType == 2) {
      return AuthOtpSession.login(props.email);
    }

    return null;
  }

  void _onVerifyOtp(BuildContext context, AuthOtpSession session) {
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
      AuthOtpSubmitted(session: session, otp: otpController.text.trim()),
    );
  }

  void _onResend(BuildContext context, AuthOtpSession session) {
    if (_requestPending ||
        _secondsRemaining.value > 0 ||
        context.read<AuthBloc>().state is AuthLoading) {
      return;
    }

    setState(() {
      _requestPending = true;
    });

    context.read<AuthBloc>().add(AuthOtpResendRequested(session));
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _resendTimer?.cancel();
    otpController.dispose();
    _secondsRemaining.dispose();

    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final arguments = Get.arguments;

    final props = arguments is OtpPageProps ? arguments : null;

    final session = _authSessionFromProps(props);

    if (session == null || props == null) {
      return _IOSOtpScaffold(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const _OtpIcon(),

            const SizedBox(height: 25),

            const Text(
              'Code unavailable',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 30,
                fontWeight: FontWeight.w700,
                letterSpacing: -1,
              ),
            ),

            const SizedBox(height: 9),

            const Text(
              'Request a new verification code to continue.',
              textAlign: TextAlign.center,
            ),

            const SizedBox(height: 30),

            PrimaryButton(
              primaryButtonProps: PrimaryButtonProps(
                text: 'Go to sign in',
                onClick: () => Get.offNamed('/login'),
              ),
            ),
          ],
        ),
      );
    }

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

        if (state is AuthAuthenticated) {
          TextInput.finishAutofillContext();
          Get.offAllNamed('/home');
        } else if (state is AuthFailure && state.action == AuthAction.login) {
          // The server may have claimed the code before a response/storage
          // failure. Let the user recover with a fresh code immediately.
          _resendTimer?.cancel();
          _resendAvailableAt = null;
          _secondsRemaining.value = 0;
        } else if (state is AuthOtpSent) {
          otpController.clear();

          _startResendTimer();

          AppMessenger.showSuccess('New code sent.');
        }
      },
      builder: (context, state) {
        final busy = _requestPending || state is AuthLoading;

        final resending =
            state is AuthLoading && state.action == AuthAction.sendOtp;

        return _IOSOtpScaffold(
          child: AutofillGroup(
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const _OtpIcon(),

                  const SizedBox(height: 23),

                  _OtpHeading(email: props.email),

                  const SizedBox(height: 26),

                  _EmailGlassChip(
                    email: props.email,
                    enabled: !busy,
                    onChangeEmail: () {
                      Get.offNamed(
                        session.isSignup ? '/signup' : '/login',
                        arguments: session.isSignup
                            ? props.signupRequest
                            : props.email,
                      );
                    },
                  ),

                  const SizedBox(height: 24),

                  AnimatedSwitcher(
                    duration: MediaQuery.disableAnimationsOf(context)
                        ? Duration.zero
                        : const Duration(milliseconds: 220),
                    child: state is AuthFailure
                        ? Padding(
                            key: const ValueKey('otp-error'),
                            padding: const EdgeInsets.only(bottom: 17),
                            child: _OtpErrorBox(message: state.message),
                          )
                        : const SizedBox.shrink(key: ValueKey('otp-normal')),
                  ),

                  OtpInputWidget(
                    controller: otpController,
                    enabled: !busy,
                    onSubmit: () => _onVerifyOtp(context, session),
                  ),

                  const SizedBox(height: 22),

                  PrimaryButton(
                    primaryButtonProps: PrimaryButtonProps(
                      text: busy && !resending ? 'Verifying...' : 'Verify',
                      loading: busy && !resending,
                      icon: busy && !resending
                          ? null
                          : const AppIcon(SwiftIcons.check, size: 18),
                      onClick: busy
                          ? null
                          : () => _onVerifyOtp(context, session),
                    ),
                  ),

                  const SizedBox(height: 22),

                  ValueListenableBuilder<int>(
                    valueListenable: _secondsRemaining,
                    builder: (context, seconds, _) {
                      return OtpResendWidget(
                        isLoading: resending,
                        secondsRemaining: seconds,
                        onResend: busy
                            ? null
                            : () => _onResend(context, session),
                      );
                    },
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

class _IOSOtpScaffold extends StatelessWidget {
  final Widget child;

  const _IOSOtpScaffold({required this.child});

  @override
  Widget build(BuildContext context) =>
      AuthScaffold(leading: const _OTPBackButton(), child: child);
}

class _OTPBackButton extends StatelessWidget {
  const _OTPBackButton();

  @override
  Widget build(BuildContext context) {
    return SwiftAppBarAction(
      icon: SwiftIcons.chevronLeft,
      tooltip: 'Back',
      onPressed: Get.back,
    );
  }
}

class _OtpIcon extends StatelessWidget {
  const _OtpIcon();

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Center(
      child: Container(
        width: 48,
        height: 48,
        decoration: BoxDecoration(
          color: scheme.surface,
          borderRadius: AppRadii.cardRadius,
          border: Border.all(color: scheme.outlineVariant),
        ),
        child: AppIcon(SwiftIcons.mail, color: scheme.primary, size: 20),
      ),
    );
  }
}

class _OtpHeading extends StatelessWidget {
  final String email;

  const _OtpHeading({required this.email});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(
          'Check your email',
          textAlign: TextAlign.center,
          style: TextStyle(
            color: Theme.of(context).colorScheme.onSurface,
            fontSize: 30,
            fontWeight: FontWeight.w700,
            letterSpacing: -1,
            height: 1.08,
          ),
        ),

        const SizedBox(height: 9),

        Text(
          'Enter the verification code we sent to your email.',
          textAlign: TextAlign.center,
          style: TextStyle(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
            fontSize: 14.5,
            height: 1.45,
          ),
        ),
      ],
    );
  }
}

class _EmailGlassChip extends StatelessWidget {
  final String email;
  final bool enabled;
  final VoidCallback onChangeEmail;

  const _EmailGlassChip({
    required this.email,
    required this.enabled,
    required this.onChangeEmail,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(13, 8, 8, 8),
      decoration: BoxDecoration(
        borderRadius: AppRadii.cardRadius,
        color: Theme.of(context).colorScheme.surface,
        border: Border.all(color: Theme.of(context).colorScheme.outlineVariant),
      ),
      child: Row(
        children: [
          AppIcon(
            SwiftIcons.mail,
            size: 18,
            color: Theme.of(context).colorScheme.primary,
          ),

          const SizedBox(width: 9),

          Expanded(
            child: Text(
              email,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
                fontSize: 13.5,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),

          CupertinoButton(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
            minimumSize: const Size(44, 44),
            onPressed: enabled ? onChangeEmail : null,
            child: Text(
              'Change',
              style: TextStyle(
                color: enabled
                    ? Theme.of(context).colorScheme.primary
                    : Theme.of(
                        context,
                      ).colorScheme.primary.withValues(alpha: 0.35),
                fontSize: 13,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _OtpErrorBox extends StatelessWidget {
  final String message;

  const _OtpErrorBox({required this.message});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 11),
      decoration: BoxDecoration(
        color: const Color(0xFFFF3B30).withValues(alpha: 0.09),
        borderRadius: AppRadii.cardRadius,
        border: Border.all(
          color: const Color(0xFFFF3B30).withValues(alpha: 0.17),
        ),
      ),
      child: Row(
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
                fontSize: 13,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
