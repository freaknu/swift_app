import 'package:flutter_test/flutter_test.dart';
import 'package:swift_app/features/common/authentication/signup/data/model/signup_response_model.dart';

void main() {
  test('auth response accepts the backend isActive field', () {
    final response = SignupResponseModel.fromJson({
      'userId': 42,
      'isActive': true,
    });

    expect(response.userId, 42);
    expect(response.active, isTrue);
  });
}
