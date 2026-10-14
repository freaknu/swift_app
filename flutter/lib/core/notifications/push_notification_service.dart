import 'dart:async';
import 'dart:convert';
import 'dart:developer' as developer;

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:swift_app/core/notifications/push_device_registration.dart';
import 'package:swift_app/core/notifications/push_message_presentation.dart';
import 'package:swift_app/features/common/authentication/auth/data/datasource/auth_local_datasource.dart';
import 'package:swift_app/features/common/chat/data/datasource/chat_remote_datasource.dart';
import 'package:swift_app/firebase_options.dart';
import 'package:shared_preferences/shared_preferences.dart';

const _chatReplyActionId = 'chat_reply';
const _chatReplyCategoryId = 'swift_chat';
const _pushEnabled = bool.fromEnvironment('PUSH_ENABLED', defaultValue: false);

enum PushDeliveryState {
  inactive,
  connecting,
  ready,
  permissionDenied,
  failed,
  unavailable,
}

class PushDeliveryStatus {
  const PushDeliveryStatus(this.state, {this.code});

  final PushDeliveryState state;

  /// A short diagnostic code. Never contains an FCM token or JWT.
  final String? code;

  bool get needsAttention =>
      state == PushDeliveryState.permissionDenied ||
      state == PushDeliveryState.failed;

  @override
  bool operator ==(Object other) =>
      other is PushDeliveryStatus && other.state == state && other.code == code;

  @override
  int get hashCode => Object.hash(state, code);
}

@pragma('vm:entry-point')
Future<void> swiftMessagingBackgroundHandler(RemoteMessage message) async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp(options: DefaultFirebaseOptions.currentPlatform);
  // Android/APNs presents notification+data messages. Data-only messages need
  // a local notification, including ride events if a sender omits notification.
  if (message.notification != null ||
      !PushMessagePresentation.supportsDataOnly(message.data) ||
      PushMessagePresentation.isExpired(message.data, DateTime.now())) {
    return;
  }
  final preferences = await SharedPreferences.getInstance();
  final auth = AuthLocalDatasource(preferences: preferences);
  if (!PushNotificationService.belongsToSession(
    message.data,
    auth.userId,
    auth.role,
  )) {
    return;
  }

  final local = FlutterLocalNotificationsPlugin();
  await local.initialize(
    settings: InitializationSettings(
      android: AndroidInitializationSettings('ic_stat_notification'),
      iOS: _darwinInitializationSettings,
    ),
    onDidReceiveBackgroundNotificationResponse:
        swiftNotificationBackgroundResponse,
  );
  await _createRideChannel(local);
  await local.show(
    id: PushMessagePresentation.notificationId(message.data),
    title: _notificationTitle(message),
    body: _notificationBody(message),
    payload: jsonEncode(message.data),
    notificationDetails: _notificationDetails(message.data),
  );
}

/// Handles Android's notification action in an isolate-safe way. The auth
/// token is read from the app's persisted session, never from notification
/// data or logs.
@pragma('vm:entry-point')
Future<void> swiftNotificationBackgroundResponse(
  NotificationResponse response,
) async {
  if (response.actionId != _chatReplyActionId) return;
  final text = response.input?.trim();
  if (text == null || text.isEmpty) return;
  try {
    final payload = _decodePayload(response.payload);
    if (payload == null) return;
    await sendChatNotificationReply(payload: payload, text: text);
  } catch (_) {
    // A background isolate has no UI. The next chat history refresh is the
    // durable recovery path when the backend is temporarily unavailable.
  }
}

class PushNotificationService with WidgetsBindingObserver {
  PushNotificationService({required this.auth, required this.registration});

  final AuthLocalDatasource auth;
  final PushDeviceRegistration registration;
  final _local = FlutterLocalNotificationsPlugin();
  final _opens = StreamController<void>.broadcast();
  final _statusChanges = StreamController<PushDeliveryStatus>.broadcast();
  Stream<void> get opens => _opens.stream;
  Stream<PushDeliveryStatus> get statusChanges => _statusChanges.stream;
  PushDeliveryStatus get status => _status;
  PushDeliveryStatus _status = const PushDeliveryStatus(
    PushDeliveryState.inactive,
  );
  Map<String, dynamic>? _pendingOpen;
  bool _initialized = false;
  bool _active = false;
  Future<void>? _initializing;
  Future<void>? _starting;
  Future<void>? _syncing;
  Timer? _retry;
  int _generation = 0;
  String? _registered;

