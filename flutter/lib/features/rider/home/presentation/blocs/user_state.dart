import 'package:swift_app/features/rider/home/domain/entity/user_reponse.dart';

enum UserStatus { initial, loading, success, failure }

class UserState {
  final UserStatus status;
  final UserReponse? user;
  final UserAddress? createdAddress;
  final String? message;
  final int? statusCode;

  const UserState({
    this.status = UserStatus.initial,
    this.user,
    this.createdAddress,
    this.message,
    this.statusCode,
  });

  UserState copyWith({
    UserStatus? status,
    UserReponse? user,
    UserAddress? createdAddress,
    String? message,
    int? statusCode,
    bool clearCreatedAddress = false,
    bool clearMessage = false,
    bool clearUser = false,
  }) {
    return UserState(
      status: status ?? this.status,

      user: clearUser ? null : user ?? this.user,

      createdAddress: clearCreatedAddress
          ? null
          : createdAddress ?? this.createdAddress,

      message: clearMessage ? null : message ?? this.message,

      statusCode: statusCode ?? this.statusCode,
    );
  }
}
