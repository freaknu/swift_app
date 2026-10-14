import 'package:swift_app/API/dio_client.dart';
import 'package:swift_app/core/notifications/push_device_registration.dart';
import 'package:swift_app/core/notifications/push_notification_service.dart';
import 'package:swift_app/core/notifications/notification_inbox_datasource.dart';
import 'package:swift_app/API/rider_api_endpoints.dart';
import 'package:swift_app/features/common/chat/data/datasource/chat_realtime_service.dart';
import 'package:swift_app/features/common/chat/data/datasource/chat_remote_datasource.dart';

import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/common/authentication/auth/presentation/bloc/auth_bloc.dart';

import 'package:swift_app/features/common/authentication/login/data/datasource/login_datasource.dart';
import 'package:swift_app/features/common/authentication/login/data/repository/login_repository_impl.dart';
import 'package:swift_app/features/common/authentication/login/domain/repository/login_repository.dart';
import 'package:swift_app/features/common/authentication/login/domain/usecase/login_usecase.dart';

import 'package:swift_app/features/common/authentication/otp/data/datasource/otp_datasource.dart';
import 'package:swift_app/features/common/authentication/otp/data/repository/otp_repository_impl.dart';
import 'package:swift_app/features/common/authentication/otp/domain/repository/otp_repository.dart';
import 'package:swift_app/features/common/authentication/otp/domain/usecase/send_otp_usecase.dart';
import 'package:swift_app/features/common/authentication/otp/domain/usecase/verify_otp_usecase.dart';

import 'package:swift_app/features/common/authentication/signup/data/datasource/signup_datasource.dart';
import 'package:swift_app/features/common/authentication/signup/data/repository/signup_repository_impl.dart';
import 'package:swift_app/features/common/authentication/signup/domain/repository/signup_repository.dart';
import 'package:swift_app/features/common/authentication/signup/domain/usecase/signup_usecase.dart';
import 'package:swift_app/features/common/backend_api/data/datasource/backend_api_datasource.dart';
import 'package:swift_app/features/common/fare/domain/usecase/fare_estimate_usecase.dart';
import 'package:swift_app/features/common/fare/presentation/blocs/fare_bloc.dart';
import 'package:swift_app/features/driver/data/datasource/driver_realtime_service.dart';
import 'package:swift_app/features/driver/data/datasource/driver_location_publisher.dart';
import 'package:swift_app/features/rider/ride/data/datasource/rider_realtime_service.dart';
import 'package:swift_app/features/driver/data/datasource/driver_remote_datasource.dart';

import 'package:swift_app/features/rider/home/data/datasource/user_remote_datasource.dart';
import 'package:swift_app/features/rider/home/data/repository/user_repository_impl.dart';
import 'package:swift_app/features/rider/home/domain/repository/user_repository.dart';
import 'package:swift_app/features/rider/home/domain/usecase/address_create_usecase.dart';
import 'package:swift_app/features/rider/home/domain/usecase/user_details_usecase.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/user_bloc.dart';

import 'package:swift_app/features/rider/home/data/datasource/ride_type_datasource.dart';
import 'package:swift_app/features/rider/home/data/repository/ride_type_repository_impl.dart';
import 'package:swift_app/features/rider/home/domain/repository/ride_type_repository.dart';
import 'package:swift_app/features/rider/home/domain/usecase/ride_type_usecase.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_type_bloc.dart';

import 'package:swift_app/features/rider/home/data/datasource/ride_history_datasource.dart';
import 'package:swift_app/features/rider/home/data/repository/ride_history_repository_impl.dart';
import 'package:swift_app/features/rider/home/domain/repository/ride_history_repository.dart';
import 'package:swift_app/features/rider/home/domain/usecase/ride_history_usecase.dart';
import 'package:swift_app/features/rider/home/presentation/blocs/ride_history_bloc.dart';

import 'package:swift_app/features/rider/home/data/datasource/offer_datasource.dart';
import 'package:swift_app/features/rider/home/data/repository/offer_repository_impl.dart';

import 'package:swift_app/features/rider/home/domain/repository/offer_repository.dart';
import 'package:swift_app/features/rider/home/domain/usecase/offer_usecase.dart';

import 'package:swift_app/features/rider/home/presentation/blocs/offer_bloc.dart';

