# Notification service push setup

The service stores one FCM token per app installation and sends Firebase
notifications from Kafka ride events. The API gateway exposes the registration
endpoint at `PUT /api/v1/notifications/devices/{installationId}`.

## Firebase credentials

Do not commit the Firebase Admin service-account JSON. For local development,
set the path before starting `start-services.sh` from the backend root:

```sh
export FIREBASE_CREDENTIALS="file:$PWD/noticationservice/src/main/resources/static/notification/swift-app-service-account.json"
```

For deployment, use a secret-mounted file or Google Application Default
Credentials. The service-account project must match the mobile Firebase project
`swift-app-c65e5`.

The mobile app registers a token only after authentication, and unregisters it
before logout. Android requires the notification permission at runtime. iOS
requires Push Notifications and Background Modes / Remote notifications enabled
for the Runner target, with a matching APNs key or certificate in Firebase.
