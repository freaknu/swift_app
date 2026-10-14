import 'package:swift_app/features/common/authentication/signup/domain/entity/signup_response.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AuthLocalDatasource {
  static const String _userIdKey = 'auth.userId';
  static const String _nameKey = 'auth.name';
  static const String _emailKey = 'auth.email';
  static const String _accountLockedKey = 'auth.accountLocked';
  static const String _deviceLockedKey = 'auth.deviceLocked';
  static const String _pinCodeKey = 'auth.pinCode';
  static const String _accessTokenKey = 'auth.accessToken';
  static const String _refreshTokenKey = 'auth.refreshToken';
  static const String _roleKey = 'auth.role';
  static const String _activeKey = 'auth.active';
  static const String _otpCompletionTokenKey = 'auth.otpCompletionToken';

  final SharedPreferences _preferences;

  AuthLocalDatasource({required SharedPreferences preferences})
    : _preferences = preferences;
  String? get accessToken {
    return _preferences.getString(_accessTokenKey);
  }

  String? get refreshToken {
    return _preferences.getString(_refreshTokenKey);
  }

  String? get otpCompletionToken =>
      _preferences.getString(_otpCompletionTokenKey);

  Future<void> completeOtpLogin(String token) async {
    // A late response from an earlier sign-in must preserve the newer challenge.
    if (otpCompletionToken == token) {
      await _preferences.remove(_otpCompletionTokenKey);
    }
  }

  bool get isAuthenticated {
    final token = accessToken;

    return token != null && token.isNotEmpty;
  }

  int? get userId {
    return _preferences.getInt(_userIdKey);
  }

  String? get name {
    return _preferences.getString(_nameKey);
  }

  String? get email {
    return _preferences.getString(_emailKey);
  }

  String? get role {
    return _preferences.getString(_roleKey);
  }

  bool get active {
    return _preferences.getBool(_activeKey) ?? false;
  }

  bool get accountLocked {
    return _preferences.getBool(_accountLockedKey) ?? false;
  }

  bool get deviceLocked {
    return _preferences.getBool(_deviceLockedKey) ?? false;
  }

  int? get pinCode {
    return _preferences.getInt(_pinCodeKey);
  }

  SignupResponse? getSavedUser() {
    final int? savedUserId = userId;
    final String? savedAccessToken = accessToken;
    final String? savedRefreshToken = refreshToken;

    if (savedUserId == null ||
        savedAccessToken == null ||
        savedRefreshToken == null) {
      return null;
    }

    return SignupResponse(
      userId: savedUserId,
      name: name ?? '',
      email: email ?? '',
      accountLocked: accountLocked,
      deviceLocked: deviceLocked,
      pinCode: pinCode ?? 0,
      accessToken: savedAccessToken,
      refreshToken: savedRefreshToken,
      role: role ?? '',
      active: active,
      otpCompletionToken: otpCompletionToken,
    );
  }

  Future<void> saveAuthUser(SignupResponse user) async {
    final saved = await Future.wait([
      _preferences.setInt(_userIdKey, user.userId),

      _preferences.setString(_nameKey, user.name),

      _preferences.setString(_emailKey, user.email),

      _preferences.setBool(_accountLockedKey, user.accountLocked),

      _preferences.setBool(_deviceLockedKey, user.deviceLocked),

      _preferences.setInt(_pinCodeKey, user.pinCode),

      _preferences.setString(_refreshTokenKey, user.refreshToken),

      _preferences.setString(_roleKey, user.role),

      _preferences.setBool(_activeKey, user.active),

      if (user.otpCompletionToken?.isNotEmpty == true)
        _preferences.setString(_otpCompletionTokenKey, user.otpCompletionToken!)
      else
        _preferences.remove(_otpCompletionTokenKey),
    ]);
    if (saved.any((success) => !success) ||
        !await _preferences.setString(_accessTokenKey, user.accessToken)) {
      throw StateError('Session persistence failed');
    }
  }

  Future<void> clear() async {
    await Future.wait([
      _preferences.remove(_userIdKey),
      _preferences.remove(_nameKey),
      _preferences.remove(_emailKey),
      _preferences.remove(_accountLockedKey),
      _preferences.remove(_deviceLockedKey),
      _preferences.remove(_pinCodeKey),
      _preferences.remove(_accessTokenKey),
      _preferences.remove(_refreshTokenKey),
      _preferences.remove(_roleKey),
      _preferences.remove(_activeKey),
      _preferences.remove(_otpCompletionTokenKey),
    ]);
  }
}