import 'package:swift_app/features/common/fare/data/datasource/fare_remote_datasource.dart';
import 'package:swift_app/features/common/fare/data/repository/fare_repository_impl.dart';
import 'package:swift_app/features/common/fare/domain/repository/fare_repository.dart';
import 'package:swift_app/features/rider/ride/data/datasource/ride_datasource.dart';
import 'package:swift_app/features/rider/ride/data/repository/ride_repository_impl.dart';
import 'package:swift_app/features/rider/ride/domain/repository/ride_repository.dart';
import 'package:swift_app/features/rider/ride/domain/usecases/ride_lifecycle_usecase.dart';
import 'package:swift_app/features/rider/ride/domain/usecases/ridebook_usecase.dart';
import 'package:swift_app/features/rider/ride/presentation/blocs/ride_bloc.dart';

import 'package:get_it/get_it.dart';
import 'package:shared_preferences/shared_preferences.dart';

final sl = GetIt.instance;

Future<void> initDependencies() async {
  final preferences = await SharedPreferences.getInstance();

  sl.registerLazySingleton<SharedPreferences>(() => preferences);

  sl.registerLazySingleton<RiderApiEndpoints>(() => RiderApiEndpoints());

  sl.registerLazySingleton<AuthLocalDatasource>(
    () => AuthLocalDatasource(preferences: sl()),
  );

  sl.registerLazySingleton<DioClient>(
    () => DioClient(
      accessTokenReader: () async {
        return sl<AuthLocalDatasource>().accessToken;
      },
      authUserIdReader: () async {
        return sl<AuthLocalDatasource>().userId;
      },
    ),
  );

  sl.registerLazySingleton<DriverRemoteDatasource>(
    () => DriverRemoteDatasource(
      dioClient: sl(),
      endpoints: sl(),
      authLocalDatasource: sl(),
    ),
  );

  sl.registerLazySingleton<PushNotificationService>(
    () => PushNotificationService(
      auth: sl(),
      registration: PushDeviceRegistration(sl<DioClient>().dio, preferences),
    ),
  );

  sl.registerLazySingleton<NotificationInboxDatasource>(
    () => NotificationInboxDatasource(sl<DioClient>().dio),
  );

  sl.registerLazySingleton<DriverRealtimeService>(
    () => DriverRealtimeService(authLocalDatasource: sl()),
  );

  sl.registerLazySingleton<DriverLocationPublisher>(
    () => DriverLocationPublisher(datasource: sl(), realtimeService: sl()),
  );

  sl.registerLazySingleton<RiderRealtimeService>(
    () => RiderRealtimeService(authLocalDatasource: sl()),
  );

  sl.registerLazySingleton<ChatRemoteDatasource>(
    () => ChatRemoteDatasource(dioClient: sl(), endpoints: sl()),
  );

  sl.registerLazySingleton<ChatRealtimeService>(
    () => ChatRealtimeService(auth: sl()),
  );

  sl.registerLazySingleton<BackendApiDatasource>(
    () => BackendApiDatasource(
      dioClient: sl(),
      endpoints: sl(),
      authLocalDatasource: sl(),
    ),
  );

  sl.registerLazySingleton<SignupDatasource>(
    () => SignupDatasource(dioClient: sl()),
  );

  sl.registerLazySingleton<SignupRepository>(
    () =>
        SignupRepositoryImpl(signupDatasource: sl(), authLocalDatasource: sl()),
  );

  sl.registerLazySingleton<SignupUsecase>(
    () => SignupUsecase(signupRepository: sl()),
  );

  sl.registerLazySingleton<LoginDatasource>(
    () => LoginDatasource(dioClient: sl(), endpoints: sl()),
  );

  sl.registerLazySingleton<LoginRepository>(
    () => LoginRepositoryImpl(loginDatasource: sl(), authLocalDatasource: sl()),
  );

  sl.registerLazySingleton<LoginUsecase>(
    () => LoginUsecase(loginRepository: sl()),
  );

  sl.registerLazySingleton<OtpDatasource>(
    () => OtpDatasource(dioClient: sl(), endpoints: sl()),
  );

  sl.registerLazySingleton<OtpRepository>(
    () => OtpRepositoryImpl(otpDatasource: sl()),
  );

  sl.registerLazySingleton<SendOtpUsecase>(
    () => SendOtpUsecase(otpRepository: sl()),
  );

  sl.registerLazySingleton<VerifyOtpUsecase>(
    () => VerifyOtpUsecase(otpRepository: sl()),
  );

  sl.registerLazySingleton<UserRemoteDatasource>(
    () => UserRemoteDatasource(
      dioClient: sl(),
      riderApiEndpoints: sl(),
      authLocalDatasource: sl(),
    ),
  );

  sl.registerLazySingleton<UserRepository>(
    () => UserRepositoryImpl(userRemoteDatasource: sl()),
  );

  sl.registerLazySingleton<AddressCreateUsecase>(
    () => AddressCreateUsecase(userRepository: sl()),
  );

  sl.registerLazySingleton<UserDetailsUsecase>(
    () => UserDetailsUsecase(userRepository: sl()),
  );

  sl.registerFactory<AuthBloc>(
    () => AuthBloc(
      sendOtpUsecase: sl(),
      verifyOtpUsecase: sl(),
      signupUsecase: sl(),
      loginUsecase: sl(),
      authLocalDatasource: sl(),
      onAuthenticated: () => sl<PushNotificationService>().startSession(),
      beforeLogout: () => sl<PushNotificationService>().endSession(),
    ),
  );

  sl.registerFactory<UserBloc>(
    () => UserBloc(userDetailsUsecase: sl(), addressCreateUsecase: sl()),
  );

  sl.registerLazySingleton<RideTypeDatasource>(
    () => RideTypeDatasource(
      dioClient: sl(),
      riderApiEndpoints: sl(),

      authLocalDatasource: sl(),
    ),
  );

  sl.registerLazySingleton<RideTypeRepository>(
    () => RideTypeRepositoryImpl(rideTypeDatasource: sl()),
  );

  sl.registerLazySingleton<RideTypeUsecase>(
    () => RideTypeUsecase(rideTypeRepository: sl()),
  );

  sl.registerFactory<RideTypeBloc>(() => RideTypeBloc(rideTypeUsecase: sl()));

  sl.registerLazySingleton<RideHistoryDatasource>(
    () => RideHistoryDatasource(
      dioClient: sl(),
      riderApiEndpoints: sl(),
      authLocalDatasource: sl(),
    ),
  );

  sl.registerLazySingleton<RideHistoryRepository>(
    () => RideHistoryRepositoryImpl(rideHistoryDatasource: sl()),
  );

  sl.registerLazySingleton<RideHistoryUsecase>(
    () => RideHistoryUsecase(rideHistoryRepository: sl()),
  );

  sl.registerFactory<RideHistoryBloc>(
    () => RideHistoryBloc(rideHistoryUsecase: sl()),
  );

  sl.registerLazySingleton<OfferDatasource>(
    () => OfferDatasource(
      dioClient: sl(),
      riderApiEndpoints: sl(),
      authLocalDatasource: sl(),
    ),
  );

  sl.registerLazySingleton<OfferRepository>(
    () => OfferRepositoryImpl(offerDatasource: sl()),
  );

  sl.registerLazySingleton<OfferUsecase>(
    () => OfferUsecase(offerRepository: sl()),
  );

  sl.registerFactory<OfferBloc>(() => OfferBloc(offerUsecase: sl()));

  sl.registerLazySingleton<FareRemoteDatasource>(
    () => FareRemoteDatasource(
      dioClient: sl(),
      endpoints: sl(),
      authLocalDatasource: sl(),
    ),
  );

  sl.registerLazySingleton<FareRepository>(
    () => FareRepositoryImpl(datasource: sl()),
  );

  sl.registerLazySingleton<FareEstimateUsecase>(
    () => FareEstimateUsecase(fareRepository: sl()),
  );

  sl.registerFactory<FareBloc>(() => FareBloc(repository: sl()));

  sl.registerLazySingleton<RideDatasource>(
    () => RideDatasource(dioClient: sl(), endpoints: sl()),
  );

  sl.registerLazySingleton<RideRepository>(
    () => RideRepositoryImpl(rideDatasource: sl()),
  );

  sl.registerLazySingleton<RidebookUsecase>(() => RidebookUsecase(sl()));

  sl.registerLazySingleton<RideLifecycleUsecase>(
    () => RideLifecycleUsecase(sl()),
  );

  sl.registerFactory<RideBloc>(() => RideBloc(ridebookUsecase: sl()));
}
