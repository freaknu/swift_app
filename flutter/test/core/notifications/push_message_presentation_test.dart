import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/core/notifications/push_message_presentation.dart';

void main() {
  test('data-only ride offer has useful content and expires', () {
    final data = {
      'type': 'RIDE_OFFER',
      'rideId': '42',
      'expiresAt': '2026-10-10T12:00:00Z',
    };
    expect(PushMessagePresentation.supportsDataOnly(data), isTrue);
    expect(PushMessagePresentation.title(data), 'New ride request');
    expect(
      PushMessagePresentation.isExpired(data, DateTime.utc(2026, 10, 10, 12)),
      isTrue,
    );
  });

  test('chat message notification cannot overwrite the ride notification', () {
    final ride = PushMessagePresentation.notificationId(const {
      'type': 'DRIVER_ASSIGNED',
      'rideId': '42',
    });
    final chat = PushMessagePresentation.notificationId(const {
      'type': 'CHAT_MESSAGE',
      'rideId': '42',
      'messageId': '18',
    });
    expect(chat, isNot(ride));
    expect(
      chat,
      PushMessagePresentation.notificationId(const {
        'type': 'CHAT_MESSAGE',
        'rideId': '42',
        'messageId': '18',
      }),
    );
  });

  test('separate review events keep separate Android notifications', () {
    final first = PushMessagePresentation.notificationId(const {
      'type': 'DOCUMENT_APPROVED',
      'eventId': 'review-101',
      'documentId': '4',
    });
    final second = PushMessagePresentation.notificationId(const {
      'type': 'DOCUMENT_APPROVED',
      'eventId': 'review-102',
      'documentId': '4',
    });
    expect(first, isNot(second));
  });
}
