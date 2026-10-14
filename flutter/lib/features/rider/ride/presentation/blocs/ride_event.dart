import 'package:swift_app/features/rider/ride/domain/entity/ride_request.dart';

abstract class RideEvent {
  const RideEvent();
}

class BookRide extends RideEvent {
  final RideRequest request;

  const BookRide({required this.request});
}

class ResetRide extends RideEvent {
  const ResetRide();
}
