import 'dart:async';
import 'dart:io' show Platform;
import 'package:swift_app/core/performance/ui_frame_profiler.dart';

import 'package:swift_app/core/di/injection_container.dart';
import 'package:swift_app/core/notifications/push_notification_service.dart';
import 'package:swift_app/core/notifications/chat_push_destination.dart';

import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_bloc.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_home_ready_datasource.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_event.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_state.dart';

import 'package:swift_app/features/common/authentication/forgetpassword/presentation/pages/change_password_page.dart';
import 'package:swift_app/features/common/authentication/forgetpassword/presentation/pages/forget_password_page.dart';
import 'package:swift_app/features/common/authentication/login/presentation/pages/login_page.dart';
import 'package:swift_app/features/common/authentication/otp/presentation/pages/otp_page.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_bloc.dart';
import 'package:swift_app/features/common/chat/data/datasource/chat_realtime_service.dart';
import 'package:swift_app/features/common/chat/data/datasource/chat_remote_datasource.dart';
import 'package:swift_app/features/common/chat/presentation/pages/chat_page.dart';
import 'package:swift_app/features/common/authentication/signup/presentation/pages/signup_page.dart';
import 'package:swift_app/features/common/shared/widgets/app_loading_page.dart';
import 'package:swift_app/features/common/splash/welcome/presentation/pages/welcome_page.dart';
import 'package:swift_app/features/driver/data/datasource/driver_realtime_service.dart';
import 'package:swift_app/features/driver/data/datasource/driver_location_publisher.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';
import 'package:swift_app/features/driver/presentation/pages/driver_home_page.dart';

import 'package:swift_app/features/rider/home/domain/entity/offer_request.dart';

import 'package:swift_app/features/rider/home/presentation/blocs/offer_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/offer_event.dart';

import 'package:swift_app/features/rider/home/presentation/blocs/ride_history_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_history_event.dart';

import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_event.dart';

import 'package:swift_app/features/rider/home/presentation/blocs/user_bloc.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_event.dart';

import 'package:swift_app/features/rider/ride/presentation/blocs/ride_bloc.dart';

import 'package:swift_app/features/rider/widgets/main_page.dart';

import 'package:swift_app/themes/app_colors.dart';
import 'package:swift_app/themes/app_theme.dart';
import 'package:swift_app/themes/theme_controller.dart';

import 'package:swift_app/utils/app_messenger.dart';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter_bloc/flutter_bloc.dart' hide Transition;

import 'package:get/get.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:google_maps_flutter_android/google_maps_flutter_android.dart';
import 'package:google_maps_flutter_platform_interface/google_maps_flutter_platform_interface.dart';

void _configureGoogleMaps() {
  if (!Platform.isAndroid) return;

  GoogleMapsFlutterAndroid.registerWith();

  final mapsImplementation =
      GoogleMapsFlutterPlatform.instance as GoogleMapsFlutterAndroid;

  mapsImplementation.useAndroidViewSurface = true;
}

SystemUiOverlayStyle _systemUiStyle(Brightness brightness) {
  final bool isDark = brightness == Brightness.dark;

  return SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,

    statusBarIconBrightness: isDark ? Brightness.light : Brightness.dark,

    statusBarBrightness: isDark ? Brightness.dark : Brightness.light,

    systemNavigationBarColor: isDark
        ? AppColors.darkPageColor
        : AppColors.lightPageColor,

    systemNavigationBarDividerColor: Colors.transparent,

    systemNavigationBarIconBrightness: isDark
        ? Brightness.light
        : Brightness.dark,

    systemNavigationBarContrastEnforced: false,
  );
}

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  UiFrameProfiler.instance.start();

  _configureGoogleMaps();

  await SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);

  await initDependencies();

  Get.put(
    ThemeController(preferences: sl<SharedPreferences>()),
    permanent: true,
  );

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  static final ThemeData _lightTheme = AppTheme.light();
  static final ThemeData _darkTheme = AppTheme.dark();

  @override
  Widget build(BuildContext context) {
    final ThemeController themeController = Get.find<ThemeController>();

    return BlocProvider<AuthBloc>(
      create: (_) => sl<AuthBloc>()..add(const AuthStarted()),

      child: Obx(() {
        final ThemeMode currentThemeMode = themeController.themeMode;

        return GetMaterialApp(
          debugShowCheckedModeBanner: false,

          title: 'Swift',
          navigatorObservers: [
            if (UiFrameProfiler.enabled)
              UiFrameProfiler.instance.navigatorObserver,
          ],

          theme: _lightTheme,
          darkTheme: _darkTheme,
          themeMode: currentThemeMode,

          scaffoldMessengerKey: AppMessenger.key,

          builder: (context, child) {
            final Brightness brightness = Theme.of(context).brightness;

            return AnnotatedRegion<SystemUiOverlayStyle>(
              value: _systemUiStyle(brightness),

              child: ColoredBox(
                color: Theme.of(context).scaffoldBackgroundColor,
                child: child ?? const SizedBox.shrink(),
              ),
            );
          },

          getPages: [
            GetPage(
              name: '/login',

              page: () => const LoginPage(),

              transition: Transition.cupertino,

              transitionDuration: const Duration(milliseconds: 320),
            ),

            GetPage(
              name: '/signup',

              page: () => const SignupPage(),

              transition: Transition.cupertino,

              transitionDuration: const Duration(milliseconds: 320),
            ),

            GetPage(
              name: '/otpPage',

              page: () => const OtpPage(),

              transition: Transition.cupertino,

              transitionDuration: const Duration(milliseconds: 320),
            ),

            GetPage(
              name: '/forgetPassword',

              page: () => ForgetPasswordPage(),

              transition: Transition.cupertino,

              transitionDuration: const Duration(milliseconds: 320),
            ),

            GetPage(
              name: '/changePasswordPage',

              page: () => ChangePasswordPage(),

              transition: Transition.cupertino,

              transitionDuration: const Duration(milliseconds: 320),
            ),

            GetPage(
              name: '/home',

              page: () => const RoleHomePage(),

              transition: Transition.cupertino,

              transitionDuration: const Duration(milliseconds: 320),
            ),
          ],

          home: const AuthGate(),
        );
      }),
    );
  }
}