  bool get supported =>
      _pushEnabled &&
      !kIsWeb &&
      (defaultTargetPlatform == TargetPlatform.android ||
          defaultTargetPlatform == TargetPlatform.iOS);

  Future<void> initialize() {
    if (!supported || _initialized) return Future.value();
    return _initializing ??= _initialize().whenComplete(
      () => _initializing = null,
    );
  }

  Future<void> _initialize() async {
    await Firebase.initializeApp(
      options: DefaultFirebaseOptions.currentPlatform,
    );
    FirebaseMessaging.onBackgroundMessage(swiftMessagingBackgroundHandler);
    await _local.initialize(
      settings: InitializationSettings(
        android: AndroidInitializationSettings('ic_stat_notification'),
        iOS: _darwinInitializationSettings,
      ),
      onDidReceiveNotificationResponse: (response) {
        if (response.actionId == _chatReplyActionId) {
          final text = response.input?.trim();
          if (text != null && text.isNotEmpty) {
            unawaited(_sendReply(response.payload, text));
          }
          return;
        }
        final data = _decodePayload(response.payload);
        if (data != null) _queueOpen(data);
      },
      onDidReceiveBackgroundNotificationResponse:
          swiftNotificationBackgroundResponse,
    );
    await _createRideChannel(_local);
    await FirebaseMessaging.instance
        .setForegroundNotificationPresentationOptions(
          alert: false,
          badge: false,
          sound: false,
        );
    FirebaseMessaging.onMessage.listen(
      (message) => unawaited(_showForeground(message)),
    );
    FirebaseMessaging.onMessageOpenedApp.listen(
      (message) => _queueOpen(message.data),
    );
    FirebaseMessaging.instance.onTokenRefresh.listen((_) {
      _registered = null;
      unawaited(sync());
    }, onError: (Object error) => _log('Token refresh failed', error));

    final initial = await FirebaseMessaging.instance.getInitialMessage();
    if (initial != null) _pendingOpen = initial.data;
    final localLaunch = await _local.getNotificationAppLaunchDetails();
    if (localLaunch?.didNotificationLaunchApp == true) {
      final response = localLaunch?.notificationResponse;
      if (response?.actionId == _chatReplyActionId) {
        final text = response?.input?.trim();
        if (text != null && text.isNotEmpty) {
          unawaited(_sendReply(response?.payload, text));
        }
      } else {
        final data = _decodePayload(response?.payload);
        if (data != null) _pendingOpen = data;
      }
    }
    WidgetsBinding.instance.addObserver(this);
    _initialized = true;
    if (_pendingOpen != null) _opens.add(null);
  }

  Future<void> startSession() {
    if (!supported) {
      _setStatus(const PushDeliveryStatus(PushDeliveryState.unavailable));
      return Future.value();
    }
    _active = true;
    return _starting ??= _startSession().whenComplete(() => _starting = null);
  }

  Future<void> _startSession() async {
    _setStatus(const PushDeliveryStatus(PushDeliveryState.connecting));
    try {
      await initialize();
      if (!_active) return;
      await FirebaseMessaging.instance.requestPermission(
        alert: true,
        badge: true,
        sound: true,
      );
      await sync();
    } catch (error) {
      _log('Push setup failed; will retry', error);
      _setStatus(
        PushDeliveryStatus(
          PushDeliveryState.failed,
          code: _safePushErrorCode(error, phase: 'setup'),
        ),
      );
      _scheduleRetry();
    }
  }

  Future<void> sync() {
    if (!_active || !_initialized || !auth.isAuthenticated) {
      return Future.value();
    }
    return _syncing ??= _sync().whenComplete(() => _syncing = null);
  }

  /// Signup can authenticate before the asynchronously created profile exists.
  /// Retry device binding once that profile is actually available, including
  /// when the initial registration request is still in flight.
  Future<void> syncAfterProfileReady() async {
    await _starting;
    await _syncing;
    await sync();
  }

