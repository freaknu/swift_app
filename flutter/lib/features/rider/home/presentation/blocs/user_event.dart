import 'package:swift_app/features/rider/home/domain/entity/address_create.dart';

abstract class UserEvent {
  const UserEvent();
}

// ============================================================
// FETCH USER DETAILS
// ============================================================

class FetchUserDetails extends UserEvent {
  const FetchUserDetails();
}

// ============================================================
// CREATE ADDRESS
// ============================================================

class CreateUserAddress extends UserEvent {
  final AddressCreate address;

  const CreateUserAddress({required this.address});
}
