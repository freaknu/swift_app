# Swift Flutter application

## Android APK

[Download SwiftApp 1.0.1 APK](https://github.com/freaknu/swift_app/releases/download/v1.0.1/SwiftApp-1.0.1.apk) · [Release notes and checksum](https://github.com/freaknu/swift_app/releases/tag/v1.0.1)

Android 7.0/API 24 or newer. Version 1.0.1, build 2, using the existing Android Debug test signer. Connects to the hosted HTTPS/WSS backend.

[Watch the rider and driver demo](https://github.com/freaknu/swift_app/issues/1) · [Live API docs](https://swift-app.duckdns.org/swagger-ui.html)

[![SwiftApp demo](../docs/demo/swift-app-demo-poster.png)](https://github.com/freaknu/swift_app/issues/1)

Flutter client for Swift's rider and driver flows: vehicle selection, live matching, maps and location, trip controls, chat, Razorpay checkout, profiles, and light/dark themes. Branding, SVG icons and vehicle photographs are bundled with the app.

## Quick start

Use Flutter 3.41 or a compatible newer stable SDK. Android needs Android tooling; Apple builds need Xcode/CocoaPods.

```bash
cd flutter
flutter pub get
cp config/dart-defines.example.json config/dart-defines.json
cp android/secrets.properties.example android/secrets.properties
# For iOS:
cp ios/Flutter/Maps.xcconfig.example ios/Flutter/Maps.xcconfig
```

Set your native Maps SDK keys in the two native files. Set `GOOGLE_PLACES_API_KEY` in the private Dart defines JSON if using a separate Places key; otherwise the app reads native Maps configuration. Restrict keys to your own application IDs and signing certificates. No Maps key ships in this repository.

```bash
flutter run --dart-define-from-file=config/dart-defines.json
```

Default API: `https://swift-app.duckdns.org/`. Default socket: `wss://swift-app.duckdns.org/ws-native`. Override `API_BASE_URL` and `WS_URL` in the ignored JSON for your own backend. Explicit `API_HOST` also supports local HTTP development on port 8080.

| Environment | API | Socket |
| --- | --- | --- |
| Hosted | `https://swift-app.duckdns.org/` | `wss://swift-app.duckdns.org/ws-native` |
| Android emulator | `http://10.0.2.2:8080/` | `ws://10.0.2.2:8080/ws-native` |
| iOS simulator / desktop | `http://127.0.0.1:8080/` | `ws://127.0.0.1:8080/ws-native` |
| Physical device | Your computer's reachable LAN address | Same address with `ws://` |

For LAN testing, bind only the gateway to a device-reachable address; keep internal services and infrastructure private.

## Optional Firebase push

Push defaults to disabled in the public configuration. The app can build without private Firebase native files. Register your own Firebase applications and use FlutterFire CLI to obtain their configuration. Keep `google-services.json`, `GoogleService-Info.plist`, `firebase.json` and account credentials outside version control.

Android applies Google Services only when `android/app/google-services.json` exists. On Apple targets, add your private plist to Runner with Xcode and configure APNs, push capabilities and your own signing team. Set matching `FIREBASE_*` Dart defines and `PUSH_ENABLED=true`. Backend push also needs Firebase Admin configuration. Never put the Admin service-account private key in Flutter.

## Payments

The backend returns Razorpay's public checkout key and order details. Razorpay secrets belong only on the backend. Use a test account for checkout validation. The UI follows backend-verified status through socket events and status refreshes.

## Structure and checks

- `lib/API/`: endpoint configuration and HTTP setup.
- `lib/core/`: dependency injection, Maps, startup and notifications.
- `lib/features/rider/`, `lib/features/driver/`: role-specific flows.
- `lib/features/common/`: authentication, chat, payments and shared widgets.
- `assets/`: branding, icons and vehicle assets.
- `test/`, `integration_test/`: automated behavior/layout and live-backend checks.

```bash
flutter analyze
flutter test
flutter build apk --debug --dart-define-from-file=config/dart-defines.json
flutter build ios --simulator --debug --dart-define-from-file=config/dart-defines.json
```

Run live integration tests with disposable test accounts and tokens supplied through test Dart defines. Never commit tokens or account credentials. Release signing is not supplied; configure your own private signing before distribution.