  Future<void> _sync() async {
    final generation = _generation;
    final userId = auth.userId;
    final role = auth.role?.toUpperCase();
    if (userId == null || (role != 'RIDER' && role != 'DRIVER')) return;
    bool? enabled;
    try {
      final settings = await FirebaseMessaging.instance
          .getNotificationSettings();
      enabled =
          settings.authorizationStatus == AuthorizationStatus.authorized ||
          settings.authorizationStatus == AuthorizationStatus.provisional;
      if (!enabled) {
        _setStatus(
          const PushDeliveryStatus(PushDeliveryState.permissionDenied),
        );
      }
      if (defaultTargetPlatform == TargetPlatform.iOS &&
          await FirebaseMessaging.instance.getAPNSToken() == null) {
        _setStatus(
          const PushDeliveryStatus(
            PushDeliveryState.failed,
            code: 'apns_token_pending',
          ),
        );
        _scheduleRetry();
        return;
      }
      final token = await FirebaseMessaging.instance.getToken();
      if (!_active || generation != _generation || userId != auth.userId) {
        return;
      }
      if (token == null) {
        _setStatus(
          const PushDeliveryStatus(
            PushDeliveryState.failed,
            code: 'fcm_token_unavailable',
          ),
        );
        _scheduleRetry();
        return;
      }
      final signature = '$userId:$role:$enabled:$token';
      if (_registered == signature) return;
      await registration.register(token: token, role: role!, enabled: enabled);
      if (_active && generation == _generation) {
        _registered = signature;
        _retry?.cancel();
        _setStatus(
          PushDeliveryStatus(
            enabled
                ? PushDeliveryState.ready
                : PushDeliveryState.permissionDenied,
          ),
        );
      }
    } catch (error) {
      _log('Device registration failed; will retry', error);
      _setStatus(
        PushDeliveryStatus(
          enabled == false
              ? PushDeliveryState.permissionDenied
              : PushDeliveryState.failed,
          code: _safePushErrorCode(error, phase: 'registration'),
        ),
      );
      _scheduleRetry();
    }
  }

  Future<void> endSession() async {
    _active = false;
    _generation++;
    _retry?.cancel();
    _pendingOpen = null;
    _registered = null;
    _setStatus(const PushDeliveryStatus(PushDeliveryState.inactive));
    await _starting;
    await _syncing;
    if (!_initialized) return;
    try {
      await registration.unregister();
    } catch (error) {
      _log('Device unregistration failed', error);
    }
    try {
      await FirebaseMessaging.instance.deleteToken();
    } catch (error) {
      _log('FCM token deletion failed', error);
    }
    await _local.cancelAll();
  }

  void _scheduleRetry() {
    _retry?.cancel();
    if (_active) {
      _retry = Timer(const Duration(seconds: 20), () {
        unawaited(_initialized ? sync() : startSession());
      });
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _registered = null;
      unawaited(sync());
    }
  }

  static bool belongsToSession(
    Map<String, dynamic> data,
    int? userId,
    String? role,
  ) =>
      userId != null &&
      data['authUserId']?.toString() == '$userId' &&
      data['role']?.toString() == role?.toUpperCase();

  Map<String, dynamic>? takePendingOpen() {
    if (!auth.isAuthenticated) return null;
    final data = _pendingOpen;
    _pendingOpen = null;
    return data != null && belongsToSession(data, auth.userId, auth.role)
        ? data
        : null;
  }

  void _queueOpen(Map<String, dynamic> data) {
    _pendingOpen = data;
    _opens.add(null);
  }

  Future<void> _showForeground(RemoteMessage message) async {
    if (!_active || !belongsToSession(message.data, auth.userId, auth.role)) {
      return;
    }
    final expiresAt = DateTime.tryParse(
      message.data['expiresAt']?.toString() ?? '',
    );
    if (PushMessagePresentation.isExpired(message.data, DateTime.now())) return;
    final notification = message.notification;
    if (notification == null &&
        !PushMessagePresentation.supportsDataOnly(message.data)) {
      return;
    }
    try {
      await _local.show(
        id: PushMessagePresentation.notificationId(message.data),
        title: _notificationTitle(message),
        body: _notificationBody(message),
        payload: jsonEncode(message.data),
        notificationDetails: _notificationDetails(
          message.data,
          timeoutAfter: expiresAt?.difference(DateTime.now()).inMilliseconds,
        ),
      );
    } catch (error) {
      _log('Foreground notification failed', error);
    }
  }

