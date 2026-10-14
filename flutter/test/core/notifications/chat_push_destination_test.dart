import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/core/notifications/chat_push_destination.dart';

void main() {
  test('chat push uses the rider profile ID even when auth ID differs', () {
    final destination = ChatPushDestination.fromData(const {
      'rideId': '42',
      'authUserId': '900',
      'riderId': '7',
      'driverId': '8',
    }, 'RIDER');
    expect(destination?.currentProfileId, 7);
    expect(destination?.rideId, 42);
  });

  test('missing profile ID does not create a chat sender from auth ID', () {
    expect(
      ChatPushDestination.fromData(const {
        'rideId': '42',
        'authUserId': '900',
      }, 'DRIVER'),
      isNull,
    );
  });
}