class AuthGate extends StatelessWidget {
  const AuthGate({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<AuthBloc, AuthState>(
      builder: (BuildContext context, AuthState state) {
        if (state is AuthInitial ||
            (state is AuthLoading && state.action == AuthAction.checking)) {
          return const AppLoadingPage(message: 'Starting Swift…');
        }

        /// Already logged in.
        if (state is AuthAuthenticated) {
          return const RoleHomePage();
        }

        /// Not logged in.
        return const WelcomePage();
      },
    );
  }
}

class RoleHomePage extends StatefulWidget {
  const RoleHomePage({super.key});

  @override
  State<RoleHomePage> createState() => _RoleHomePageState();
}

class _RoleHomePageState extends State<RoleHomePage> {
  StreamSubscription<void>? _pushOpenSubscription;

  @override
  void initState() {
    super.initState();
    final push = sl<PushNotificationService>();
    _pushOpenSubscription = push.opens.listen((_) => _openPushDestination());
    WidgetsBinding.instance.addPostFrameCallback((_) => _openPushDestination());
  }

  @override
  void dispose() {
    _pushOpenSubscription?.cancel();
    super.dispose();
  }

  void _openPushDestination() {
    if (!mounted) return;
    final data = sl<PushNotificationService>().takePendingOpen();
    if (data == null) return;
    if (_isChatPush(data)) {
      final authState = context.read<AuthBloc>().state;
      final role = authState is AuthAuthenticated
          ? authState.user?.role.toUpperCase()
          : null;
      final destination = ChatPushDestination.fromData(data, role);
      if (destination != null) {
        Navigator.of(context).push(
          CupertinoPageRoute(
            builder: (_) => ChatPage(
              rideId: destination.rideId,
              currentUserId: destination.currentProfileId,
              role: destination.role,
              rideStatus: destination.rideStatus ?? 'STARTED',
              riderId: destination.riderId,
              driverId: destination.driverId,
              datasource: sl<ChatRemoteDatasource>(),
              realtime: sl<ChatRealtimeService>(),
            ),
          ),
        );
        return;
      }
    }
    // Recreate the role home so rider active-ride recovery and driver offer
    // polling run immediately after a notification tap.
    Get.offNamed('/home');
  }

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<AuthBloc, AuthState>(
      builder: (context, state) {
        if (state is! AuthAuthenticated) {
          return const WelcomePage();
        }

        final role = state.user?.role.toUpperCase();

        if (role == 'DRIVER') {
          return DriverHomePage(
            onHomeReady: sl<AuthHomeReadyDatasource>().acknowledge,
            datasource: sl<DriverRemoteDatasource>(),
            realtimeService: sl<DriverRealtimeService>(),
            locationPublisher: sl<DriverLocationPublisher>(),
            chatDatasource: sl<ChatRemoteDatasource>(),
            chatRealtime: sl<ChatRealtimeService>(),
          );
        }

        return MainPageWithProviders(
          onHomeReady: sl<AuthHomeReadyDatasource>().acknowledge,
        );
      },
    );
  }
}

bool _isChatPush(Map<String, dynamic> data) {
  final type = data['type']?.toString().toUpperCase();
  return type == 'CHAT' ||
      type == 'CHAT_MESSAGE' ||
      data['sessionId'] != null ||
      data['messageContent'] != null;
}

class MainPageWithProviders extends StatelessWidget {
  const MainPageWithProviders({super.key, this.onHomeReady});

  final Future<bool> Function()? onHomeReady;

  @override
  Widget build(BuildContext context) {
    return MultiBlocProvider(
      providers: [
        BlocProvider<UserBloc>(
          create: (_) => sl<UserBloc>()..add(const FetchUserDetails()),
        ),

        BlocProvider<RideTypeBloc>(
          create: (_) => sl<RideTypeBloc>()..add(const FetchRideTypes()),
        ),

        BlocProvider<RideHistoryBloc>(
          create: (_) => sl<RideHistoryBloc>()..add(const FetchRideHistory()),
        ),

        BlocProvider<OfferBloc>(
          create: (_) => sl<OfferBloc>()
            ..add(
              FetchOffers(request: OfferRequest(startFrom: DateTime.now())),
            ),
        ),

        BlocProvider<FareBloc>(create: (_) => sl<FareBloc>()),

        BlocProvider<RideBloc>(create: (_) => sl<RideBloc>()),
      ],

      child: MainPage(onHomeReady: onHomeReady),
    );
  }
}