  Future<void> _sendReply(String? payload, String text) async {
    final data = _decodePayload(payload);
    if (data == null) return;
    try {
      await sendChatNotificationReply(payload: data, text: text);
    } catch (error) {
      _log('Chat notification reply failed', error);
    }
  }

  void _log(String message, Object error) {
    developer.log(
      message,
      name: 'PushNotifications',
      error: _safePushErrorCode(error, phase: 'runtime'),
    );
  }

  void _setStatus(PushDeliveryStatus next) {
    if (_status == next) return;
    _status = next;
    _statusChanges.add(next);
  }
}

String _safePushErrorCode(Object error, {required String phase}) {
  if (error is DioException) {
    return '${phase}_http_${error.response?.statusCode ?? 0}';
  }
  if (error is FirebaseException) {
    return '${phase}_firebase_${_safeCodePart(error.code)}';
  }
  if (error is PlatformException) {
    return '${phase}_platform_${_safeCodePart(error.code)}';
  }
  return '${phase}_${error.runtimeType}';
}

String _safeCodePart(String value) {
  final sanitized = value.replaceAll(RegExp(r'[^a-zA-Z0-9_-]'), '_');
  return sanitized.substring(0, sanitized.length > 48 ? 48 : sanitized.length);
}

Map<String, dynamic>? _decodePayload(String? payload) {
  if (payload == null || payload.isEmpty) return null;
  try {
    final decoded = jsonDecode(payload);
    if (decoded is Map) return Map<String, dynamic>.from(decoded);
  } on FormatException {
    // Ignore malformed old local notification payloads.
  }
  return null;
}

String _notificationTitle(RemoteMessage message) {
  return message.notification?.title ??
      PushMessagePresentation.title(message.data);
}

String _notificationBody(RemoteMessage message) {
  return message.notification?.body ??
      PushMessagePresentation.body(message.data);
}

NotificationDetails _notificationDetails(
  Map<String, dynamic> data, {
  int? timeoutAfter,
}) {
  final chat = PushMessagePresentation.isChat(data);
  return NotificationDetails(
    android: AndroidNotificationDetails(
      'swift_rides',
      'Ride updates',
      importance: Importance.high,
      priority: Priority.high,
      timeoutAfter: timeoutAfter,
      actions: chat
          ? const [
              AndroidNotificationAction(
                _chatReplyActionId,
                'Reply',
                showsUserInterface: false,
                inputs: [
                  AndroidNotificationActionInput(
                    label: 'Message',
                    allowFreeFormInput: true,
                  ),
                ],
              ),
            ]
          : null,
    ),
    iOS: DarwinNotificationDetails(
      presentAlert: true,
      presentSound: true,
      categoryIdentifier: chat ? _chatReplyCategoryId : null,
    ),
  );
}

final _darwinInitializationSettings = DarwinInitializationSettings(
  requestAlertPermission: false,
  requestBadgePermission: false,
  requestSoundPermission: false,
  notificationCategories: [
    DarwinNotificationCategory(
      _chatReplyCategoryId,
      actions: [
        DarwinNotificationAction.text(
          _chatReplyActionId,
          'Reply',
          buttonTitle: 'Send',
          placeholder: 'Message',
        ),
      ],
    ),
  ],
);

Future<void> _createRideChannel(FlutterLocalNotificationsPlugin local) async {
  await local
      .resolvePlatformSpecificImplementation<
        AndroidFlutterLocalNotificationsPlugin
      >()
      ?.createNotificationChannel(
        const AndroidNotificationChannel(
          'swift_rides',
          'Ride updates',
          description: 'New driver requests and changes to your trip',
          importance: Importance.high,
        ),
      );
}
